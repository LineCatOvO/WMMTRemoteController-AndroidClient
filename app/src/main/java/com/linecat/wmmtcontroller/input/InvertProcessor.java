package com.linecat.wmmtcontroller.input;

/**
 * 静态处理器模型 - 方向反转处理器
 * 负责方向反转
 */
public class InvertProcessor {
    /**
     * 反转输入值
     * @param value 输入值
     * @param invert 是否反转
     * @return 处理后的值
     */
    public static float invert(float value, boolean invert) {
        return invert ? -value : value;
    }
}
