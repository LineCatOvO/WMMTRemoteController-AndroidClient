package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.input.Region;

import java.util.List;
import java.util.Map;

/**
 * 布局渲染器
 * 负责将布局快照渲染到屏幕上，并处理触摸事件
 */
public class LayoutRenderer extends View {
    private static final String TAG = "LayoutRenderer";
    
    // 用于控制日志打印频率的变量
    private static long lastTouchLogTime = 0;
    private static final long TOUCH_LOG_INTERVAL = 5000; // 5秒间隔
    private static int touchEventCount = 0;
    private static String lastTouchedRegion = "";

    // 布局快照
    private LayoutSnapshot currentLayout;

    // 画笔
    private Paint paint;
    private Paint textPaint;
    private Paint borderPaint;

    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    // 当前触摸的区域
    private Region currentTouchRegion;

    // 输入处理器
    private RawInput rawInput;
    
    // 输入控制器，用于同步输入状态
    private InteractionCapture inputController;

    // 布局是否启用
    private boolean isLayoutEnabled = false;

    public LayoutRenderer(Context context) {
        super(context);
        init();
    }

    /**
     * 初始化渲染器
     */
    private void init() {
        // 初始化画笔
        paint = new Paint();
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(24);
        textPaint.setColor(Color.WHITE);

        borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        borderPaint.setColor(Color.BLUE);

        // 初始化输入
        rawInput = new RawInput();

        // 设置为可触摸
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * 设置当前布局快照
     */
    public void setLayout(LayoutSnapshot layout) {
        this.currentLayout = layout;
        invalidate();
    }

    /**
     * 设置布局启用状态
     */
    public void setLayoutEnabled(boolean enabled) {
        this.isLayoutEnabled = enabled;
        invalidate();
    }

    /**
     * 获取当前布局启用状态
     */
    public boolean isLayoutEnabled() {
        return isLayoutEnabled;
    }

    /**
     * 归一化坐标转换为屏幕坐标
     */
    private float normalizeX(float x) {
        return x * screenWidth;
    }

    private float normalizeY(float y) {
        return y * screenHeight;
    }

    /**
     * 屏幕坐标转换为归一化坐标
     */
    private float screenToNormalX(float x) {
        return x / screenWidth;
    }

    private float screenToNormalY(float y) {
        return y / screenHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.screenWidth = w;
        this.screenHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 如果布局未启用或没有布局，不渲染
        if (!isLayoutEnabled || currentLayout == null) {
            return;
        }

        // 获取所有区域
        List<Region> regions = currentLayout.getRegions();
        if (regions == null || regions.isEmpty()) {
            return;
        }

        // 绘制每个区域
        for (Region region : regions) {
            drawRegion(canvas, region);
        }
    }

    /**
     * 绘制单个区域
     */
    private void drawRegion(Canvas canvas, Region region) {
        // 计算区域在屏幕上的位置
        float left = normalizeX(region.getLeft());
        float top = normalizeY(region.getTop());
        float right = normalizeX(region.getRight());
        float bottom = normalizeY(region.getBottom());

        RectF rect = new RectF(left, top, right, bottom);

        // 根据区域类型设置不同的填充颜色
        switch (region.getType()) {
            case BUTTON:
                paint.setColor(Color.argb(100, 0, 0, 255));
                break;
            case AXIS:
                paint.setColor(Color.argb(100, 0, 255, 0));
                break;
            case GYROSCOPE:
                paint.setColor(Color.argb(100, 255, 0, 0));
                break;
            case GESTURE:
                paint.setColor(Color.argb(100, 255, 255, 0));
                break;
            default:
                paint.setColor(Color.argb(100, 128, 128, 128));
                break;
        }

        // 绘制区域背景
        canvas.drawRect(rect, paint);

        // 绘制边框
        canvas.drawRect(rect, borderPaint);

        // 绘制区域ID
        String text = region.getId();
        float textX = left + 10;
        float textY = top + 30;
        canvas.drawText(text, textX, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 如果布局未启用，不处理触摸事件
        if (!isLayoutEnabled) {
            return false; // 不消费事件，允许穿透
        }

        // 如果没有布局或没有定义的区域，不处理触摸事件
        if (currentLayout == null || currentLayout.getRegions() == null || currentLayout.getRegions().isEmpty()) {
            return false; // 没有UI元素，允许触摸穿透
        }

        // 获取触摸坐标
        float x = event.getX();
        float y = event.getY();
        float normalizedX = screenToNormalX(x);
        float normalizedY = screenToNormalY(y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 查找触摸到的区域
                currentTouchRegion = findTouchedRegion(normalizedX, normalizedY);
                if (currentTouchRegion != null) {
                    // 更新统计信息
                    lastTouchedRegion = currentTouchRegion.getId();
                    touchEventCount++;
                    
                    // 按时间间隔打印日志
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTouchLogTime >= TOUCH_LOG_INTERVAL) {
                        Log.d(TAG, "Touch events summary - Last touched region: " + lastTouchedRegion + 
                              ", Total touch events in interval: " + touchEventCount);
                        // 重置计数器
                        touchEventCount = 0;
                        lastTouchLogTime = currentTime;
                    }
                    
                    // 更新输入状态
                    updateInputState(normalizedX, normalizedY, true);
                    return true; // 只有在触摸到区域时才消费事件
                } else {
                    // 触摸不在任何UI区域内，不消费事件，允许穿透
                    currentTouchRegion = null; // 确保没有当前触摸区域
                    return false;
                }

            case MotionEvent.ACTION_MOVE:
                if (currentTouchRegion != null) {
                    // 更新输入状态
                    updateInputState(normalizedX, normalizedY, true);
                    return true; // 只有在处理移动事件时才消费事件
                } else {
                    // 没有当前触摸区域，不消费事件
                    return false;
                }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentTouchRegion != null) {
                    // 重置输入状态
                    updateInputState(normalizedX, normalizedY, false);
                    currentTouchRegion = null;
                    return true; // 只有在处理区域释放时才消费事件
                } else {
                    // 没有当前触摸区域，不消费事件
                    return false;
                }
        }

        // 默认情况下不消费事件
        return false;
    }

    /**
     * 查找触摸到的区域
     */
    private Region findTouchedRegion(float x, float y) {
        if (currentLayout == null) {
            return null;
        }

        List<Region> regions = currentLayout.getRegions();
        if (regions == null || regions.isEmpty()) {
            return null;
        }

        // 遍历所有区域，查找包含触摸点的区域
        for (Region region : regions) {
            if (region.contains(x, y)) {
                return region;
            }
        }

        return null;
    }

    /**
     * 更新输入状态
     */
    private void updateInputState(float x, float y, boolean isPressed) {
        if (currentTouchRegion == null || inputController == null) {
            return;
        }

        // 根据区域类型直接发送事件到输入控制器
        switch (currentTouchRegion.getType()) {
            case BUTTON:
                // 直接发送按钮事件到输入控制器
                sendButtonEventToController(currentTouchRegion.getId(), isPressed);
                break;

            case AXIS:
                // 计算轴的归一化值（0.0-1.0）
                float axisValue = calculateAxisValue(currentTouchRegion, x, y);
                // 直接发送轴事件到输入控制器
                sendAxisEventToController(currentTouchRegion.getId(), axisValue);
                break;

            case GYROSCOPE:
                // 陀螺仪区域通常是显示用，不处理触摸
                break;

            case GESTURE:
                // 发送手势事件到输入控制器
                inputController.processTouchInput(x, y, isPressed);
                break;
        }
    }
    
    /**
     * 发送按钮事件到输入控制器
     */
    private void sendButtonEventToController(String buttonId, boolean isPressed) {
        if (inputController != null) {
            // 使用InputController的方法更新游戏手柄按钮状态
            synchronized (inputController) {
                inputController.getCurrentRawInput().getGamepad().setButton(buttonId, isPressed);
            }
            inputController.triggerProjection();
        }
    }
    
    /**
     * 发送轴事件到输入控制器
     */
    private void sendAxisEventToController(String axisId, float value) {
        if (inputController != null) {
            // 使用InputController的方法更新游戏手柄轴状态
            synchronized (inputController) {
                inputController.getCurrentRawInput().getGamepad().setAxis(axisId, value);
            }
            inputController.triggerProjection();
        }
    }

    /**
     * 计算轴的归一化值
     */
    private float calculateAxisValue(Region region, float x, float y) {
        // 计算触摸点在区域内的相对位置
        float relX = (x - region.getLeft()) / (region.getRight() - region.getLeft());
        float relY = (y - region.getTop()) / (region.getBottom() - region.getTop());

        // 对于模拟摇杆，通常取垂直方向的位置作为值
        // 这里简化处理，返回垂直方向的归一化值
        return 1.0f - relY; // 从上到下，值从1.0到0.0
    }


    
    /**
     * 设置输入控制器，用于同步输入状态
     */
    public void setInputController(InteractionCapture inputController) {
        this.inputController = inputController;
    }
    

}