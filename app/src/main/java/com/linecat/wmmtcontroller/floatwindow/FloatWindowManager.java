package com.linecat.wmmtcontroller.floatwindow;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.linecat.wmmtcontroller.R;

/**
 * 浮窗管理器
 * 负责浮窗的创建、显示、隐藏和更新
 */
public class FloatWindowManager {
    private static final String TAG = "FloatWindowManager";
    
    // 浮窗视图
    private View floatView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    
    // 上下文
    private Context context;
    
    // 浮窗是否显示
    private boolean isShowing = false;
    
    // 单例实例
    private static FloatWindowManager instance;
    
    /**
     * 私有构造方法
     */
    private FloatWindowManager(Context context) {
        this.context = context.getApplicationContext();
        initFloatWindow();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized FloatWindowManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloatWindowManager(context);
        }
        return instance;
    }
    
    // 弹出菜单显示状态
    private boolean isPopupMenuShowing = false;
    
    // 组件引用
    private View circleEntryView;
    private View popupMenuView;
    
    /**
     * 初始化浮窗
     */
    private void initFloatWindow() {
        // 获取WindowManager服务
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        // 创建浮窗参数
        windowParams = new WindowManager.LayoutParams();
        
        // 设置浮窗类型
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        // 设置浮窗参数
        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        
        // 设置浮窗位置和大小
        windowParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowParams.x = 100;
        windowParams.y = 100;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        
        // 加载浮窗布局
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        floatView = inflater.inflate(R.layout.float_window, null);
        
        // 获取组件引用
        circleEntryView = floatView.findViewById(R.id.ll_circle_entry);
        popupMenuView = floatView.findViewById(R.id.ll_popup_menu);
        
        // 设置圆形入口的点击事件
        circleEntryView.setOnClickListener(v -> togglePopupMenu());
        
        // 设置菜单项点击事件
        setupMenuItemListeners();
        
        // 设置浮窗触摸事件，实现拖拽功能
        circleEntryView.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY;
            private int paramX, paramY;
            private boolean isDragging = false;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = windowParams.x;
                        paramY = windowParams.y;
                        isDragging = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        // 如果移动距离超过阈值，视为拖拽
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isDragging = true;
                            windowParams.x = paramX + dx;
                            windowParams.y = paramY + dy;
                            // 更新浮窗位置
                            windowManager.updateViewLayout(floatView, windowParams);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // 如果不是拖拽，触发点击事件
                        if (!isDragging) {
                            v.performClick();
                        }
                        break;
                }
                // 如果是拖拽，返回true消费事件，否则返回false让点击事件处理
                return isDragging;
            }
        });
        
        // 设置浮窗其他区域的点击事件，用于隐藏弹出菜单
        floatView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 如果点击的是浮窗区域但不是圆形入口和弹出菜单，隐藏菜单
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (isPopupMenuShowing) {
                        hidePopupMenu();
                    }
                }
                return false;
            }
        });
    }
    
    /**
     * 设置菜单项点击事件
     */
    private void setupMenuItemListeners() {
        // 开始连接按钮点击事件
        floatView.findViewById(R.id.btn_start_connect).setOnClickListener(v -> {
            Log.d(TAG, "Start connect button clicked");
            // 发送开始连接广播
            Intent intent = new Intent("com.linecat.wmmtcontroller.ACTION_START_CONNECT");
            context.sendBroadcast(intent);
            hidePopupMenu();
        });
        
        // 断开连接按钮点击事件
        floatView.findViewById(R.id.btn_stop_connect).setOnClickListener(v -> {
            Log.d(TAG, "Stop connect button clicked");
            // 发送断开连接广播
            Intent intent = new Intent("com.linecat.wmmtcontroller.ACTION_STOP_CONNECT");
            context.sendBroadcast(intent);
            hidePopupMenu();
        });
        
        // 设置按钮点击事件
        floatView.findViewById(R.id.btn_menu_settings).setOnClickListener(v -> {
            Log.d(TAG, "Settings button clicked");
            hidePopupMenu();
        });
    }
    
    /**
     * 切换弹出菜单的显示/隐藏状态
     */
    private void togglePopupMenu() {
        if (isPopupMenuShowing) {
            hidePopupMenu();
        } else {
            showPopupMenu();
        }
    }
    
    /**
     * 显示弹出菜单
     */
    private void showPopupMenu() {
        popupMenuView.setVisibility(View.VISIBLE);
        isPopupMenuShowing = true;
        // 更新窗口参数，允许获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Popup menu showed");
    }
    
    /**
     * 隐藏弹出菜单
     */
    private void hidePopupMenu() {
        popupMenuView.setVisibility(View.GONE);
        isPopupMenuShowing = false;
        // 恢复窗口参数，不获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Popup menu hidden");
    }
    
    /**
     * 显示浮窗
     */
    public void showFloatWindow() {
        if (!isShowing) {
            try {
                windowManager.addView(floatView, windowParams);
                isShowing = true;
                Log.d(TAG, "Float window showed");
            } catch (Exception e) {
                Log.e(TAG, "Failed to show float window: " + e.getMessage());
            }
        }
    }
    
    /**
     * 隐藏浮窗
     */
    public void hideFloatWindow() {
        if (isShowing) {
            try {
                windowManager.removeView(floatView);
                isShowing = false;
                Log.d(TAG, "Float window hidden");
            } catch (Exception e) {
                Log.e(TAG, "Failed to hide float window: " + e.getMessage());
            }
        }
    }
    
    /**
     * 更新浮窗状态文本
     */
    public void updateStatusText(String status) {
        // 新设计中状态文本不再直接显示在浮窗上
        // 可以根据需要添加状态指示逻辑
        Log.d(TAG, "Float window status updated: " + status);
    }
    
    /**
     * 浮窗是否正在显示
     */
    public boolean isFloatWindowShowing() {
        return isShowing;
    }
    
    /**
     * 销毁浮窗
     */
    public void destroyFloatWindow() {
        hideFloatWindow();
        instance = null;
    }
}