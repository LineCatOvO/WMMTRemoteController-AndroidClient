package com.linecat.wmmtcontroller.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;

/**
 * 传输控制器
 * 负责处理通信相关功能：WebSocket 连接、消息发送、ACK 处理、Metrics 统计
 */
public class TransportController {
    private static final String TAG = "TransportController";
    private final Context context;
    private final WebSocketClient webSocketClient;
    private final RuntimeConfig runtimeConfig;
    
    // Metrics 统计
    private long totalMessagesSent = 0;
    private long totalMessagesReceived = 0;
    private long totalAckReceived = 0;
    private long lastRtt = 0;
    private long lastMessageTime = 0;
    
    // 连接信息更新广播接收器
    private final BroadcastReceiver connectionInfoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RuntimeEvents.ACTION_CONNECTION_INFO_UPDATED.equals(intent.getAction())) {
                Log.d(TAG, "Connection info updated, refreshing WebSocket URL");
                // 获取新的WebSocket URL
                String newUrl = runtimeConfig.getWebSocketUrl();
                Log.d(TAG, "New WebSocket URL: " + newUrl);
                
                // 更新WebSocket URL
                updateWebSocketUrl(newUrl);
                
                // 如果当前已连接，断开并重新连接
                if (isConnected()) {
                    Log.d(TAG, "Reconnecting with new URL");
                    disconnect();
                    connect();
                }
            }
        }
    };
    
    public TransportController(Context context, RuntimeConfig runtimeConfig) {
        this.context = context;
        this.runtimeConfig = runtimeConfig;
        // 注册连接信息更新广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(RuntimeEvents.ACTION_CONNECTION_INFO_UPDATED);
        context.registerReceiver(connectionInfoUpdateReceiver, filter);
        
        this.webSocketClient = new WebSocketClient(context, runtimeConfig.getWebSocketUrl());
    }

    /**
     * 初始化传输控制器
     */
    public void init() {
        webSocketClient.init();
        Log.d(TAG, "Transport controller initialized");
    }

    /**
     * 连接到服务器
     */
    public void connect() {
        webSocketClient.connect();
        Log.d(TAG, "Connecting to server: " + runtimeConfig.getWebSocketUrl());
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        webSocketClient.disconnect();
        Log.d(TAG, "Disconnected from server");
        
        // 主动断开连接时，也发送广播触发安全清零
        Intent disconnectIntent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
        context.sendBroadcast(disconnectIntent);
    }

    /**
     * 发送输入状态
     */
    public void sendInputState(InputState inputState) {
        if (webSocketClient.isConnected()) {
            webSocketClient.sendInputState(inputState);
            totalMessagesSent++;
            lastMessageTime = System.currentTimeMillis();
            Log.d(TAG, "Input state sent, frameId: " + inputState.getFrameId());
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return webSocketClient.isConnected();
    }

    /**
     * 更新 WebSocket URL
     */
    public void updateWebSocketUrl(String newUrl) {
        runtimeConfig.setWebSocketUrl(newUrl);
        webSocketClient.updateWebSocketUrl(newUrl);
    }

    /**
     * 获取 RTT (Round Trip Time)
     */
    public long getRtt() {
        return lastRtt;
    }

    /**
     * 获取丢包率
     */
    public double getPacketLossRate() {
        if (totalMessagesSent == 0) {
            return 0.0;
        }
        return (double) (totalMessagesSent - totalAckReceived) / totalMessagesSent;
    }

    /**
     * 处理 ACK 消息
     */
    public void handleAck(long frameId, long timestamp) {
        totalAckReceived++;
        lastRtt = System.currentTimeMillis() - timestamp;
        Log.d(TAG, "ACK received for frameId: " + frameId + ", RTT: " + lastRtt + "ms");
    }

    /**
     * 处理接收到的消息
     */
    public void handleReceivedMessage(String message) {
        totalMessagesReceived++;
        Log.d(TAG, "Message received: " + message);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        // 注销广播接收器
        try {
            context.unregisterReceiver(connectionInfoUpdateReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering connection info update receiver: " + e.getMessage());
        }
        webSocketClient.shutdown();
        Log.d(TAG, "Transport controller cleaned up");
    }
}
