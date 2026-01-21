package com.linecat.wmmtcontroller.input;

import com.linecat.wmmtcontroller.annotation.Experimental;

/**
 * 主机服务接口
 * 包含日志、调试、任何可能产生副作用或未来会变的能力
 */
@Experimental(version = "1.0.0")
public interface HostServices {
    /**
     * 记录日志信息
     * @param message 日志消息
     */
    void log(String message);
    
    /**
     * 记录调试信息
     * @param message 调试消息
     */
    void debug(String message);
    
    /**
     * 记录错误信息
     * @param message 错误消息
     * @param error 错误对象
     */
    void error(String message, Throwable error);
    
    /**
     * 设置鼠标位置
     * @param x X坐标
     * @param y Y坐标
     */
    void setMousePosition(float x, float y);
    
    /**
     * 设置鼠标按键状态
     * @param button 鼠标按键名称（left, right, middle）
     * @param pressed 是否按下
     */
    void setMouseButton(String button, boolean pressed);
    
    /**
     * 获取陀螺仪数据
     * @return 陀螺仪数据对象
     */
    ScriptContext.GyroData getGyro();
    
    /**
     * 获取触摸数据
     * @return 触摸数据对象
     */
    ScriptContext.TouchData getTouch();
    
    /**
     * 获取当前配置文件的元信息
     * @param key 元信息键
     * @return 元信息值
     */
    Object getProfileMetadata(String key);
    
    /**
     * 请求权限
     * @param permission 权限名称
     * @return 是否获得权限
     */
    boolean requestPermission(String permission);
}