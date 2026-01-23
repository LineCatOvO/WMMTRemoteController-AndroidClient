package com.linecat.wmmtcontroller.model;

/**
 * 输入元数据模型
 * 用于WebSocket消息的metadata字段
 */
public class InputMetadata {
    private String clientId;
    private long timestamp;
    private Long latency;

    public InputMetadata() {
        this.clientId = "android_client_" + System.currentTimeMillis();
        this.timestamp = System.currentTimeMillis();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getLatency() {
        return latency;
    }

    public void setLatency(Long latency) {
        this.latency = latency;
    }
}