package com.linecat.wmmtcontroller.input;

/**
 * 手势事件
 * 表示复杂手势，如滑动、缩放、旋转等
 */
public class GestureEvent implements NormalizedEvent {
    
    /**
     * 手势类型枚举
     */
    public enum GestureType {
        SWIPE,       // 滑动
        PINCH,       // 缩放
        ROTATE,      // 旋转
        LONG_PRESS,  // 长按
        DOUBLE_TAP   // 双击
    }
    
    private final String regionId;       // 关联的区域ID
    private final GestureType gestureType;  // 手势类型
    private final float value1;          // 手势参数1（根据手势类型不同而含义不同）
    private final float value2;          // 手势参数2（根据手势类型不同而含义不同）
    private final float value3;          // 手势参数3（根据手势类型不同而含义不同）
    private final long timestamp;        // 事件时间戳
    
    /**
     * 构造函数
     */
    public GestureEvent(String regionId, GestureType gestureType, float value1, float value2, float value3, long timestamp) {
        this.regionId = regionId;
        this.gestureType = gestureType;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.timestamp = timestamp;
    }
    
    @Override
    public EventType getType() {
        return EventType.GESTURE;
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
     * 获取手势类型
     * @return 手势类型
     */
    public GestureType getGestureType() {
        return gestureType;
    }
    
    /**
     * 获取手势参数1
     * @return 手势参数1
     */
    public float getValue1() {
        return value1;
    }
    
    /**
     * 获取手势参数2
     * @return 手势参数2
     */
    public float getValue2() {
        return value2;
    }
    
    /**
     * 获取手势参数3
     * @return 手势参数3
     */
    public float getValue3() {
        return value3;
    }
    
    /**
     * 创建滑动手势事件
     * @param regionId 区域ID
     * @param directionX X方向（-1.0 到 1.0）
     * @param directionY Y方向（-1.0 到 1.0）
     * @param velocity 速度
     * @return 手势事件实例
     */
    public static GestureEvent createSwipe(String regionId, float directionX, float directionY, float velocity) {
        return new GestureEvent(regionId, GestureType.SWIPE, directionX, directionY, velocity, System.currentTimeMillis());
    }
    
    /**
     * 创建缩放手势事件
     * @param regionId 区域ID
     * @param scaleFactor 缩放因子
     * @return 手势事件实例
     */
    public static GestureEvent createPinch(String regionId, float scaleFactor) {
        return new GestureEvent(regionId, GestureType.PINCH, scaleFactor, 0f, 0f, System.currentTimeMillis());
    }
    
    /**
     * 创建旋转手势事件
     * @param regionId 区域ID
     * @param rotationDegrees 旋转角度（度）
     * @return 手势事件实例
     */
    public static GestureEvent createRotate(String regionId, float rotationDegrees) {
        return new GestureEvent(regionId, GestureType.ROTATE, rotationDegrees, 0f, 0f, System.currentTimeMillis());
    }
    
    /**
     * 创建长按手势事件
     * @param regionId 区域ID
     * @return 手势事件实例
     */
    public static GestureEvent createLongPress(String regionId) {
        return new GestureEvent(regionId, GestureType.LONG_PRESS, 0f, 0f, 0f, System.currentTimeMillis());
    }
    
    /**
     * 创建双击手势事件
     * @param regionId 区域ID
     * @return 手势事件实例
     */
    public static GestureEvent createDoubleTap(String regionId) {
        return new GestureEvent(regionId, GestureType.DOUBLE_TAP, 0f, 0f, 0f, System.currentTimeMillis());
    }
}
