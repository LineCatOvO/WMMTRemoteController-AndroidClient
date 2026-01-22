package com.linecat.wmmtcontroller.input;

/**
 * 静态处理器模型 - 范围映射处理器
 * 负责将输入值映射到指定的输出范围
 */
public class RangeMapper {
    /**
     * 将输入值从输入范围映射到输出范围
     * @param value 输入值
     * @param inputMin 输入范围最小值
     * @param inputMax 输入范围最大值
     * @param outputMin 输出范围最小值
     * @param outputMax 输出范围最大值
     * @return 映射后的值
     */
    public static float map(float value, float inputMin, float inputMax, float outputMin, float outputMax) {
        // 先将输入值归一化到 0.0-1.0 范围
        float normalized = (value - inputMin) / (inputMax - inputMin);
        // 然后映射到输出范围
        return normalized * (outputMax - outputMin) + outputMin;
    }

    /**
     * 将输入值裁剪到指定范围
     * @param value 输入值
     * @param min 最小值
     * @param max 最大值
     * @return 裁剪后的值
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
