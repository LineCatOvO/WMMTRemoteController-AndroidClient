package com.linecat.wmmtcontroller.service;

/**
 * 运行时事件常量定义
 * 用于LocalBroadcast通信，实现运行时状态的可观测性
 */
public class RuntimeEvents {

    // 广播动作常量
    public static final String ACTION_RUNTIME_STARTED = "com.linecat.wmmtcontroller.RUNTIME_STARTED";
    public static final String ACTION_PROFILE_LOADED = "com.linecat.wmmtcontroller.PROFILE_LOADED";
    public static final String ACTION_SCRIPT_ENGINE_READY = "com.linecat.wmmtcontroller.SCRIPT_ENGINE_READY";
    public static final String ACTION_WS_CONNECTED = "com.linecat.wmmtcontroller.WS_CONNECTED";
    public static final String ACTION_WS_DISCONNECTED = "com.linecat.wmmtcontroller.WS_DISCONNECTED";
    public static final String ACTION_WS_SENT_FRAME = "com.linecat.wmmtcontroller.WS_SENT_FRAME";
    public static final String ACTION_RUNTIME_ERROR = "com.linecat.wmmtcontroller.RUNTIME_ERROR";
    public static final String ACTION_PROFILE_ROLLBACK = "com.linecat.wmmtcontroller.PROFILE_ROLLBACK";
    public static final String ACTION_CONNECTION_INFO_UPDATED = "com.linecat.wmmtcontroller.CONNECTION_INFO_UPDATED";
    
    // 广播额外数据键
    public static final String EXTRA_FRAME_ID = "frameId";
    public static final String EXTRA_ERROR_TYPE = "errorType";
    public static final String EXTRA_PROFILE_ID = "profileId";
    public static final String EXTRA_PROFILE_NAME = "profileName";
    public static final String EXTRA_WS_MESSAGE = "wsMessage";
    
    // 错误类型常量
    public static final String ERROR_TYPE_COMPILE_ERROR = "COMPILE_ERROR";
    public static final String ERROR_TYPE_RUNTIME_ERROR = "RUNTIME_ERROR";
    public static final String ERROR_TYPE_TIMEOUT = "TIMEOUT";
    public static final String ERROR_TYPE_WEBSOCKET_ERROR = "WEBSOCKET_ERROR";
}
