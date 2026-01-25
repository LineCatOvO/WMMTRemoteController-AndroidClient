package com.linecat.wmmtcontroller.service;

import com.google.gson.annotations.SerializedName;

/**
 * 键盘事件类
 * 符合技术设计文档中的键盘事件结构
 */
public class KeyboardEvent {
    @SerializedName("keyId")
    private String keyId;

    @SerializedName("eventType")
    private String eventType;

    // 事件类型常量
    public static final String EVENT_TYPE_PRESSED = "pressed";
    public static final String EVENT_TYPE_RELEASED = "released";
    public static final String EVENT_TYPE_HELD = "held";

    // 构造函数
    public KeyboardEvent(String keyId, String eventType) {
        this.keyId = keyId;
        this.eventType = eventType;
    }

    // getter方法
    public String getKeyId() {
        return keyId;
    }

    public String getEventType() {
        return eventType;
    }

    // 工厂方法
    public static KeyboardEvent pressed(String keyId) {
        return new KeyboardEvent(keyId, EVENT_TYPE_PRESSED);
    }

    public static KeyboardEvent released(String keyId) {
        return new KeyboardEvent(keyId, EVENT_TYPE_RELEASED);
    }

    public static KeyboardEvent held(String keyId) {
        return new KeyboardEvent(keyId, EVENT_TYPE_HELD);
    }
}
