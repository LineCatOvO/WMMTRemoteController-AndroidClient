package com.linecat.wmmtcontroller.input;

import android.util.Log;

/**
 * 安全控制器
 * 负责安全清零、异常处理和系统稳定性保障
 */
public class SafetyController {
    private static final String TAG = "SafetyController";
    private final OutputController outputController;
    private boolean isSafetyState = false;

    public SafetyController(OutputController outputController) {
        this.outputController = outputController;
    }

    /**
     * 触发安全清零
     */
    public void triggerSafetyClear() {
        synchronized (this) {
            if (!isSafetyState) {
                Log.d(TAG, "Triggering safety clear");
                
                // 立即清零所有输出
                outputController.clearAllOutputs();
                
                isSafetyState = true;
            }
        }
    }

    /**
     * 退出安全状态
     */
    public void exitSafetyState() {
        synchronized (this) {
            if (isSafetyState) {
                Log.d(TAG, "Exiting safety state");
                isSafetyState = false;
            }
        }
    }

    /**
     * 检查是否处于安全状态
     */
    public boolean isInSafetyState() {
        synchronized (this) {
            return isSafetyState;
        }
    }

    /**
     * 处理异常情况
     */
    public void handleException(Exception e) {
        Log.e(TAG, "Exception handled, triggering safety clear: " + e.getMessage(), e);
        triggerSafetyClear();
    }

    /**
     * 验证系统状态是否安全
     */
    public boolean verifySafeState() {
        // 验证系统状态是否安全
        // 返回 true 表示安全，false 表示不安全
        return !isSafetyState && outputController.isOutputSafe();
    }

    /**
     * 销毁安全控制器
     */
    public void destroy() {
        triggerSafetyClear();
    }
}