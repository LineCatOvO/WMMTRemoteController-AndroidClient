package com.linecat.wmmtcontroller.input;

/**
 * 轴事件
 * 表示区域的轴状态变化，如摇杆移动、滑动等
 */
public class AxisEvent implements NormalizedEvent {
    
    private final String regionId;       // 关联的区域ID
    private final float valueX;          // X轴值（-1.0 到 1.0）
    private final float valueY;          // Y轴值（-1.0 到 1.0）
    private final long timestamp;        // 事件时间戳
    
    /**
     * 构造函数
     */
    public AxisEvent(String regionId, float valueX, float valueY, long timestamp) {
        this.regionId = regionId;
        this.valueX = valueX;
        this.valueY = valueY;
        this.timestamp = timestamp;
    }
    
    @Override
    public EventType getType() {
        return EventType.AXIS;
    }
    
    @Override
    public String getRegionId() {
        return regionId;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取X轴值
     * @return X轴值（-1.0 到 1.0）
     */
    public float getValueX() {
        return valueX;
    }
    
    /**
     * 获取Y轴值
     * @return Y轴值（-1.0 到 1.0）
     */
    public float getValueY() {
        return valueY;
    }
    
    /**
     * 创建轴事件的静态工厂方法
     * @param regionId 区域ID
     * @param valueX X轴值
     * @param valueY Y轴值
     * @return 轴事件实例
     */
    public static AxisEvent create(String regionId, float valueX, float valueY) {
        return new AxisEvent(regionId, valueX, valueY, System.currentTimeMillis());
    }
    
    /**
     * 创建单轴事件的静态工厂方法
     * @param regionId 区域ID
     * @param value 轴值
     * @return 轴事件实例（Y轴值为0）
     */
    public static AxisEvent createSingleAxis(String regionId, float value) {
        return new AxisEvent(regionId, value, 0f, System.currentTimeMillis());
    }
}
