package com.linecat.wmmtcontroller.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.linecat.wmmtcontroller.model.InputState;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
        client = new OkHttpClient();
        gson = new Gson();
        
        try {
            URI serverUri = new URI(serverUrl);
            Request request = new Request.Builder()
                    .url(serverUri.toString())
                    .build();
            
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                    isConnected = true;
                    Log.d(TAG, "WebSocket connected");
                    
                    // 连接成功，重置重连尝试次数
                    reconnectAttempts = 0;
                    
                    // 发送WebSocket连接成功广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_CONNECTED);
                    context.sendBroadcast(intent);
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    Log.d(TAG, "WebSocket message: " + text);
                }
                
                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    Log.d(TAG, "WebSocket message (bytes): " + bytes.hex());
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    isConnected = false;
                    webSocket.close(1000, null);
                    Log.d(TAG, "WebSocket closing: " + code + " - " + reason);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    isConnected = false;
                    Log.d(TAG, "WebSocket closed: " + code + " - " + reason);
                    
                    // 发送WebSocket断开连接广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
                    context.sendBroadcast(intent);
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                    isConnected = false;
                    Log.e(TAG, "WebSocket failure: " + t.getMessage(), t);
                    
                    // 发送WebSocket断开连接广播
                    Intent intent = new Intent(RuntimeEvents.ACTION_WS_DISCONNECTED);
                    context.sendBroadcast(intent);
                    
                    // 尝试重连
                    reconnect();
                }
            });
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid WebSocket URI: " + serverUrl, e);
        }
        
        Log.d(TAG, "WebSocketClient initialized");
    }
    
    /**
     * 发送输入状态到服务器
     * @param inputState 输入状态
     */
    public void sendInputState(InputState inputState) {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "WebSocket not connected, skipping send");
            return;
        }
        
        try {
            // 将输入状态转换为JSON
            String json = gson.toJson(inputState);
            
            // 发送WebSocket消息
            webSocket.send(json);
            
            // 发送WebSocket发送帧成功广播
            Intent intent = new Intent(RuntimeEvents.ACTION_WS_SENT_FRAME);
            // 从json中提取frameId（如果存在），否则使用时间戳作为frameId
            long frameId = System.currentTimeMillis();
            try {
                // 尝试从json中解析frameId
                org.json.JSONObject jsonObj = new org.json.JSONObject(json);
                if (jsonObj.has("frameId")) {
                    frameId = jsonObj.getLong("frameId");
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
     * 重连WebSocket
     * 使用指数退避算法计算重连延迟
     */
    private void reconnect() {
        // 递增重连尝试次数
        reconnectAttempts++;
        
        // 计算指数退避延迟：5秒 * (2 ^ (reconnectAttempts - 1))
        long calculatedDelay = 5000 * (long) Math.pow(2, reconnectAttempts - 1);
        
        // 确保延迟不超过最大值
        final long delay = Math.min(calculatedDelay, MAX_RECONNECT_DELAY);
        
        Log.d(TAG, "Attempting to reconnect WebSocket (attempt " + reconnectAttempts + ") with delay " + delay + "ms...");
        
        // 延迟重连，避免频繁尝试
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                init();
            } catch (InterruptedException e) {
                Log.e(TAG, "Reconnect interrupted", e);
            }
        }).start();
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