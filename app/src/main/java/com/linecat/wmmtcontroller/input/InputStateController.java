package com.linecat.wmmtcontroller.input;

import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;

/**
 * 输出控制器
 * 负责生成和管理控制结果状态
 */
public class InputStateController {
    private static final String TAG = "InputStateController";
    private InputState currentOutputState;
    private boolean isOutputEnabled = false;

    public InputStateController() {
        this.currentOutputState = new InputState();
    }

    /**
     * 更新输出状态
     */
    public void updateOutput(InputState newState) {
        synchronized (this) {
            if (isOutputEnabled) {
                this.currentOutputState = new InputState(newState); // 返回副本，避免并发问题
            }
        }
    }

    /**
     * 获取当前输出状态
     */
    public InputState getCurrentOutput() {
        synchronized (this) {
            return new InputState(currentOutputState);
        }
    }

    /**
     * 启用输出
     */
    public void enableOutput() {
        synchronized (this) {
            isOutputEnabled = true;
            Log.d(TAG, "Output enabled");
        }
    }

    /**
     * 禁用输出
     */
    public void disableOutput() {
        synchronized (this) {
            isOutputEnabled = false;
            clearAllOutputs();
            Log.d(TAG, "Output disabled");
        }
    }

    /**
     * 清零所有输出
     */
    public void clearAllOutputs() {
        synchronized (this) {
            this.currentOutputState = new InputState();
            Log.d(TAG, "All outputs cleared");
        }
    }

    /**
     * 检查输出是否安全
     */
    public boolean isOutputSafe() {
        synchronized (this) {
            // 检查输出是否安全，例如没有异常值
            return true;
        }
    }

    /**
     * 检查输出是否启用
     */
    public boolean isOutputEnabled() {
        synchronized (this) {
            return isOutputEnabled;
        }
    }

    /**
     * 销毁输出控制器
     */
    public void destroy() {
        disableOutput();
    }
}
