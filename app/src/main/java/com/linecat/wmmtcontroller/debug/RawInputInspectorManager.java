package com.linecat.wmmtcontroller.debug;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * 原始输入检查管理器
 * 负责管理RawInputInspectorView的生命周期和数据更新
 */
public class RawInputInspectorManager {
    private static final String TAG = "RawInputInspectorManager";
    
    // 单例实例
    private static RawInputInspectorManager sInstance;
    
    // 上下文
    private Context mContext;
    // 原始输入检查View
    private RawInputInspectorView mInspectorView;
    // 窗口管理器
    private WindowManager mWindowManager;
    // 窗口参数
    private WindowManager.LayoutParams mWindowParams;
    
    // 显示状态
    private boolean isVisible = true;
    
    /**
     * 获取单例实例
     */
    public static synchronized RawInputInspectorManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RawInputInspectorManager(context.getApplicationContext());
        }
        return sInstance;
    }
    
    /**
     * 构造方法
     */
    private RawInputInspectorManager(Context context) {
        mContext = context;
        initWindowManager();
        initInspectorView();
        // 默认显示原始输入检查View
        show();
    }
    
    /**
     * 初始化窗口管理器
     */
    private void initWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        
        // 创建窗口参数
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mWindowParams.format = android.graphics.PixelFormat.TRANSLUCENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowParams.x = 0;
        mWindowParams.y = 0;
    }
    
    /**
     * 初始化检查View
     */
    private void initInspectorView() {
        mInspectorView = new RawInputInspectorView(mContext);
        mInspectorView.setOnClickListener(v -> {
            // 点击切换陀螺仪轴
            mInspectorView.switchGyroAxis();
        });
        
        // 长按调整刷新频率
        mInspectorView.setOnLongClickListener(v -> {
            // 长按增加刷新频率
            mInspectorView.increaseRefreshRate();
            return true;
        });
    }
    
    /**
     * 显示原始输入检查View
     */
    public void show() {
        if (!isVisible) {
            try {
                mWindowManager.addView(mInspectorView, mWindowParams);
                isVisible = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 隐藏原始输入检查View
     */
    public void hide() {
        if (isVisible) {
            try {
                mWindowManager.removeView(mInspectorView);
                isVisible = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 切换显示状态
     */
    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * 处理触摸事件
     */
    public void onTouchEvent(MotionEvent event) {
        if (mInspectorView != null) {
            float x = event.getX();
            float y = event.getY();
            mInspectorView.updateTouchData(x, y);
        }
    }
    
    /**
     * 处理触摸结束事件
     */
    public void onTouchEnd() {
        if (mInspectorView != null) {
            mInspectorView.clearTouchData();
        }
    }
    
    /**
     * 更新触摸数据
     */
    public void updateTouchData(float x, float y) {
        if (mInspectorView != null) {
            mInspectorView.updateTouchData(x, y);
        }
    }
    
    /**
     * 更新陀螺仪数据
     */
    public void updateGyroData(float x, float y, float z) {
        if (mInspectorView != null) {
            mInspectorView.updateGyroData(x, y, z);
        }
    }
    
    /**
     * 设置刷新频率
     */
    public void setRefreshRate(int rate) {
        if (mInspectorView != null) {
            mInspectorView.setRefreshRate(rate);
        }
    }
    
    /**
     * 获取显示状态
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 销毁
     */
    public void destroy() {
        hide();
        mInspectorView = null;
        mWindowManager = null;
        sInstance = null;
    }
}
