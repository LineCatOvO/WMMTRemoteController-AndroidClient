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
    
    // 用于控制日志打印频率的变量
    private static long lastProjectorLogTime = 0;
    private static final long PROJECTOR_LOG_INTERVAL = 5000; // 5秒间隔
    private static int projectorCallCount = 0;

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
            
            // 按时间间隔打印日志
            projectorCallCount++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProjectorLogTime >= PROJECTOR_LOG_INTERVAL) {

                // 重置计数器
                projectorCallCount = 0;
                lastProjectorLogTime = currentTime;
            }
        } else {
            // 按时间间隔打印日志
            projectorCallCount++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProjectorLogTime >= PROJECTOR_LOG_INTERVAL) {

                // 重置计数器
                projectorCallCount = 0;
                lastProjectorLogTime = currentTime;
            }
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return transportController != null && transportController.isConnected();
    }
}