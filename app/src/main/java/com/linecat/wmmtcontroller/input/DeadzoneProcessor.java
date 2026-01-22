package com.linecat.wmmtcontroller.input;

/**
 * 静态处理器模型 - Deadzone 处理器
 * 负责处理传感器噪声过滤
 */
public class DeadzoneProcessor {
    /**
     * 应用死区过滤
     * @param value 输入值
     * @param deadzone 死区大小（0.0-1.0）
     * @return 处理后的值
     */
    public static float process(float value, float deadzone) {
        if (Math.abs(value) < deadzone) {
            return 0f;
        } else {
            // 对超出死区的值进行归一化
            return (value - Math.signum(value) * deadzone) / (1f - deadzone);
        }
    }
}
