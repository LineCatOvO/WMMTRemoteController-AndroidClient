package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.util.Log;

import com.linecat.wmmtcontroller.input.IntentComposer;
import com.linecat.wmmtcontroller.input.InputInterpreter;
import com.linecat.wmmtcontroller.input.RegionResolver;
import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 转换层
 * 将UI输入转换为抽象操作，为映射层提供终点事件
 * 负责将原始输入转换为抽象操作意图
 */
public class ConversionLayer extends LayerBase {
    private static final String TAG = "ConversionLayer";
    private IntentComposer intentComposer;
    private InputInterpreter inputInterpreter;
    private RegionResolver regionResolver;

    public ConversionLayer(Context context) {
        super(context);
    }

    @Override
    public void init() {
        if (isInitialized) {
            Log.w(TAG, "Layer already initialized");
            return;
        }

        Log.d(TAG, "Initializing ConversionLayer");

        // 初始化意图合成器
        intentComposer = new IntentComposer();

        // 初始化输入解释器
        inputInterpreter = new InputInterpreter();

        // 初始化区域解析器
        regionResolver = new RegionResolver(context);

        isInitialized = true;
        Log.d(TAG, "ConversionLayer initialized");
    }

    @Override
    public void start() {
        if (!isInitialized) {
            Log.e(TAG, "Cannot start layer, not initialized");
            return;
        }

        if (isRunning) {
            Log.w(TAG, "Layer already running");
            return;
        }

        Log.d(TAG, "Starting ConversionLayer");

        isRunning = true;
        Log.d(TAG, "ConversionLayer started");
    }

    @Override
    public void stop() {
        if (!isRunning) {
            Log.w(TAG, "Layer not running");
            return;
        }

        Log.d(TAG, "Stopping ConversionLayer");

        isRunning = false;
        Log.d(TAG, "ConversionLayer stopped");
    }

    @Override
    public void destroy() {
        Log.d(TAG, "Destroying ConversionLayer");

        if (isRunning) {
            stop();
        }

        // 清理资源
        intentComposer = null;
        inputInterpreter = null;
        regionResolver = null;

        isInitialized = false;
        Log.d(TAG, "ConversionLayer destroyed");
    }

    /**
     * 合成操作意图
     * 将原始输入转换为抽象操作意图
     */
    public RawInput composeIntent(RawInput rawInput) {
        if (!isInitialized || !isRunning) {
            Log.e(TAG, "Layer not initialized or running");
            return rawInput;
        }

        return intentComposer.composeIntent(rawInput);
    }

    /**
     * 解释输入
     * 将抽象操作意图解释为具体的命令
     */
    public void interpretInput(RawInput processedInput) {
        if (!isInitialized || !isRunning) {
            Log.e(TAG, "Layer not initialized or running");
            return;
        }

        // 这里可以添加输入解释逻辑
        // inputInterpreter.interpret(processedInput);
    }

    /**
     * 解析区域
     * 确定输入事件发生的区域
     */
    public void resolveRegion(float x, float y) {
        if (!isInitialized || !isRunning) {
            Log.e(TAG, "Layer not initialized or running");
            return;
        }

        // 这里可以添加区域解析逻辑
        // regionResolver.resolve(x, y);
    }

    /**
     * 获取意图合成器
     */
    public IntentComposer getIntentComposer() {
        return intentComposer;
    }

    /**
     * 获取输入解释器
     */
    public InputInterpreter getInputInterpreter() {
        return inputInterpreter;
    }

    /**
     * 获取区域解析器
     */
    public RegionResolver getRegionResolver() {
        return regionResolver;
    }
}