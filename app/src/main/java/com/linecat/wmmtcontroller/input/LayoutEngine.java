package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

import java.util.ArrayList;

/**
 * 布局引擎
 * 负责执行三层布局处理：UI 层 → Operation 层 → Mapping 层
 */
public class LayoutEngine {
    private static final String TAG = "LayoutEngine";
    
    // 用于控制日志打印频率的变量
    private static long lastLayoutLogTime = 0;
    private static final long LAYOUT_LOG_INTERVAL = 5000; // 5秒间隔
    private static int layoutExecutionCount = 0;

    // 各层处理器
    private UILayerHandler uiLayerHandler;
    private OperationLayerHandler operationLayerHandler;
    private MappingLayerHandler mappingLayerHandler;

    // 当前布局快照
    private LayoutSnapshot currentLayout;
    
    // 输出控制器
    private OutputController outputController;
    
    // 布局加载器
    private LayoutLoader layoutLoader;
    
    // 默认布局JSON字符串
    private static final String DEFAULT_LAYOUT_JSON = "{\"layoutId\": \"basic_racing_layout\", \"version\": 1, \"description\": \"Basic racing layout with throttle, brake, gear shift and gyro steering\", \"elements\": [{\"id\": \"steering_wheel\", \"type\": \"gyro\", \"displayOnly\": true, \"position\": {\"x\": 0.5, \"y\": 0.15}, \"size\": {\"width\": 0.4, \"height\": 0.25}, \"mapping\": {\"axis\": \"LX\", \"source\": \"gyroscope\", \"sensitivity\": 1.0}}, {\"id\": \"gear_up\", \"type\": \"button\", \"position\": {\"x\": 0.05, \"y\": 0.55}, \"size\": {\"width\": 0.12, \"height\": 0.15}, \"mapping\": {\"button\": \"RB\"}}, {\"id\": \"gear_down\", \"type\": \"button\", \"position\": {\"x\": 0.05, \"y\": 0.72}, \"size\": {\"width\": 0.12, \"height\": 0.15}, \"mapping\": {\"button\": \"LB\"}}, {\"id\": \"brake\", \"type\": \"analog\", \"position\": {\"x\": 0.20, \"y\": 0.60}, \"size\": {\"width\": 0.18, \"height\": 0.30}, \"mapping\": {\"trigger\": \"LT\"}}, {\"id\": \"throttle\", \"type\": \"analog\", \"position\": {\"x\": 0.75, \"y\": 0.60}, \"size\": {\"width\": 0.20, \"height\": 0.30}, \"mapping\": {\"trigger\": \"RT\"}}]} ";
    
    public LayoutEngine(OutputController outputController) {
        this.outputController = outputController;
        this.uiLayerHandler = new UILayerHandler();
        this.operationLayerHandler = new OperationLayerHandler();
        this.mappingLayerHandler = new MappingLayerHandler();
    }

    /**
     * 设置上下文，用于创建 LayoutLoader
     */
    public void setContext(Context context) {
        this.layoutLoader = new LayoutLoader(context);
        // 上下文设置后，加载默认布局
        try {
            LayoutSnapshot defaultLayout = layoutLoader.parseLayoutJson(DEFAULT_LAYOUT_JSON);
            if (defaultLayout != null) {
                setLayout(defaultLayout);
                Log.d(TAG, "Loaded default layout after setting context");
            } else {
                Log.w(TAG, "Failed to parse default layout after setting context");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading default layout after setting context", e);
        }
    }

    /**
     * 初始化布局引擎
     */
    public void init() {
        Log.d(TAG, "Layout engine initialized");
        // 如果没有设置上下文，不加载默认布局，等待设置上下文后再加载
        if (layoutLoader != null) {
            try {
                LayoutSnapshot defaultLayout = layoutLoader.parseLayoutJson(DEFAULT_LAYOUT_JSON);
                if (defaultLayout != null) {
                    setLayout(defaultLayout);
                    Log.d(TAG, "Loaded default layout");
                } else {
                    Log.w(TAG, "Failed to parse default layout");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading default layout", e);
            }
        } else {
            Log.d(TAG, "LayoutLoader not initialized yet, will load default layout when context is set");
        }
    }

    /**
     * 设置当前布局
     */
    public void setLayout(LayoutSnapshot layout) {
        this.currentLayout = layout;
        Log.d(TAG, "Layout set: " + (layout != null ? layout.toString() : "null"));

        // 布局切换时清零输出
        outputController.clearAllOutputs();
    }

    /**
     * 从 JSON 字符串加载布局
     */
    public void loadLayoutFromJson(String jsonString) {
        try {
            if (layoutLoader != null) {
                LayoutSnapshot layout = layoutLoader.parseLayoutJson(jsonString);
                setLayout(layout);
                Log.d(TAG, "Layout loaded from JSON string");
            } else {
                Log.e(TAG, "LayoutLoader not initialized, cannot load layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading layout from JSON string", e);
            // 加载失败时设置空布局
            setLayout(null);
        }
    }

    /**
     * 执行布局处理
     */
    public InputState executeLayout(RawInput rawInput, long frameId) {
        if (currentLayout == null) {
            // 创建一个默认的空布局，避免无限输出警告日志
            currentLayout = new LayoutSnapshot(new ArrayList<>());
        }

        // 创建输入状态
        InputState inputState = new InputState();
        inputState.setFrameId(frameId);

        // 1. UI 层处理：原始输入 → 抽象值
        uiLayerHandler.process(rawInput, currentLayout, inputState);

        // 2. Operation 层处理：抽象控制语义
        operationLayerHandler.process(rawInput, currentLayout, inputState);

        // 3. Mapping 层处理：抽象语义 → 设备输出
        mappingLayerHandler.process(rawInput, currentLayout, inputState);
        
        // 按时间间隔打印日志
        layoutExecutionCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLayoutLogTime >= LAYOUT_LOG_INTERVAL) {
            Log.d(TAG, "Layout execution summary - Total executions in interval: " + layoutExecutionCount + ", frameId: " + frameId);
            // 重置计数器
            layoutExecutionCount = 0;
            lastLayoutLogTime = currentTime;
        }

        // 更新输出状态
        outputController.updateOutput(inputState);

        return inputState;
    }

    /**
     * 重置布局引擎
     */
    public void reset() {
        this.currentLayout = null;
        uiLayerHandler.reset();
        operationLayerHandler.reset();
        mappingLayerHandler.reset();
        outputController.clearAllOutputs();
        Log.d(TAG, "Layout engine reset");
    }

    /**
     * 获取当前布局
     */
    public LayoutSnapshot getCurrentLayout() {
        return currentLayout;
    }
}
