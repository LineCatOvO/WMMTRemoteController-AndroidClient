package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.util.Log;

import com.linecat.wmmtcontroller.floatwindow.OverlayController;
import com.linecat.wmmtcontroller.input.InteractionCapture;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.service.TransportController;

/**
 * UI输入层
 * 负责浮窗、显示、事件绑定
 * 负责悬浮球的创建、管理和命令处理
 */
public class UIInputLayer extends LayerBase {
    private static final String TAG = "UIInputLayer";
    private OverlayController overlayController;
    private TransportController transportController;
    private InteractionCapture interactionCapture;

    public UIInputLayer(Context context) {
        super(context);
    }

    /**
     * 设置传输控制器
     */
    public void setTransportController(TransportController transportController) {
        this.transportController = transportController;
        if (overlayController != null) {
            overlayController.setTransportController(transportController);
        }
    }

    /**
     * 设置交互捕获器
     */
    public void setInteractionCapture(InteractionCapture interactionCapture) {
        this.interactionCapture = interactionCapture;
        if (overlayController != null) {
            overlayController.setInputController(interactionCapture);
        }
    }

    @Override
    public void init() {
        if (isInitialized) {
            Log.w(TAG, "Layer already initialized");
            return;
        }

        Log.d(TAG, "Initializing UIInputLayer");

        // 初始化悬浮球控制器
        overlayController = new OverlayController(context);

        // 设置依赖组件
        if (transportController != null) {
            overlayController.setTransportController(transportController);
        }

        if (interactionCapture != null) {
            overlayController.setInputController(interactionCapture);
        }

        isInitialized = true;
        Log.d(TAG, "UIInputLayer initialized");
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

        Log.d(TAG, "Starting UIInputLayer");

        // 显示悬浮球
        overlayController.showOverlay();

        isRunning = true;
        Log.d(TAG, "UIInputLayer started");
    }

    @Override
    public void stop() {
        if (!isRunning) {
            Log.w(TAG, "Layer not running");
            return;
        }

        Log.d(TAG, "Stopping UIInputLayer");

        // 隐藏悬浮球
        overlayController.hideOverlay();

        isRunning = false;
        Log.d(TAG, "UIInputLayer stopped");
    }

    @Override
    public void destroy() {
        Log.d(TAG, "Destroying UIInputLayer");

        if (isRunning) {
            stop();
        }

        // 销毁悬浮球控制器
        if (overlayController != null) {
            overlayController.destroy();
            overlayController = null;
        }

        transportController = null;
        interactionCapture = null;

        isInitialized = false;
        Log.d(TAG, "UIInputLayer destroyed");
    }

    /**
     * 更新悬浮球状态文本
     */
    public void updateFloatWindowStatus(String status) {
        if (overlayController != null) {
            overlayController.updateStatus(status);
        }
    }

    /**
     * 显示连接错误提示
     */
    public void showConnectionError() {
        if (overlayController != null) {
            overlayController.showConnectionError();
        }
    }

    /**
     * 设置当前布局
     */
    public void setCurrentLayout(LayoutSnapshot layout) {
        if (overlayController != null) {
            overlayController.setCurrentLayout(layout);
        }
    }

    /**
     * 获取悬浮球控制器
     */
    public OverlayController getOverlayController() {
        return overlayController;
    }
}