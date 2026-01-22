package com.linecat.wmmtcontroller.input;

/**
 * 标准化事件接口
 * 定义Java层到JS层的唯一通信格式
 * 所有事件必须实现此接口，确保语义化和一致性
 */
public interface NormalizedEvent {
    
    /**
     * 获取事件关联的区域ID
     * @return 区域ID
     */
    String getRegionId();
    
    /**
     * 获取事件时间戳
     * @return 时间戳（毫秒）
     */
    long getTimestamp();
    
    /**
     * 获取事件类型
     * @return 事件类型
     */
    EventType getType();
    
    /**
     * 事件类型枚举
     */
    enum EventType {
        BUTTON,  // 按钮事件
        AXIS,    // 轴事件
        GESTURE  // 手势事件
    }
}
