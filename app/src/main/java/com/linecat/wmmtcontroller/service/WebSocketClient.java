package com.linecat.wmmtcontroller.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.linecat.wmmtcontroller.model.FormattedInputMessage;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.service.EventDelta;
import com.linecat.wmmtcontroller.service.EventMessage;
import com.linecat.wmmtcontroller.service.KeyboardEvent;
import com.linecat.wmmtcontroller.service.StateMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocket客户端
 * 负责将输入状态发送到服务器
 */
public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    private OkHttpClient client;
    private WebSocket webSocket;
    private Gson gson;
    private boolean isConnected = false;
    private String serverUrl;
    private Context context;
    // 重连尝试次数，用于指数退避重连
    private int reconnectAttempts = 0;
    // 最大重连延迟（30秒）
    private static final long MAX_RECONNECT_DELAY = 30000;
    // 连接超时时间（3秒）
    private static final long CONNECTION_TIMEOUT = 3000;
    // 超时处理Handler
    private Handler timeoutHandler;
    // 连接开始时间
    private long connectStartTime;
    // 连接结果回调
    private boolean connectionResultReported = false;
    
    // 状态ID计数器
    private long stateId = 0;
    // 事件ID计数器
    private long eventId = 0;
    // 最后确认的状态ID
    private long lastAckedStateId = 0;
    
    // 事件缓存，保存最近100个发送的事件
    private final Map<Long, String> eventCache = Collections.synchronizedMap(
        new LinkedHashMap<Long, String>(100, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
                return size() > 100;
            }
        }
    );
    
    /**
     * WebSocket客户端构造函数
     * @param context 上下文，用于发送广播
     * @param serverUrl WebSocket服务器URL
     */
    public WebSocketClient(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;
    }
    
    /**
     * 初始化WebSocket客户端
     */
    public void init() {
        Log.d(TAG, "[WebSocket] 开始执行init()方法");
        
        Log.d(TAG, "[WebSocket] 初始化OkHttpClient");
        client = new OkHttpClient();
        
        Log.d(TAG, "[WebSocket] 初始化Gson实例");
        gson = new Gson();
        
        Log.d(TAG, "[WebSocket] 初始化超时处理Handler");
        timeoutHandler = new Handler();
        
        Log.d(TAG, "[WebSocket] 初始化完成，当前服务器URL: " + serverUrl);
        Log.d(TAG, "[WebSocket] WebSocketClient已准备就绪，可以连接");
    }
    
    /**
     * 手动开始连接WebSocket
     */
    public void connect() {
        if (isConnected) {
            Log.d(TAG, "WebSocket already connected, skipping connect");
            showConnectionToast(true, 0);
            return;
        }
        
        // 重置连接结果报告标志
        connectionResultReported = false;
        // 记录连接开始时间
        connectStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "[连接开始] 准备连接到服务器: " + serverUrl);
        Log.d(TAG, "[连接开始] 连接超时时间设置为: " + CONNECTION_TIMEOUT + "ms");
        
        try {
            URI serverUri = new URI(serverUrl);
            Log.d(TAG, "[连接开始] 解析服务器URI成功: " + serverUri.toString());
            
            Request request = new Request.Builder()
                    .url(serverUri.toString())
                    .build();
            Log.d(TAG, "[连接开始] 构建WebSocket请求成功");
            
            // 设置连接超时任务
            timeoutHandler.postDelayed(() -> {
                if (!isConnected && !connectionResultReported) {
                    long elapsedTime = System.currentTimeMillis() - connectStartTime;
                    Log.e(TAG, "[连接超时] 连接服务器超时，耗时: " + elapsedTime + "ms，超过超时时间: " + CONNECTION_TIMEOUT + "ms");
                    connectionResultReported = true;
                    
                    // 清理WebSocket资源
                    if (webSocket != null) {
                        webSocket.close(1000, "Connection timeout");
                        webSocket = null;
                    }
                    
                    isConnected = false;
                    
                    // 发送WebSocket断开连接广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
                    context.sendBroadcast(intent);
                    
                    // 显示连接失败toast
                    showConnectionToast(false, elapsedTime);
                }
            }, CONNECTION_TIMEOUT);
            
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                    long elapsedTime = System.currentTimeMillis() - connectStartTime;
                    Log.d(TAG, "[连接成功] WebSocket连接成功，耗时: " + elapsedTime + "ms");
                    
                    // 取消超时任务
                    timeoutHandler.removeCallbacksAndMessages(null);
                    
                    isConnected = true;
                    connectionResultReported = true;
                    
                    // 连接成功，重置重连尝试次数
                    reconnectAttempts = 0;
                    
                    // 发送WebSocket连接成功广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_CONNECTED);
                    context.sendBroadcast(intent);
                    
                    // 显示连接成功toast
                    showConnectionToast(true, elapsedTime);
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    
                    try {
                        // 解析JSON消息
                        org.json.JSONObject jsonObj = new org.json.JSONObject(text);
                        String type = jsonObj.getString("type");
                        
                        // 处理状态确认消息
                        if ("stateAck".equals(type)) {
                            long ackedStateId = jsonObj.getLong("ackStateId");
                            lastAckedStateId = Math.max(lastAckedStateId, ackedStateId);
                            Log.d(TAG, "[消息接收] 收到状态确认: ackStateId=" + ackedStateId + ", 更新lastAckedStateId=" + lastAckedStateId);
                        }
                        
                        // 处理事件确认消息
                        else if ("eventAck".equals(type)) {
                            long ackedEventId = jsonObj.getLong("eventId");
                            Log.d(TAG, "[消息接收] 收到事件确认: eventId=" + ackedEventId);
                        }
                        
                        // 处理错误消息
                        else if ("error".equals(type)) {
                            long errorId = jsonObj.getLong("errorId");
                            String errorMsg = jsonObj.getString("message");
                            
                            // 获取对应的原始发送数据
                            String originalData = eventCache.get(errorId);
                            
                            // 构建完整的错误信息
                            String fullErrorMsg = "服务端返回错误: " + errorMsg + ", 错误ID: " + errorId + ", 原始发送数据: " + originalData;
                            
                            // 抛出错误并打印栈溢出
                            Exception error = new RuntimeException(fullErrorMsg);
                            Log.e(TAG, fullErrorMsg, error);
                            
                            // 发送WebSocket错误广播
                            Intent intent = new Intent(RuntimeEvents.ACTION_RUNTIME_ERROR);
                            intent.putExtra(RuntimeEvents.EXTRA_ERROR_TYPE, RuntimeEvents.ERROR_TYPE_WEBSOCKET_ERROR);
                            context.sendBroadcast(intent);
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "[消息接收] 解析WebSocket消息失败: " + e.getMessage() + ", 原始消息: " + text);
                    }
                }
                
                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    Log.d(TAG, "[消息接收] WebSocket二进制消息: " + bytes.hex());
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "[连接关闭中] WebSocket关闭中: " + code + " - " + reason);
                    isConnected = false;
                    webSocket.close(1000, null);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "[连接已关闭] WebSocket已关闭: " + code + " - " + reason);
                    isConnected = false;
                    
                    // 发送WebSocket断开连接广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
                    context.sendBroadcast(intent);
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                    long elapsedTime = System.currentTimeMillis() - connectStartTime;
                    Log.e(TAG, "[连接失败] WebSocket连接失败，耗时: " + elapsedTime + "ms，错误原因: " + t.getMessage(), t);
                    
                    // 取消超时任务
                    timeoutHandler.removeCallbacksAndMessages(null);
                    
                    isConnected = false;
                    connectionResultReported = true;
                    
                    // 发送WebSocket断开连接广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
                    context.sendBroadcast(intent);
                    
                    // 确保webSocket被正确清理
                    if (WebSocketClient.this.webSocket != null) {
                        WebSocketClient.this.webSocket.close(1000, "Connection failed");
                        WebSocketClient.this.webSocket = null;
                    }
                    
                    // 显示连接失败toast
                    showConnectionToast(false, elapsedTime);
                    
                    // 不自动重连，等待用户手动触发
                }
            });
            
            Log.d(TAG, "[连接开始] WebSocket连接请求已发送到服务器");
        } catch (URISyntaxException e) {
            long elapsedTime = System.currentTimeMillis() - connectStartTime;
            Log.e(TAG, "[连接失败] 无效的WebSocket URI: " + serverUrl, e);
            
            isConnected = false;
            connectionResultReported = true;
            
            // 发送WebSocket连接失败广播
            Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
            context.sendBroadcast(intent);
            
            // 显示连接失败toast
            showConnectionToast(false, elapsedTime);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - connectStartTime;
            Log.e(TAG, "[连接失败] 连接WebSocket时发生意外错误: " + e.getMessage(), e);
            
            isConnected = false;
            connectionResultReported = true;
            
            // 发送WebSocket连接失败广播
            Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
            context.sendBroadcast(intent);
            
            // 显示连接失败toast
            showConnectionToast(false, elapsedTime);
        }
    }
    
    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Manual disconnect");
            webSocket = null;
        }
        isConnected = false;
        Log.d(TAG, "WebSocket disconnected manually");
    }
    
    /**
     * 更新WebSocket服务器URL
     * @param newUrl 新的WebSocket服务器URL
     */
    public void updateWebSocketUrl(String newUrl) {
        this.serverUrl = newUrl;
        Log.d(TAG, "WebSocket URL updated to: " + newUrl);
    }
    
    /**
     * 发送输入状态到服务器
     * @param inputState 输入状态
     */
    public void sendInputState(InputState inputState) {
        try {
            // 创建包含类型字段的消息对象
            // 创建符合服务端格式的消息对象
            FormattedInputMessage message = new FormattedInputMessage(inputState);
            
            // 将消息转换为JSON
            String json = gson.toJson(message);
            
            // 尝试发送WebSocket消息
            if (isConnected && webSocket != null) {
                Log.d(TAG, "Sending message to server: " + json);
                webSocket.send(json);
            } else {
                Log.d(TAG, "WebSocket not connected, skipping send but still broadcasting event");
                Log.d(TAG, "Attempted to send message to server: " + json);
            }
            
            // 发送WebSocket发送帧成功广播，无论WebSocket是否连接
            Intent intent = new Intent(RuntimeEvents.ACTION_WS_SENT_FRAME);
            // 从json中提取frameId（如果存在），否则使用时间戳作为frameId
            long frameId = System.currentTimeMillis();
            try {
                // 尝试从json中解析frameId
                org.json.JSONObject jsonObj = new org.json.JSONObject(json);
                if (jsonObj.has("data") && jsonObj.getJSONObject("data").has("frameId")) {
                    frameId = jsonObj.getJSONObject("data").getLong("frameId");
                }
            } catch (org.json.JSONException e) {
                // 如果解析失败，继续使用时间戳
            }
            intent.putExtra(RuntimeEvents.EXTRA_FRAME_ID, frameId);
            intent.putExtra(RuntimeEvents.EXTRA_WS_MESSAGE, json);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending input state: " + e.getMessage(), e);
            
            // 发送WebSocket发送帧失败广播
            Intent intent = new Intent(RuntimeEvents.ACTION_RUNTIME_ERROR);
            intent.putExtra(RuntimeEvents.EXTRA_ERROR_TYPE, RuntimeEvents.ERROR_TYPE_WEBSOCKET_ERROR);
            context.sendBroadcast(intent);
        }
    }
    
    /**
     * 发送状态消息到服务器
     * @param keyboardState 键盘状态
     * @param gamepadState 游戏手柄状态
     * @param zeroOutput 是否为零输出
     */
    public void sendStateMessage(List<KeyboardEvent> keyboardState, StateMessage.GamepadState gamepadState, boolean zeroOutput) {
        try {
            // 生成新的状态ID
            long currentStateId = ++stateId;
            
            // 创建状态消息
            StateMessage stateMessage = new StateMessage(currentStateId, keyboardState, gamepadState, zeroOutput);
            
            // 将消息转换为JSON
            String json = gson.toJson(stateMessage);
            
            // 尝试发送WebSocket消息
            if (isConnected && webSocket != null) {
                Log.d(TAG, "Sending state message: stateId=" + currentStateId + ", keyboardStateSize=" + keyboardState.size());
                webSocket.send(json);
            } else {
                Log.d(TAG, "WebSocket not connected, skipping state send");
                Log.e(TAG, "Attempted to send state message: " + json);
            }
            
            // 发送WebSocket发送帧成功广播，无论WebSocket是否连接
            Intent intent = new Intent(RuntimeEvents.ACTION_WS_SENT_FRAME);
            intent.putExtra(RuntimeEvents.EXTRA_FRAME_ID, currentStateId);
            intent.putExtra(RuntimeEvents.EXTRA_WS_MESSAGE, json);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending state message: " + e.getMessage(), e);
            
            // 发送WebSocket发送帧失败广播
            Intent intent = new Intent(RuntimeEvents.ACTION_RUNTIME_ERROR);
            intent.putExtra(RuntimeEvents.EXTRA_ERROR_TYPE, RuntimeEvents.ERROR_TYPE_WEBSOCKET_ERROR);
            context.sendBroadcast(intent);
        }
    }
    
    /**
     * 发送事件消息到服务器
     * @param delta 事件增量
     * @param zeroOutput 是否为零输出
     */
    public void sendEventMessage(EventDelta delta, boolean zeroOutput) {
        try {
            // 生成新的事件ID
            long currentEventId = ++eventId;
            
            // 使用最后确认的状态ID作为基础状态ID
            long currentBaseStateId = lastAckedStateId;
            
            // 创建事件消息
            EventMessage eventMessage = new EventMessage(currentEventId, currentBaseStateId, delta, zeroOutput);
            
            // 将消息转换为JSON
            String json = gson.toJson(eventMessage);
            
            // 缓存事件到事件缓存中
            eventCache.put(currentEventId, json);
            
            // 尝试发送WebSocket消息
            if (isConnected && webSocket != null) {
                Log.d(TAG, "Sending event message: eventId=" + currentEventId + ", baseStateId=" + currentBaseStateId);
                webSocket.send(json);
            } else {
                Log.d(TAG, "WebSocket not connected, skipping event send");
                Log.e(TAG, "Attempted to send event message: " + json);
            }
            
            // 发送WebSocket发送帧成功广播，无论WebSocket是否连接
            Intent intent = new Intent(RuntimeEvents.ACTION_WS_SENT_FRAME);
            intent.putExtra(RuntimeEvents.EXTRA_FRAME_ID, currentEventId);
            intent.putExtra(RuntimeEvents.EXTRA_WS_MESSAGE, json);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending event message: " + e.getMessage(), e);
            
            // 发送WebSocket发送帧失败广播
            Intent intent = new Intent(RuntimeEvents.ACTION_RUNTIME_ERROR);
            intent.putExtra(RuntimeEvents.EXTRA_ERROR_TYPE, RuntimeEvents.ERROR_TYPE_WEBSOCKET_ERROR);
            context.sendBroadcast(intent);
        }
    }
    
    /**
     * 重连WebSocket
     * 不再自动重连，需要用户手动触发
     */
    private void reconnect() {
        Log.d(TAG, "Reconnect requested, but auto-reconnect is disabled. Please use manual connect.");
        // 不执行自动重连，等待用户手动触发
    }
    
    /**
     * 显示连接结果Toast
     * @param success 是否连接成功
     * @param elapsedTime 连接耗时（毫秒）
     */
    private void showConnectionToast(boolean success, long elapsedTime) {
        // 在主线程显示Toast
        new Handler(context.getMainLooper()).post(() -> {
            String message;
            if (success) {
                message = "连接成功，耗时 " + elapsedTime + "ms";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "[Toast提示] 显示连接成功提示: " + message);
            } else {
                if (elapsedTime >= CONNECTION_TIMEOUT) {
                    message = "连接超时，超过 " + CONNECTION_TIMEOUT + "ms";
                } else {
                    message = "连接失败，耗时 " + elapsedTime + "ms";
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "[Toast提示] 显示连接失败提示: " + message);
            }
        });
    }
    
    /**
     * 关闭WebSocket客户端
     */
    public void shutdown() {
        if (webSocket != null) {
            webSocket.close(1000, "Client shutdown");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
        // 清理超时任务
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        isConnected = false;
        Log.d(TAG, "WebSocketClient shutdown");
    }
    
    /**
     * 检查WebSocket是否连接
     * @return 是否连接
     */
    public boolean isConnected() {
        return isConnected;
    }
}