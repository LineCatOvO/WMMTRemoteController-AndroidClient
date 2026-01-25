package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.util.Log;

import com.linecat.wmmtcontroller.control.mapping.DeviceMapping;
import com.linecat.wmmtcontroller.core.layout.EnhancedLayoutEngine;
import com.linecat.wmmtcontroller.input.DeviceProjector;
import com.linecat.wmmtcontroller.input.LayoutEngine;
import com.linecat.wmmtcontroller.util.LayoutEngineAdapter;
import com.linecat.wmmtcontroller.input.InputStateController;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.service.TransportController;

/**
 * 映射层
 * 维护最终映射设备的状态，按照规则调用网络层与服务端交互
 * 负责生成和管理控制结果状态，维护设备映射关系
 */
public class MappingLayer extends LayerBase {
    private static final String TAG = "MappingLayer";
    private InputStateController inputStateController;
    private LayoutEngine layoutEngine;
    private EnhancedLayoutEngine enhancedLayoutEngine;
    private LayoutEngineAdapter layoutEngineAdapter;
    private DeviceProjector deviceProjector;
    private TransportController transportController;

    public MappingLayer(Context context) {
        super(context);
    }

    /**
     * 设置传输控制器
     */
    public void setTransportController(TransportController transportController) {
        this.transportController = transportController;
        if (deviceProjector != null) {
            // DeviceProjector 已经在初始化时设置了 TransportController，这里不需要再次设置
        }
    }

    @Override
    public void init() {
        if (isInitialized) {
            Log.w(TAG, "Layer already initialized");
            return;
        }

        Log.d(TAG, "Initializing MappingLayer");

        // 初始化输出控制器
        inputStateController = new InputStateController();

        // 初始化布局引擎
        layoutEngine = new LayoutEngine(inputStateController);
        layoutEngine.setContext(context);
        layoutEngine.init();

        // 初始化新版布局引擎
        DeviceMapping defaultMapping = new DeviceMapping("default_mapping", "默认映射", DeviceMapping.MappingType.KEYBOARD);
        enhancedLayoutEngine = new EnhancedLayoutEngine(defaultMapping);

        // 初始化布局引擎适配器，默认使用旧引擎以保证兼容性
        layoutEngineAdapter = new LayoutEngineAdapter(layoutEngine, enhancedLayoutEngine);

        // 初始化设备投影器
        if (transportController != null) {
            deviceProjector = new DeviceProjector(transportController, layoutEngine);
        } else {
            Log.e(TAG, "Missing dependency: transportController");
            return;
        }

        isInitialized = true;
        Log.d(TAG, "MappingLayer initialized");
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

        Log.d(TAG, "Starting MappingLayer");

        // 启用输出
        inputStateController.enableOutput();

        isRunning = true;
        Log.d(TAG, "MappingLayer started");
    }

    @Override
    public void stop() {
        if (!isRunning) {
            Log.w(TAG, "Layer not running");
            return;
        }

        Log.d(TAG, "Stopping MappingLayer");

        // 禁用输出
        inputStateController.disableOutput();

        isRunning = false;
        Log.d(TAG, "MappingLayer stopped");
    }

    @Override
    public void destroy() {
        Log.d(TAG, "Destroying MappingLayer");

        if (isRunning) {
            stop();
        }

        // 清理输出控制器
        if (inputStateController != null) {
            inputStateController.clearAllOutputs();
            inputStateController = null;
        }

        // 清理布局引擎
        if (layoutEngine != null) {
            layoutEngine.reset();
            layoutEngine = null;
        }

        // 清理新版布局引擎
        if (enhancedLayoutEngine != null) {
            enhancedLayoutEngine.reset();
            enhancedLayoutEngine = null;
        }

        // 清理布局引擎适配器
        layoutEngineAdapter = null;

        // 清理设备投影器
        deviceProjector = null;

        transportController = null;

        isInitialized = false;
        Log.d(TAG, "MappingLayer destroyed");
    }

    /**
     * 更新输出状态
     */
    public void updateOutput(InputState newState) {
        if (!isInitialized || !isRunning) {
            Log.e(TAG, "Layer not initialized or running");
            return;
        }

        inputStateController.updateOutput(newState);
    }

    /**
     * 获取当前输出状态
     */
    public InputState getCurrentOutput() {
        if (!isInitialized) {
            Log.e(TAG, "Layer not initialized");
            return new InputState();
        }

        return inputStateController.getCurrentOutput();
    }

    /**
     * 清零所有输出
     */
    public void clearAllOutputs() {
        if (inputStateController != null) {
            inputStateController.clearAllOutputs();
        }
    }

    /**
     * 切换到新版布局引擎
     */
    public void switchToNewEngine() {
        if (layoutEngineAdapter != null) {
            layoutEngineAdapter.setUseNewEngine(true);
            Log.d(TAG, "Switched to new layout engine");
        }
    }

    /**
     * 切换回旧版布局引擎
     */
    public void switchToLegacyEngine() {
        if (layoutEngineAdapter != null) {
            layoutEngineAdapter.setUseNewEngine(false);
            Log.d(TAG, "Switched back to legacy layout engine");
        }
    }

    /**
     * 获取设备投影器
     */
    public DeviceProjector getDeviceProjector() {
        return deviceProjector;
    }

    /**
     * 获取布局引擎
     */
    public LayoutEngine getLayoutEngine() {
        return layoutEngine;
    }

    /**
     * 获取输出控制器
     */
    public InputStateController getInputStateController() {
        return inputStateController;
    }
}