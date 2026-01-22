package com.linecat.wmmtcontroller.input;

/**
 * 静态处理器模型 - 曲线处理器
 * 负责应用不同类型的灵敏度曲线
 */
public class CurveProcessor {
    /**
     * 应用灵敏度曲线
     * @param value 输入值（-1.0 到 1.0）
     * @param curveType 曲线类型：linear, exponential, logarithmic, sine
     * @param curveParam 曲线参数（根据曲线类型不同而不同）
     * @return 处理后的值
     */
    public static float applyCurve(float value, String curveType, float curveParam) {
        switch (curveType) {
            case "exponential":
                return applyExponentialCurve(value, curveParam);
            case "logarithmic":
                return applyLogarithmicCurve(value, curveParam);
            case "sine":
                return applySineCurve(value, curveParam);
            case "linear":
            default:
                return value;
        }
    }

    /**
     * 应用指数曲线
     * @param value 输入值
     * @param gamma 伽马值（>1.0 增强灵敏度，<1.0 降低灵敏度）
     * @return 处理后的值
     */
    private static float applyExponentialCurve(float value, float gamma) {
        float absValue = Math.abs(value);
        float sign = Math.signum(value);
        return sign * (float) Math.pow(absValue, gamma);
    }

    /**
     * 应用对数曲线
     * @param value 输入值
     * @param factor 因子
     * @return 处理后的值
     */
    private static float applyLogarithmicCurve(float value, float factor) {
        float absValue = Math.abs(value);
        float sign = Math.signum(value);
        return sign * (float) Math.log10(absValue * factor + 1) / (float) Math.log10(factor + 1);
    }

    /**
     * 应用正弦曲线
     * @param value 输入值
     * @param factor 因子
     * @return 处理后的值
     */
    private static float applySineCurve(float value, float factor) {
        return (float) Math.sin(value * Math.PI / 2 * factor);
    }
}
