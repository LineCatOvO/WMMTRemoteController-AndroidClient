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

import java.util.List;

/**
 * 布局渲染器
 * 负责将布局快照渲染到屏幕上，并处理触摸事件
 */
public class LayoutRenderer extends View {
    private static final String TAG = "LayoutRenderer";

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
            return super.onTouchEvent(event);
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
                    Log.d(TAG, "Touched region: " + currentTouchRegion.getId());
                    // 更新输入状态
                    updateInputState(normalizedX, normalizedY, true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentTouchRegion != null) {
                    // 更新输入状态
                    updateInputState(normalizedX, normalizedY, true);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentTouchRegion != null) {
                    // 重置输入状态
                    updateInputState(normalizedX, normalizedY, false);
                    currentTouchRegion = null;
                }
                break;
        }

        return true;
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
        if (currentTouchRegion == null) {
            return;
        }

        // 根据区域类型更新不同的输入状态
        switch (currentTouchRegion.getType()) {
            case BUTTON:
                // 更新按钮状态
                // 使用RawInput的gamepad数据来存储按钮状态
                rawInput.getGamepad().setButton(currentTouchRegion.getId(), isPressed);
                break;

            case AXIS:
                // 计算轴的归一化值（0.0-1.0）
                float axisValue = calculateAxisValue(currentTouchRegion, x, y);
                // 使用RawInput的gamepad数据来存储轴值
                rawInput.getGamepad().setAxis(currentTouchRegion.getId(), axisValue);
                break;

            case GYROSCOPE:
                // 陀螺仪区域通常是显示用，不处理触摸
                break;

            case GESTURE:
                // 处理手势
                // 使用RawInput的触摸数据来存储手势信息
                rawInput.setTouchPressed(isPressed);
                rawInput.setTouchX(x);
                rawInput.setTouchY(y);
                break;
        }

        // 触发输入状态变化事件
        // TODO: 添加事件监听机制
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
     * 获取当前输入状态
     */
    public RawInput getRawInput() {
        return rawInput;
    }
}