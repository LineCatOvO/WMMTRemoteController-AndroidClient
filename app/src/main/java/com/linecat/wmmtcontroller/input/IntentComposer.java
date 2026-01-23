package com.linecat.wmmtcontroller.input;

import android.util.Log;

import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 意图合成器
 * 负责将原始输入转换为抽象操作意图
 * 这是三层输入架构中的抽象操作层
 */
public class IntentComposer {
    private static final String TAG = "IntentComposer";

    /**
     * 合成操作意图
     * 将原始输入转换为抽象操作意图
     *
     * @param rawInput 原始输入数据
     * @return 抽象操作意图
     */
    public RawInput composeIntent(RawInput rawInput) {
        // 对输入进行处理和抽象化
        // 这里可以应用过滤、平滑、死区处理等算法
        RawInput processedInput = new RawInput(rawInput);

        // 应用各种处理逻辑
        applyDeadzone(processedInput);
        applySmoothing(processedInput);
        applyCurve(processedInput);

        Log.d(TAG, "Intent composed from raw input");

        return processedInput;
    }

    /**
     * 应用死区处理
     */
    private void applyDeadzone(RawInput input) {
        // 对陀螺仪数据应用死区
        if (Math.abs(input.getGyroPitch()) < 0.1f) {
            input.setGyroPitch(0f);
        }
        if (Math.abs(input.getGyroRoll()) < 0.1f) {
            input.setGyroRoll(0f);
        }
        if (Math.abs(input.getGyroYaw()) < 0.1f) {
            input.setGyroYaw(0f);
        }

        // 对游戏手柄轴应用死区
        for (String axisId : input.getGamepad().getAxes().keySet()) {
            Float value = input.getGamepad().getAxes().get(axisId);
            if (Math.abs(value) < 0.1f) {
                input.getGamepad().setAxis(axisId, 0f);
            }
        }
    }

    /**
     * 应用平滑处理
     */
    private void applySmoothing(RawInput input) {
        // 实现平滑算法，可以使用历史数据来平滑当前输入
        // 这里暂时留空，具体实现可根据需要添加
    }

    /**
     * 应用曲线处理
     */
    private void applyCurve(RawInput input) {
        // 应用灵敏度曲线或其他非线性变换
        // 这里暂时留空，具体实现可根据需要添加
    }
}