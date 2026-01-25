package com.linecat.wmmtcontroller.service;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 事件通道消息类
 * 符合技术设计文档中的Event消息格式
 */
public class EventMessage {
    @SerializedName("type")
    private final String type = "event";

    @SerializedName("eventId")
    private long eventId;

    @SerializedName("baseStateId")
    private long baseStateId;

    @SerializedName("clientSendTs")
    private long clientSendTs;

    @SerializedName("delta")
    private EventDelta delta;

    @SerializedName("flags")
    private List<String> flags;

    // 构造函数
    public EventMessage(long eventId, long baseStateId, EventDelta delta) {
        this.eventId = eventId;
        this.baseStateId = baseStateId;
        this.clientSendTs = System.currentTimeMillis();
        this.delta = delta;
        this.flags = List.of();
    }

    // 带零输出标志的构造函数
    public EventMessage(long eventId, long baseStateId, EventDelta delta, boolean zeroOutput) {
        this(eventId, baseStateId, delta);
        if (zeroOutput) {
            this.flags = List.of("zero-output");
        }
    }

    // getter方法
    public String getType() {
        return type;
    }

    public long getEventId() {
        return eventId;
    }

    public long getBaseStateId() {
        return baseStateId;
    }

    public long getClientSendTs() {
        return clientSendTs;
    }

    public EventDelta getDelta() {
        return delta;
    }

    public List<String> getFlags() {
        return flags;
    }
}
