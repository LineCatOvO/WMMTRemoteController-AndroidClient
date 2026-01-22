package com.linecat.wmmtcontroller.input;

/**
 * 按钮事件
 * 表示区域被按下或释放的事件
 */
public class ButtonEvent implements NormalizedEvent {
    
    private final String regionId;       // 关联的区域ID
    private final boolean pressed;       // 按下状态
    private final long timestamp;        // 事件时间戳
    
    /**
     * 构造函数
     */
    public ButtonEvent(String regionId, boolean pressed, long timestamp) {
        this.regionId = regionId;
        this.pressed = pressed;
        this.timestamp = timestamp;
    }
    
    @Override
    public EventType getType() {
        return EventType.BUTTON;
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
     * 获取按下状态
     * @return 按下状态
     */
    public boolean isPressed() {
        return pressed;
    }
    
    /**
     * 创建按钮事件的静态工厂方法
     * @param regionId 区域ID
     * @param pressed 按下状态
     * @return 按钮事件实例
     */
    public static ButtonEvent create(String regionId, boolean pressed) {
        return new ButtonEvent(regionId, pressed, System.currentTimeMillis());
    }
}
