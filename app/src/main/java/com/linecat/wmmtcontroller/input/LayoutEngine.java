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
    }

    /**
     * 初始化布局引擎
     */
    public void init() {
        Log.d(TAG, "Layout engine initialized");
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
        Log.d(TAG, "Executing UI layer");
        uiLayerHandler.process(rawInput, currentLayout, inputState);

        // 2. Operation 层处理：抽象控制语义
        Log.d(TAG, "Executing Operation layer");
        operationLayerHandler.process(rawInput, currentLayout, inputState);

        // 3. Mapping 层处理：抽象语义 → 设备输出
        Log.d(TAG, "Executing Mapping layer");
        mappingLayerHandler.process(rawInput, currentLayout, inputState);

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
