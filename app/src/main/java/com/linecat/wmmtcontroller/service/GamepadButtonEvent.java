package com.linecat.wmmtcontroller.service;

import com.google.gson.annotations.SerializedName;

/**
 * 游戏手柄按键事件类
 * 符合技术设计文档中的游戏手柄按键事件结构
 */
public class GamepadButtonEvent {
    @SerializedName("buttonId")
    private String buttonId;

    @SerializedName("eventType")
    private String eventType;

    // 事件类型常量
    public static final String EVENT_TYPE_PRESSED = "pressed";
    public static final String EVENT_TYPE_RELEASED = "released";
    public static final String EVENT_TYPE_HELD = "held";

    // 构造函数
    public GamepadButtonEvent(String buttonId, String eventType) {
        this.buttonId = buttonId;
        this.eventType = eventType;
    }

    // getter方法
    public String getButtonId() {
        return buttonId;
    }

    public String getEventType() {
        return eventType;
    }

    // 工厂方法
    public static GamepadButtonEvent pressed(String buttonId) {
        return new GamepadButtonEvent(buttonId, EVENT_TYPE_PRESSED);
    }

    public static GamepadButtonEvent released(String buttonId) {
        return new GamepadButtonEvent(buttonId, EVENT_TYPE_RELEASED);
    }

    public static GamepadButtonEvent held(String buttonId) {
        return new GamepadButtonEvent(buttonId, EVENT_TYPE_HELD);
    }
}
