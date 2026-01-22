package com.linecat.wmmtcontroller.service;

import android.content.Context;
import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;

/**
 * 输出派发器实现
 * 负责将JS层的输出命令派发到Android系统
 * 是Java输出层的核心组件，JS层只能通过此接口与Android系统交互
 */
public class OutputDispatcherImpl implements OutputDispatcher {
    private static final String TAG = "OutputDispatcherImpl";
    
    private Context context;
    private WebSocketClient webSocketClient;
    
    /**
     * 构造函数
     * @param context 上下文
     * @param webSocketClient WebSocket客户端，用于发送输出命令
     */
    public OutputDispatcherImpl(Context context, WebSocketClient webSocketClient) {
        this.context = context;
        this.webSocketClient = webSocketClient;
    }
    
    @Override
    public void sendKey(int keyCode, boolean pressed) {
        Log.d(TAG, "sendKey: " + keyCode + " pressed: " + pressed);
        
        // 创建一个临时InputState用于发送按键事件
        // 实际应用中，应该从当前运行时状态获取InputState
        InputState inputState = new InputState();
        
        // 根据按键状态更新InputState
        String keyName = getKeyName(keyCode);
        if (keyName != null) {
            if (pressed) {
                inputState.getKeyboard().add(keyName);
            } else {
                inputState.getKeyboard().remove(keyName);
            }
        }
        
        // 通过WebSocket发送InputState
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.sendInputState(inputState);
        }
    }
    
    @Override
    public void sendAxis(int axisId, float value) {
        Log.d(TAG, "sendAxis: " + axisId + " value: " + value);
        
        // 轴事件处理，目前暂未实现
        // 实际应用中，应该根据轴ID更新对应的轴值到InputState
    }
    
    @Override
    public void sendMacro(String macroId) {
        Log.d(TAG, "sendMacro: " + macroId);
        
        // 宏事件处理，目前暂未实现
        // 实际应用中，应该根据宏ID执行对应的宏命令
    }
    
    /**
     * 根据按键代码获取按键名称
     * @param keyCode 按键代码
     * @return 按键名称
     */
    private String getKeyName(int keyCode) {
        // 这里需要根据实际的按键映射关系实现
        // 暂时返回一个简单的映射
        switch (keyCode) {
            case 65: // A
                return "A";
            case 66: // B
                return "B";
            case 67: // C
                return "C";
            case 68: // D
                return "D";
            case 38: // UP
                return "UP";
            case 40: // DOWN
                return "DOWN";
            case 37: // LEFT
                return "LEFT";
            case 39: // RIGHT
                return "RIGHT";
            default:
                return String.valueOf(keyCode);
        }
    }
    
    /**
     * 更新WebSocket客户端
     * @param webSocketClient WebSocket客户端
     */
    public void setWebSocketClient(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }
}