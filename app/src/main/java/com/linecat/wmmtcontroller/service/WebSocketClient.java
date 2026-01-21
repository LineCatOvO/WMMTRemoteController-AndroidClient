package com.linecat.wmmtcontroller.service;

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
    private static final String SERVER_URL = "ws://localhost:8080/ws/input";
    
    private OkHttpClient client;
    private WebSocket webSocket;
    private Gson gson;
    private boolean isConnected = false;
    
    /**
     * 初始化WebSocket客户端
     */
    public void init() {
        client = new OkHttpClient();
        gson = new Gson();
        
        try {
            URI serverUri = new URI(SERVER_URL);
            Request request = new Request.Builder()
                    .url(serverUri.toString())
                    .build();
            
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                    isConnected = true;
                    Log.d(TAG, "WebSocket connected");
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
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                    isConnected = false;
                    Log.e(TAG, "WebSocket failure: " + t.getMessage(), t);
                    // 尝试重连
                    reconnect();
                }
            });
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid WebSocket URI: " + SERVER_URL, e);
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
        } catch (Exception e) {
            Log.e(TAG, "Error sending input state: " + e.getMessage(), e);
        }
    }
    
    /**
     * 重连WebSocket
     */
    private void reconnect() {
        Log.d(TAG, "Attempting to reconnect WebSocket...");
        
        // 延迟重连，避免频繁尝试
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 5秒后重连
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