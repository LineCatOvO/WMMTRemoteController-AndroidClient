package com.linecat.wmmtcontroller.input;

import com.linecat.wmmtcontroller.annotation.Stable;
import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 只读访问接口
 * 提供稳定的只读能力，包括raw输入、profile元信息、时间/frameId
 */
@Stable
public interface RawAccess {
    /**
     * 获取原始输入数据
     * @return 原始输入数据
     */
    RawInput getRawInput();
    
    /**
     * 获取摇杆轴值
     * @param axisName 轴名称
     * @return 轴值
     */
    float getAxis(String axisName);
    
    /**
     * 检查游戏手柄按钮是否被按下
     * @param buttonName 按钮名称
     * @return 是否被按下
     */
    boolean isGamepadButtonPressed(String buttonName);
    
    /**
     * 获取当前帧ID
     * @return 帧ID
     */
    long getFrameId();
    
    /**
     * 获取当前时间戳
     * @return 时间戳（毫秒）
     */
    long getTimestamp();
}