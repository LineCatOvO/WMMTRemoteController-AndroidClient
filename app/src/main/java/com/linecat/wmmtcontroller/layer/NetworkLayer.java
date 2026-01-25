package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.service.RuntimeConfig;
import com.linecat.wmmtcontroller.service.TransportController;

/**
 * 网络层
 * 负责websocket的访问接口实现
 * 负责处理通信相关功能：WebSocket 连接、消息发送、ACK 处理、Metrics 统计
 */
public class NetworkLayer extends LayerBase {
    private static final String TAG = "NetworkLayer";
    private TransportController transportController;
    private RuntimeConfig runtimeConfig;

    public NetworkLayer(Context context) {
        super(context);
    }

    /**
     * 设置运行时配置
     */
    public void setRuntimeConfig(RuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    public void init() {
        if (isInitialized) {
            Log.w(TAG, "Layer already initialized");
            return;
        }

        Log.d(TAG, "Initializing NetworkLayer");

        if (runtimeConfig == null) {
            Log.e(TAG, "Missing dependency: runtimeConfig");
            return;
        }

        // 初始化传输控制器
        transportController = new TransportController(context, runtimeConfig);
        transportController.init();

        isInitialized = true;
        Log.d(TAG, "NetworkLayer initialized");
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

        Log.d(TAG, "Starting NetworkLayer");

        // 连接到服务器
        transportController.connect();

        isRunning = true;
        Log.d(TAG, "NetworkLayer started");
    }

    @Override
    public void stop() {
        if (!isRunning) {
            Log.w(TAG, "Layer not running");
            return;
        }

        Log.d(TAG, "Stopping NetworkLayer");

        // 断开连接
        transportController.disconnect();

        isRunning = false;
        Log.d(TAG, "NetworkLayer stopped");
    }

    @Override
    public void destroy() {
        Log.d(TAG, "Destroying NetworkLayer");

        if (isRunning) {
            stop();
        }

        // 清理传输控制器
        if (transportController != null) {
            transportController.cleanup();
            transportController = null;
        }

        runtimeConfig = null;

        isInitialized = false;
        Log.d(TAG, "NetworkLayer destroyed");
    }

    /**
     * 发送输入状态
     */
    public void sendInputState(InputState inputState) {
        if (!isInitialized || !isRunning) {
            Log.e(TAG, "Layer not initialized or running");
            return;
        }

        transportController.sendInputState(inputState);
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        if (!isInitialized) {
            Log.e(TAG, "Layer not initialized");
            return false;
        }

        return transportController.isConnected();
    }

    /**
     * 更新 WebSocket URL
     */
    public void updateWebSocketUrl(String newUrl) {
        if (!isInitialized) {
            Log.e(TAG, "Layer not initialized");
            return;
        }

        transportController.updateWebSocketUrl(newUrl);
    }

    /**
     * 获取 RTT (Round Trip Time)
     */
    public long getRtt() {
        if (!isInitialized) {
            Log.e(TAG, "Layer not initialized");
            return 0;
        }

        return transportController.getRtt();
    }

    /**
     * 获取丢包率
     */
    public double getPacketLossRate() {
        if (!isInitialized) {
            Log.e(TAG, "Layer not initialized");
            return 0.0;
        }

        return transportController.getPacketLossRate();
    }

    /**
     * 获取传输控制器
     */
    public TransportController getTransportController() {
        return transportController;
    }
}