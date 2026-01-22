package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于WebView的JavaScript输入脚本引擎实现
 * 实现InputScriptEngine接口，提供脚本执行功能
 */
public class JsInputScriptEngine implements InputScriptEngine {
    
    private static final String TAG = "JsInputScriptEngine";
    private static final long EXECUTION_TIMEOUT_MS = 5000; // 5秒执行超时
    
    private Context context;
    private WebView webView;
    private Handler uiHandler;
    
    private EngineState state = EngineState.UNINITIALIZED;
    private String lastError = null;
    private AtomicLong lastExecutionTime = new AtomicLong(0);
    
    // 帧序号管理
    private AtomicLong currentFrameId = new AtomicLong(0);
    private AtomicLong expectedFrameId = new AtomicLong(0);
    
    // 脚本执行结果
    private AtomicReference<InputState> executionResult = new AtomicReference<>();
    private CountDownLatch executionLatch;
    
    // 存储当前帧的原始输入JSON，用于ScriptBridge的RawAccess方法
    private String rawInputToJson;
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public JsInputScriptEngine(Context context) {
        this.context = context;
        this.uiHandler = new Handler(Looper.getMainLooper());
        init();
    }
    
    @Override
    public void init() {
        uiHandler.post(() -> {
            // 初始化WebView
            webView = new WebView(context);
            WebSettings settings = webView.getSettings();
            
            // 启用JavaScript
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            
            // 禁用缓存和其他不需要的功能
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setUseWideViewPort(false);
            settings.setLoadWithOverviewMode(false);
            
            // 注入JavaScript接口
            webView.addJavascriptInterface(new ScriptBridge(), "android");
            
            // 设置WebViewClient
            webView.setWebViewClient(new WebViewClient());
            
            // 初始加载空白页面
            webView.loadUrl("about:blank");
            
            state = EngineState.INITIALIZED;
        });
    }
    
    @Override
    public boolean loadScript(String scriptCode) {
        if (state != EngineState.INITIALIZED && state != EngineState.LOADED) {
            lastError = "Script engine not initialized";
            state = EngineState.ERROR;
            return false;
        }
        
        state = EngineState.LOADING;
        lastError = null;
        
        final CountDownLatch loadLatch = new CountDownLatch(1);
        final AtomicBoolean loadSuccess = new AtomicBoolean(false);
        
        uiHandler.post(() -> {
            try {
                // 加载脚本
                String html = generateHtml(scriptCode);
                webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
                
                // 验证脚本是否加载成功
                webView.evaluateJavascript(
                    "typeof update === 'function' && typeof init === 'function'",
                    result -> {
                        if (result.equals("true")) {
                            loadSuccess.set(true);
                            state = EngineState.LOADED;
                        } else {
                            lastError = "Script missing required functions (update, init)";
                            state = EngineState.ERROR;
                        }
                        loadLatch.countDown();
                    }
                );
            } catch (Exception e) {
                lastError = "Error loading script: " + e.getMessage();
                state = EngineState.ERROR;
                loadLatch.countDown();
            }
        });
        
        try {
            loadLatch.await(EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            lastError = "Script loading timed out";
            state = EngineState.ERROR;
            return false;
        }
        
        return loadSuccess.get();
    }
    
    @Override
    public boolean update(RawInput rawInput, InputState inputState) {
        if (state != EngineState.LOADED) {
            // 脚本未加载或处于错误状态时，仅释放按键，不返回错误
            // 避免在WebSocket未连接时导致浮窗显示错误
            inputState.clearAllKeys();
            // 脚本未加载是正常状态，不是错误，返回true
            return true;
        }
        
        long startTime = System.currentTimeMillis();
        long frameId = currentFrameId.incrementAndGet();
        expectedFrameId.set(frameId);
        
        executionResult.set(inputState);
        executionLatch = new CountDownLatch(1);
        
        final AtomicBoolean success = new AtomicBoolean(true);
        
        uiHandler.post(() -> {
            try {
                // 调用脚本的update函数
                String rawJson = rawInputToJson(rawInput, frameId);
                // 保存raw input JSON，用于RawAccess方法
                this.rawInputToJson = rawJson;
                // 传递rawAccess和stateMutator两个参数，保持接口一致性
                String jsCode = String.format(
                    "try { var result = update(rawAccess, stateMutator); android.onUpdateComplete(JSON.stringify({frameId: %d, result: result})); } catch(e) { android.onScriptError('RUNTIME_ERROR', e.message, %d); }",
                    frameId, frameId
                );
                
                webView.evaluateJavascript(jsCode, null);
                
                // 等待执行完成或超时
                if (!executionLatch.await(EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    // 处理超时错误
                    if (expectedFrameId.get() == frameId) {
                        lastError = "TIMEOUT: Script execution timed out";
                        state = EngineState.ERROR;
                        success.set(false);
                        // 确保所有按键释放，防止粘键
                        inputState.clearAllKeys();
                    }
                    executionLatch.countDown();
                }
            } catch (Exception e) {
                // 处理运行时错误
                if (expectedFrameId.get() == frameId) {
                    lastError = "RUNTIME_ERROR: " + e.getMessage();
                    state = EngineState.ERROR;
                    success.set(false);
                    // 确保所有按键释放，防止粘键
                    inputState.clearAllKeys();
                }
                executionLatch.countDown();
            }
        });
        
        try {
            executionLatch.await(EXECUTION_TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            lastError = "INTERRUPTED: Script execution interrupted";
            state = EngineState.ERROR;
            success.set(false);
            // 确保所有按键释放，防止粘键
            inputState.clearAllKeys();
        }
        
        lastExecutionTime.set(System.currentTimeMillis() - startTime);
        return success.get();
    }
    
    @Override
    public void onEvent(GameInputEvent event) {
        if (state != EngineState.LOADED) {
            return;
        }
        
        uiHandler.post(() -> {
            try {
                String eventJson = eventToJson(event);
                String jsCode = String.format(
                    "if (typeof onEvent === 'function') { onEvent(%s); }",
                    eventJson
                );
                webView.evaluateJavascript(jsCode, null);
            } catch (Exception e) {
                lastError = "RUNTIME_ERROR: " + e.getMessage();
                state = EngineState.ERROR;
            }
        });
    }
    
    @Override
    public void reset() {
        if (state == EngineState.SHUTDOWN) {
            return;
        }
        
        uiHandler.post(() -> {
            try {
                webView.evaluateJavascript(
                    "if (typeof reset === 'function') { reset(); }",
                    null
                );
            } catch (Exception e) {
                lastError = "Error resetting script: " + e.getMessage();
                state = EngineState.ERROR;
            }
        });
    }
    
    @Override
    public void shutdown() {
        state = EngineState.SHUTDOWN;
        
        uiHandler.post(() -> {
            if (webView != null) {
                webView.removeJavascriptInterface("android");
                webView.stopLoading();
                webView.destroy();
                webView = null;
            }
        });
    }
    
    @Override
    public EngineState getState() {
        return state;
    }
    
    @Override
    public String getLastError() {
        return lastError;
    }
    
    @Override
    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }
    
    /**
     * 生成包含脚本的HTML页面
     * @param scriptCode 脚本代码
     * @return 完整的HTML页面
     */
    private String generateHtml(String scriptCode) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'><script>" +
               "// JavaScript Runtime Environment - 分层架构实现\n" +
               
               "// ========== Layer 1: Environment Layer (宿主层) ==========\n" +
               "// 提供IO、绘制、时间、日志等基础服务\n" +
               "// 不包含任何输入逻辑\n" +
               "const Env = {\n" +
               "    now: function() { return android.getTimestamp(); },\n" +
               "    log: function(msg) { android.debug(msg); },\n" +
               "    sendKey: function(code, pressed) { if (pressed) { android.holdKey(code); } else { android.releaseKey(code); } },\n" +
               "    sendAxis: function(id, value) { /* 轴事件处理，目前暂未实现 */ },\n" +
               "    sendMacro: function(macroId) { /* 宏事件处理，目前暂未实现 */ }\n" +
               "};\n" +
               
               "// ========== Layer 2: Core Input Logic (核心层) ==========\n" +
               "// 负责输入核心逻辑，管理区域定义和事件绑定\n" +
               "const InputCore = (() => {\n" +
               "    const regions = new Map();\n" +
               "    const bindings = new Map();\n" +
               "    \n" +
               "    function defineRegion(def) {\n" +
               "        regions.set(def.id, def);\n" +
               "    }\n" +
               "    \n" +
               "    function bind(regionId, handler) {\n" +
               "        bindings.set(regionId, handler);\n" +
               "    }\n" +
               "    \n" +
               "    function onEvent(event) {\n" +
               "        const handler = bindings.get(event.region);\n" +
               "        if (handler) {\n" +
               "            try {\n" +
               "                handler(event);\n" +
               "            } catch (e) {\n" +
               "                console.error('Error handling event:', e);\n" +
               "            }\n" +
               "        }\n" +
               "    }\n" +
               "    \n" +
               "    return {\n" +
               "        defineRegion,\n" +
               "        bind,\n" +
               "        onEvent\n" +
               "    }\n" +
               "})();\n" +
               
               "// ========== Layer 3: User Script Layer (用户层) ==========\n" +
               "// 用户只能访问受控API，不能直接访问原始输入或平台细节\n" +
               "const UserAPI = Object.freeze({\n" +
               "    onButton: function(id, fn) { InputCore.bind(id, fn); },\n" +
               "    sendKey: Env.sendKey,\n" +
               "    sendAxis: Env.sendAxis,\n" +
               "    now: Env.now,\n" +
               "    log: Env.log\n" +
               "});\n" +
               
               "// ========== 旧版本兼容层 ==========\n" +
               "// 为了兼容旧版本脚本，保留原有的rawAccess和stateMutator接口\n" +
               "const rawAccess = {\n" +
               "    getFrameId: function() { return android.getFrameId(); },\n" +
               "    getTimestamp: function() { return android.getTimestamp(); },\n" +
               "    getAxis: function(axisName) { return android.getAxis(axisName); },\n" +
               "    isGamepadButtonPressed: function(buttonName) { return android.isGamepadButtonPressed(buttonName); },\n" +
               "    getRawInput: function() { return {\n" +
               "        getGyroPitch: function() { return android.getGyroPitch(); },\n" +
               "        getGyroRoll: function() { return android.getGyroRoll(); },\n" +
               "        getGyroYaw: function() { return android.getGyroYaw(); },\n" +
               "        isTouchPressed: function() { return android.isTouchPressed(); },\n" +
               "        getTouchX: function() { return android.getTouchX(); },\n" +
               "        getTouchY: function() { return android.getTouchY(); },\n" +
               "        isButtonA: function() { return android.isButtonA(); },\n" +
               "        isButtonB: function() { return android.isButtonB(); },\n" +
               "        isButtonC: function() { return android.isButtonC(); },\n" +
               "        isButtonD: function() { return android.isButtonD(); }\n" +
               "    }; }\n" +
               "};\n" +
               "\n" +
               "const stateMutator = {\n" +
               "    holdKey: function(key) { android.holdKey(key); },\n" +
               "    releaseKey: function(key) { android.releaseKey(key); },\n" +
               "    releaseAllKeys: function() { android.releaseAllKeys(); },\n" +
               "    isKeyHeld: function(key) { return android.isKeyHeld(key); },\n" +
               "    pushEvent: function(eventType, eventData) { android.pushEvent(eventType, eventData); }\n" +
               "};\n" +
               
               "// ========== 用户脚本执行 ==========\n" +
               "// 用户代码只能访问UserAPI，不能直接访问Env或InputCore\n" +
               scriptCode +
               "</script></head><body></body></html>";
    }
    
    /**
     * 将RawInput转换为JSON字符串
     * @param rawInput 原始输入数据
     * @param frameId 帧序号
     * @return JSON字符串
     */
    private String rawInputToJson(RawInput rawInput, long frameId) {
        // 使用JSONObject构建JSON，避免手动转义错误
        org.json.JSONObject jsonObj = new org.json.JSONObject();
        
        try {
            // 基本字段
            jsonObj.put("frameId", frameId);
            jsonObj.put("gyroPitch", rawInput.getGyroPitch());
            jsonObj.put("gyroRoll", rawInput.getGyroRoll());
            jsonObj.put("gyroYaw", rawInput.getGyroYaw());
            jsonObj.put("touchPressed", rawInput.isTouchPressed());
            jsonObj.put("touchX", rawInput.getTouchX());
            jsonObj.put("touchY", rawInput.getTouchY());
            jsonObj.put("buttonA", rawInput.isButtonA());
            jsonObj.put("buttonB", rawInput.isButtonB());
            jsonObj.put("buttonC", rawInput.isButtonC());
            jsonObj.put("buttonD", rawInput.isButtonD());
            
            // 游戏手柄数据
            org.json.JSONObject gamepadObj = new org.json.JSONObject();
            
            // 轴数据
            org.json.JSONObject axesObj = new org.json.JSONObject();
            for (Map.Entry<String, Float> axis : rawInput.getGamepad().getAxes().entrySet()) {
                axesObj.put(axis.getKey(), axis.getValue());
            }
            gamepadObj.put("axes", axesObj);
            
            // 按键数据
            org.json.JSONObject buttonsObj = new org.json.JSONObject();
            for (Map.Entry<String, Boolean> button : rawInput.getGamepad().getButtons().entrySet()) {
                buttonsObj.put(button.getKey(), button.getValue());
            }
            gamepadObj.put("buttons", buttonsObj);
            
            jsonObj.put("gamepad", gamepadObj);
            
            return jsonObj.toString();
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error building raw input JSON: " + e.getMessage());
            // 返回简化的JSON格式
            return String.format(
                "{\"frameId\": %d, \"gyroPitch\": %.6f, \"gyroRoll\": %.6f, \"gyroYaw\": %.6f}",
                frameId,
                rawInput.getGyroPitch(),
                rawInput.getGyroRoll(),
                rawInput.getGyroYaw()
            );
        }
    }
    
    /**
     * 将GameInputEvent转换为JSON字符串
     * @param event 游戏输入事件
     * @return JSON字符串
     */
    private String eventToJson(GameInputEvent event) {
        return String.format(
            "{\"key\": \"%s\", \"type\": \"%s\", \"timestamp\": %d}",
            event.getKey(),
            event.getType().name(),
            event.getTimestamp()
        );
    }
    
    /**
     * 将NormalizedEvent转换为JSON字符串，用于发送到JS层
     * @param event 标准化事件
     * @return JSON字符串
     */
    private String normalizedEventToJson(NormalizedEvent event) {
        if (event == null) {
            return "null";
        }
        
        String eventJson = "{";
        
        // 通用事件属性
        eventJson += String.format("\"region\": \"%s\", ", event.getRegionId());
        eventJson += String.format("\"type\": \"%s\", ", event.getType().name());
        eventJson += String.format("\"timestamp\": %d", event.getTimestamp());
        
        // 事件类型特定属性
        switch (event.getType()) {
            case BUTTON:
                ButtonEvent buttonEvent = (ButtonEvent) event;
                eventJson += String.format(", \"pressed\": %b", buttonEvent.isPressed());
                break;
            case AXIS:
                AxisEvent axisEvent = (AxisEvent) event;
                eventJson += String.format(", \"valueX\": %.6f, \"valueY\": %.6f", axisEvent.getValueX(), axisEvent.getValueY());
                break;
            case GESTURE:
                GestureEvent gestureEvent = (GestureEvent) event;
                eventJson += String.format(", \"gestureType\": \"%s\", \"value1\": %.6f, \"value2\": %.6f, \"value3\": %.6f", 
                    gestureEvent.getGestureType().name(), 
                    gestureEvent.getValue1(), 
                    gestureEvent.getValue2(), 
                    gestureEvent.getValue3());
                break;
        }
        
        eventJson += "}";
        return eventJson;
    }
    
    /**
     * 将标准化事件发送到JS层的InputCore.onEvent函数
     * @param event 标准化事件
     */
    public void sendNormalizedEventToJs(NormalizedEvent event) {
        if (state != EngineState.LOADED) {
            return;
        }
        
        uiHandler.post(() -> {
            try {
                String eventJson = normalizedEventToJson(event);
                String jsCode = String.format(
                    "if (typeof InputCore !== 'undefined' && typeof InputCore.onEvent === 'function') { InputCore.onEvent(%s); }",
                    eventJson
                );
                webView.evaluateJavascript(jsCode, null);
            } catch (Exception e) {
                lastError = "RUNTIME_ERROR: " + e.getMessage();
                state = EngineState.ERROR;
            }
        });
    }
    
    /**
     * JavaScript桥接类
     * 提供Android与JavaScript之间的通信
     */
    private class ScriptBridge {
        // 当前是否允许使用HostServices
        private boolean allowHostServices = false;
        
        /**
         * 设置是否允许使用HostServices
         * @param allow true表示允许，false表示不允许
         */
        public void setAllowHostServices(boolean allow) {
            this.allowHostServices = allow;
        }
        
        // === RawAccess 相关方法 ===
        @JavascriptInterface
        public float getAxis(String axisName) {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                // 从当前帧的原始输入中获取轴值
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    org.json.JSONObject gamepadObj = rawJsonObj.optJSONObject("gamepad");
                    if (gamepadObj != null) {
                        return (float) gamepadObj.optDouble(axisName, 0.0);
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting axis value: " + e.getMessage());
                }
            }
            return 0.0f;
        }
        
        @JavascriptInterface
        public boolean isGamepadButtonPressed(String buttonName) {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                // 从当前帧的原始输入中获取按钮状态
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    org.json.JSONObject gamepadObj = rawJsonObj.optJSONObject("gamepad");
                    if (gamepadObj != null) {
                        return gamepadObj.optBoolean(buttonName, false);
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting button state: " + e.getMessage());
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public float getGyroPitch() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return (float) rawJsonObj.optDouble("gyroPitch", 0.0);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting gyro pitch: " + e.getMessage());
                }
            }
            return 0.0f;
        }
        
        @JavascriptInterface
        public float getGyroRoll() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return (float) rawJsonObj.optDouble("gyroRoll", 0.0);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting gyro roll: " + e.getMessage());
                }
            }
            return 0.0f;
        }
        
        @JavascriptInterface
        public float getGyroYaw() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return (float) rawJsonObj.optDouble("gyroYaw", 0.0);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting gyro yaw: " + e.getMessage());
                }
            }
            return 0.0f;
        }
        
        @JavascriptInterface
        public boolean isTouchPressed() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return rawJsonObj.optBoolean("touchPressed", false);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting touch pressed: " + e.getMessage());
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public float getTouchX() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return (float) rawJsonObj.optDouble("touchX", 0.0);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting touch X: " + e.getMessage());
                }
            }
            return 0.0f;
        }
        
        @JavascriptInterface
        public float getTouchY() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return (float) rawJsonObj.optDouble("touchY", 0.0);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting touch Y: " + e.getMessage());
                }
            }
            return 0.0f;
        }
        
        @JavascriptInterface
        public boolean isButtonA() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return rawJsonObj.optBoolean("buttonA", false);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting button A: " + e.getMessage());
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public boolean isButtonB() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return rawJsonObj.optBoolean("buttonB", false);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting button B: " + e.getMessage());
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public boolean isButtonC() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return rawJsonObj.optBoolean("buttonC", false);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting button C: " + e.getMessage());
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public boolean isButtonD() {
            if (executionResult.get() != null && JsInputScriptEngine.this.rawInputToJson != null) {
                try {
                    org.json.JSONObject rawJsonObj = new org.json.JSONObject(JsInputScriptEngine.this.rawInputToJson);
                    return rawJsonObj.optBoolean("buttonD", false);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error getting button D: " + e.getMessage());
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public long getFrameId() {
            return expectedFrameId.get();
        }
        
        @JavascriptInterface
        public long getTimestamp() {
            return System.currentTimeMillis();
        }
        
        // === StateMutator 相关方法 ===
        @JavascriptInterface
        public void holdKey(String key) {
            InputState state = executionResult.get();
            if (state != null) {
                state.getKeyboard().add(key);
            }
        }
        
        @JavascriptInterface
        public void releaseKey(String key) {
            InputState state = executionResult.get();
            if (state != null) {
                state.getKeyboard().remove(key);
            }
        }
        
        @JavascriptInterface
        public void releaseAllKeys() {
            InputState state = executionResult.get();
            if (state != null) {
                state.getKeyboard().clear();
            }
        }
        
        @JavascriptInterface
        public boolean isKeyHeld(String key) {
            InputState state = executionResult.get();
            return state != null && state.getKeyboard().contains(key);
        }
        
        @JavascriptInterface
        public void pushEvent(String eventType, String eventData) {
            // 事件推送实现，目前仅记录日志
            Log.d(TAG, "Event pushed: " + eventType + " - " + eventData);
        }
        
        // === HostServices 相关方法（需要显式允许） ===
        @JavascriptInterface
        public void log(String message) {
            if (allowHostServices) {
                Log.d(TAG, "[SCRIPT] " + message);
            }
        }
        
        @JavascriptInterface
        public void debug(String message) {
            if (allowHostServices) {
                Log.d(TAG, "[SCRIPT_DEBUG] " + message);
            }
        }
        
        @JavascriptInterface
        public void error(String message) {
            if (allowHostServices) {
                Log.e(TAG, "[SCRIPT_ERROR] " + message);
            }
        }
        
        @JavascriptInterface
        public void setMousePosition(float x, float y) {
            if (allowHostServices) {
                InputState state = executionResult.get();
                if (state != null) {
                    state.getMouse().setX(x);
                    state.getMouse().setY(y);
                }
            }
        }
        
        @JavascriptInterface
        public void setMouseButton(String button, boolean pressed) {
            if (allowHostServices) {
                InputState state = executionResult.get();
                if (state != null) {
                    switch (button.toLowerCase()) {
                        case "left":
                            state.getMouse().setLeft(pressed);
                            break;
                        case "right":
                            state.getMouse().setRight(pressed);
                            break;
                        case "middle":
                            state.getMouse().setMiddle(pressed);
                            break;
                    }
                }
            }
        }
        
        // === 内部回调方法 ===
        @JavascriptInterface
        public void onScriptError(String type, String message, long frameId) {
            if (expectedFrameId.get() == frameId) {
                lastError = type + ": " + message;
                state = EngineState.ERROR;
            }
            if (executionLatch != null) {
                executionLatch.countDown();
            }
        }
        
        @JavascriptInterface
        public void onUpdateComplete(String resultJson) {
            try {
                // 解析结果JSON
                org.json.JSONObject resultObj = new org.json.JSONObject(resultJson);
                long resultFrameId = resultObj.getLong("frameId");
                
                // 只处理最新帧的结果
                if (expectedFrameId.get() == resultFrameId) {
                    // 处理ScriptOutput结果
                    org.json.JSONObject scriptOutputObj = resultObj.optJSONObject("result");
                    if (scriptOutputObj != null) {
                        applyScriptOutput(scriptOutputObj);
                    }
                }
            } catch (org.json.JSONException e) {
                Log.e(TAG, "Error parsing script output: " + e.getMessage());
                lastError = "RUNTIME_ERROR: Invalid script output format";
                state = EngineState.ERROR;
            } finally {
                if (executionLatch != null) {
                    executionLatch.countDown();
                }
            }
        }
        
        /**
         * 应用ScriptOutput到InputState
         */
        private void applyScriptOutput(org.json.JSONObject scriptOutput) throws org.json.JSONException {
            InputState state = executionResult.get();
            if (state == null) return;
            
            // 处理heldKeys
            org.json.JSONArray heldKeysArray = scriptOutput.optJSONArray("heldKeys");
            if (heldKeysArray != null) {
                // 先清空当前按键状态
                state.getKeyboard().clear();
                
                // 添加新的按键状态
                for (int i = 0; i < heldKeysArray.length(); i++) {
                    String key = heldKeysArray.getString(i);
                    state.getKeyboard().add(key);
                }
            }
            
            // 处理events（暂时忽略，未来实现）
            // 处理debug（暂时忽略，未来实现）
        }
    }
}