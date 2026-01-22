package com.linecat.wmmtcontroller.service;

/**
 * 输出派发器接口
 * 负责将JS层的输出命令派发到Android系统
 * 是Java输出层的核心组件，JS层只能通过此接口与Android系统交互
 */
public interface OutputDispatcher {
    
    /**
     * 发送按键事件
     * @param keyCode 按键代码
     * @param pressed 是否按下
     */
    void sendKey(int keyCode, boolean pressed);
    
    /**
     * 发送轴事件
     * @param axisId 轴ID
     * @param value 轴值
     */
    void sendAxis(int axisId, float value);
    
    /**
     * 发送宏事件
     * @param macroId 宏ID
     */
    void sendMacro(String macroId);
}