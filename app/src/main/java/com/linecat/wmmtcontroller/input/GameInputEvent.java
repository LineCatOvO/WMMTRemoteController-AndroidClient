package com.linecat.wmmtcontroller.input;

/**
 * 游戏输入事件类
 * 用于表示离散的游戏输入事件
 * 包含瞬时性的输入动作
 */
public class GameInputEvent {
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        PRESS,    // 按下事件
        RELEASE,  // 释放事件
        TAP,      // 点击事件（快速按下并释放）
        LONG_PRESS // 长按事件
    }
    
    // 事件对应的按键
    private String key;
    
    // 事件类型
    private EventType type;
    
    // 事件发生的时间戳
    private long timestamp;
    
    /**
     * 构造函数
     * @param key 事件对应的按键
     * @param type 事件类型
     */
    public GameInputEvent(String key, EventType type) {
        this.key = key;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取事件对应的按键
     * @return 按键名称
     */
    public String getKey() {
        return key;
    }
    
    /**
     * 设置事件对应的按键
     * @param key 按键名称
     */
    public void setKey(String key) {
        this.key = key;
    }
    
    /**
     * 获取事件类型
     * @return 事件类型枚举值
     */
    public EventType getType() {
        return type;
    }
    
    /**
     * 设置事件类型
     * @param type 事件类型枚举值
     */
    public void setType(EventType type) {
        this.type = type;
    }
    
    /**
     * 获取事件时间戳
     * @return 事件发生的毫秒时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 设置事件时间戳
     * @param timestamp 事件发生的毫秒时间戳
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "GameInputEvent{" +
                "key='" + key + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
}