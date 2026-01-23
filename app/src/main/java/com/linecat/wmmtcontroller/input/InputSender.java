package com.linecat.wmmtcontroller.input;

import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.service.TransportController;

/**
 * 输入发送器
 * 负责将输入状态发送到服务端
 * 从属于InputController，处理输入数据的实际发送
 */
public class InputSender {
    private static final String TAG = "InputSender";
    
    private final TransportController transportController;
    private final LayoutEngine layoutEngine;
    
    // 发送频率控制
    private long lastSendTime = 0;
    private final long minSendInterval; // 最小发送间隔（毫秒）
    
    public InputSender(TransportController transportController, LayoutEngine layoutEngine, long minSendInterval) {
        this.transportController = transportController;
        this.layoutEngine = layoutEngine;
        this.minSendInterval = minSendInterval;
    }
    
    /**
     * 发送输入状态到服务端
     * @param rawInput 原始输入数据
     * @param frameId 帧ID
     */
    public void sendInputState(RawInput rawInput, long frameId) {
        // 检查依赖项是否为null
        if (transportController == null || layoutEngine == null) {
            Log.e(TAG, "TransportController or LayoutEngine is null, cannot send input state");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 控制发送频率
        if (currentTime - lastSendTime < minSendInterval) {
            return; // 未达到最小发送间隔，跳过发送
        }
        
        // 使用布局引擎处理输入并生成最终输入状态
        InputState inputState = layoutEngine.executeLayout(rawInput, frameId);
        
        // 发送到服务端
        if (transportController.isConnected()) {
            transportController.sendInputState(inputState);
            lastSendTime = currentTime;
            Log.d(TAG, "Input state sent, frameId: " + frameId);
        } else {
            Log.d(TAG, "Not connected, skipping input state send, frameId: " + frameId);
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        if (transportController == null) {
            Log.e(TAG, "TransportController is null");
            return false;
        }
        return transportController.isConnected();
    }
}