package com.linecat.wmmtcontroller.input;

import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.service.TransportController;

/**
 * 设备投影器
 * 负责将抽象操作意图投影到具体的设备输出
 * 这是三层输入架构中的设备映射层
 */
public class DeviceProjector {
    private static final String TAG = "DeviceProjector";

    private final TransportController transportController;
    private final LayoutEngine layoutEngine;

    public DeviceProjector(TransportController transportController, LayoutEngine layoutEngine) {
        this.transportController = transportController;
        this.layoutEngine = layoutEngine;
    }

    /**
     * 投影到设备
     * 将抽象操作意图转换为具体的设备输出并发送
     *
     * @param rawInput 抽象操作意图
     * @param frameId  帧ID
     */
    public void projectToDevice(RawInput rawInput, long frameId) {
        // 使用布局引擎处理输入并生成最终输入状态
        InputState inputState = layoutEngine.executeLayout(rawInput, frameId);

        // 发送到服务端
        if (transportController != null && transportController.isConnected()) {
            transportController.sendInputState(inputState);
            Log.d(TAG, "Input state projected to device, frameId: " + frameId);
        } else {
            Log.d(TAG, "Not connected, skipping input state projection, frameId: " + frameId);
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return transportController != null && transportController.isConnected();
    }
}