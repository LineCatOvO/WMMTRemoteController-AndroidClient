package com.linecat.wmmtcontroller.floatwindow;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;

/**
 * 悬浮球控制器
 * 负责悬浮球的创建、管理和命令处理
 */
public class OverlayController {
    private static final String TAG = "OverlayController";
    private final Context context;
    private final FloatWindowManager floatWindowManager;
    private boolean isOverlayVisible = false;

    public OverlayController(Context context) {
        this.context = context;
        this.floatWindowManager = FloatWindowManager.getInstance(context);
    }

    /**
     * 显示悬浮球
     */
    public void showOverlay() {
        if (!isOverlayVisible) {
            floatWindowManager.showFloatWindow();
            isOverlayVisible = true;
            Log.d(TAG, "Overlay shown");
        }
    }

    /**
     * 隐藏悬浮球
     */
    public void hideOverlay() {
        if (isOverlayVisible) {
            floatWindowManager.hideFloatWindow();
            isOverlayVisible = false;
            Log.d(TAG, "Overlay hidden");
        }
    }

    /**
     * 更新悬浮球状态文本
     */
    public void updateStatus(String status) {
        floatWindowManager.updateStatusText(status);
    }

    /**
     * 显示连接错误提示
     */
    public void showConnectionError() {
        floatWindowManager.showConnectionError();
    }

    /**
     * 打开配置界面
     */
    public void openConfigActivity() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 检查悬浮球是否可见
     */
    public boolean isOverlayVisible() {
        return isOverlayVisible;
    }

    /**
     * 设置当前布局
     */
    public void setCurrentLayout(LayoutSnapshot layout) {
        floatWindowManager.setCurrentLayout(layout);
    }
    
    /**
     * 设置输入控制器到布局渲染器
     */
    public void setInputController(com.linecat.wmmtcontroller.input.InteractionCapture inputController) {
        floatWindowManager.setInputController(inputController);
    }

    /**
     * 销毁悬浮球控制器
     */
    public void destroy() {
        hideOverlay();
    }
}