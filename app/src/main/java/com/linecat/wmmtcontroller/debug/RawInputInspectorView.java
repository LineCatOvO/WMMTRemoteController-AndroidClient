package com.linecat.wmmtcontroller.debug;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import java.util.Map;
import java.util.HashMap;

/**
 * 原始输入检查View
 * Debug专用，用于实时显示触控输入内容和陀螺仪数据
 */
public class RawInputInspectorView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "RawInputInspectorView";
    
    // 表面持有者
    private SurfaceHolder mHolder;
    // 画布
    private Canvas mCanvas;
    // 绘制线程
    private Thread mDrawThread;
    // 线程运行标志
    private boolean isRunning = false;
    
    // 画笔
    private Paint mPaint;
    private Paint mTextPaint;
    
    // 数据缓冲
    private final Object dataLock = new Object();
    private boolean dataUpdated = false;
    
    // 触摸点数据
    private Map<Integer, TouchPoint> touchPoints = new HashMap<>();
    private boolean hasTouch = false;
    
    // 陀螺仪数据
    private float gyroX = 0;
    private float gyroY = 0;
    private float gyroZ = 0;
    private int selectedAxis = 0; // 0: X, 1: Y, 2: Z
    
    // 陀螺仪波形数据
    private static final int WAVEFORM_LENGTH = 100;
    private float[] waveformData = new float[WAVEFORM_LENGTH];
    private int waveformIndex = 0;
    
    // 动画相关
    private ValueAnimator gyroIconAnimator;
    private float currentRotation = 0;
    private float targetRotation = 0;
    
    // 显示配置
    private int refreshRate = 50; // 默认50Hz
    private boolean showCoordinateSystem = true;
    private boolean showTouchPoints = true;
    private boolean showGyroData = true;
    private boolean showWaveform = true;
    private boolean showGyroIcon = true;
    
    // 触摸点类
    private static class TouchPoint {
        public float x;
        public float y;
        public long timestamp;
        
        public TouchPoint(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }
    }
    
    // 构造方法
    public RawInputInspectorView(Context context) {
        super(context);
        init();
    }
    
    public RawInputInspectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public RawInputInspectorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    /**
     * 初始化
     */
    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        
        // 设置半透明背景
        setZOrderOnTop(true);
        mHolder.setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        
        // 初始化画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(20);
        mTextPaint.setColor(Color.WHITE);
        
        // 初始化动画
        initAnimator();
        
        // 初始化绘制线程
        mDrawThread = new Thread(this);
    }
    
    /**
     * 开始绘制
     */
    public void startDrawing() {
        isRunning = true;
        mDrawThread.start();
    }
    
    /**
     * 停止绘制
     */
    public void stopDrawing() {
        isRunning = false;
        try {
            mDrawThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 释放资源
     */
    public void releaseResources() {
        stopDrawing();
        
        // 停止动画
        if (gyroIconAnimator != null && gyroIconAnimator.isRunning()) {
            gyroIconAnimator.cancel();
            gyroIconAnimator = null;
        }
        
        // 清理触摸点数据
        synchronized (dataLock) {
            touchPoints.clear();
            hasTouch = false;
        }
        
        // 清理波形数据
        waveformData = null;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseResources();
    }
    
    /**
     * 初始化动画
     */
    private void initAnimator() {
        gyroIconAnimator = ValueAnimator.ofFloat(0, 1);
        gyroIconAnimator.setDuration(100); // 100ms动画，与50Hz刷新率匹配
        gyroIconAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gyroIconAnimator.setRepeatMode(ValueAnimator.RESTART);
        gyroIconAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            // 平滑过渡到目标旋转角度
            currentRotation = currentRotation + (targetRotation - currentRotation) * fraction;
        });
        // 启动动画
        gyroIconAnimator.start();
    }
    
    /**
     * 更新目标旋转角度
     */
    private void updateTargetRotation() {
        float value = getSelectedAxisValue();
        targetRotation = value * 30; // 缩放因子
    }
    
    /**
     * 更新触摸点数据
     */
    public void updateTouchData(float x, float y) {
        // 默认使用ID 0，后续优化支持多点触控
        updateTouchData(0, x, y);
    }
    
    /**
     * 更新触摸点数据（支持多点触控）
     */
    public void updateTouchData(int id, float x, float y) {
        synchronized (dataLock) {
            touchPoints.put(id, new TouchPoint(x, y, System.currentTimeMillis()));
            hasTouch = true;
            dataUpdated = true;
        }
    }
    
    /**
     * 清除触摸点数据
     */
    public void clearTouchData() {
        synchronized (dataLock) {
            touchPoints.clear();
            hasTouch = false;
            dataUpdated = true;
        }
    }
    
    /**
     * 清除指定ID的触摸点数据
     */
    public void clearTouchData(int id) {
        synchronized (dataLock) {
            touchPoints.remove(id);
            hasTouch = !touchPoints.isEmpty();
            dataUpdated = true;
        }
    }
    
    /**
     * 更新陀螺仪数据
     */
    public void updateGyroData(float x, float y, float z) {
        synchronized (dataLock) {
            gyroX = x;
            gyroY = y;
            gyroZ = z;
            
            // 更新波形数据
            updateWaveformData();
            
            // 更新目标旋转角度
            updateTargetRotation();
            
            dataUpdated = true;
        }
    }
    
    /**
     * 更新波形数据
     */
    private void updateWaveformData() {
        // 获取当前选中轴的数据
        float value = getSelectedAxisValue();
        
        // 更新波形数据数组
        waveformData[waveformIndex] = value;
        waveformIndex = (waveformIndex + 1) % WAVEFORM_LENGTH;
    }
    
    /**
     * 获取当前选中轴的值
     */
    private float getSelectedAxisValue() {
        switch (selectedAxis) {
            case 0: return gyroX;
            case 1: return gyroY;
            case 2: return gyroZ;
            default: return 0;
        }
    }
    
    /**
     * 获取当前选中轴的名称
     */
    private String getSelectedAxisName() {
        switch (selectedAxis) {
            case 0: return "X";
            case 1: return "Y";
            case 2: return "Z";
            default: return "Unknown";
        }
    }
    
    /**
     * 切换选中的陀螺仪轴
     */
    public void switchGyroAxis() {
        selectedAxis = (selectedAxis + 1) % 3;
    }
    
    /**
     * 绘制陀螺仪图标
     */
    private void drawGyroIcon(Canvas canvas, int width, int height) {
        // 计算图标位置
        int iconX = width - 100;
        int iconY = 50;
        int iconSize = 60;
        
        // 绘制图标背景
        mPaint.setColor(Color.argb(64, 0, 255, 255));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(iconX, iconY, iconSize / 2, mPaint);
        
        // 绘制图标边框
        mPaint.setColor(Color.CYAN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        canvas.drawCircle(iconX, iconY, iconSize / 2, mPaint);
        
        // 绘制图标坐标轴
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(2);
        
        // 根据选中轴和当前值绘制旋转
        float rotation = 0;
        switch (selectedAxis) {
            case 0: // X轴
                rotation = gyroX * 30; // 缩放因子
                break;
            case 1: // Y轴
                rotation = gyroY * 30;
                break;
            case 2: // Z轴
                rotation = gyroZ * 30;
                break;
        }
        
        // 绘制旋转后的坐标轴
        canvas.save();
        canvas.rotate(rotation, iconX, iconY);
        canvas.drawLine(iconX - iconSize / 4, iconY, iconX + iconSize / 4, iconY, mPaint);
        canvas.drawLine(iconX, iconY - iconSize / 4, iconX, iconY + iconSize / 4, mPaint);
        canvas.restore();
    }
    
    /**
     * 设置刷新频率
     */
    public void setRefreshRate(int rate) {
        if (rate > 0 && rate <= 120) { // 限制在1-120Hz之间
            this.refreshRate = rate;
        }
    }
    
    /**
     * 增加刷新频率
     */
    public void increaseRefreshRate() {
        setRefreshRate(refreshRate + 10);
    }
    
    /**
     * 减少刷新频率
     */
    public void decreaseRefreshRate() {
        setRefreshRate(refreshRate - 10);
    }
    
    /**
     * 处理屏幕旋转
     */
    public void handleScreenRotation(int width, int height) {
        // 屏幕旋转时不需要特殊处理，因为我们使用的是相对坐标
        // SurfaceView会自动调整大小，坐标系也会根据新的宽高重新计算
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 处理屏幕大小变化
        handleScreenRotation(w, h);
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startDrawing();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 表面变化时的处理
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopDrawing();
    }
    
    @Override
    public void run() {
        while (isRunning) {
            boolean shouldDraw = false;
            
            synchronized (dataLock) {
                shouldDraw = dataUpdated;
                if (shouldDraw) {
                    dataUpdated = false;
                }
            }
            
            if (shouldDraw || showCoordinateSystem) { // 坐标系需要始终绘制
                try {
                    // 锁定画布并绘制
                    mCanvas = mHolder.lockCanvas();
                    if (mCanvas != null) {
                        // 清除画布
                        mCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
                        
                        // 绘制背景（半透明）
                        mPaint.setColor(Color.argb(128, 0, 0, 0));
                        mCanvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
                        
                        // 绘制坐标系
                        if (showCoordinateSystem) {
                            drawCoordinateSystem(mCanvas);
                        }
                        
                        // 绘制触摸点
                        if (showTouchPoints && hasTouch) {
                            synchronized (dataLock) {
                                drawTouchPoints(mCanvas);
                            }
                        }
                        
                        // 绘制陀螺仪数据
                        if (showGyroData) {
                            synchronized (dataLock) {
                                drawGyroData(mCanvas);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (mCanvas != null) {
                        mHolder.unlockCanvasAndPost(mCanvas);
                    }
                }
            }
            
            // 控制刷新频率
            try {
                Thread.sleep(1000 / refreshRate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 绘制坐标系
     */
    private void drawCoordinateSystem(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        
        // 绘制坐标轴
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(1);
        
        // X轴
        canvas.drawLine(0, centerY, width, centerY, mPaint);
        // Y轴
        canvas.drawLine(centerX, 0, centerX, height, mPaint);
        
        // 绘制原点
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(4);
        canvas.drawCircle(centerX, centerY, 5, mPaint);
        
        // 绘制刻度
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(2);
        for (int i = -10; i <= 10; i++) {
            if (i == 0) continue; // 跳过原点
            
            int x = centerX + i * 50;
            int y = centerY + i * 50;
            
            if (x >= 0 && x < width) {
                canvas.drawLine(x, centerY - 5, x, centerY + 5, mPaint);
                // 绘制刻度值
                String value = String.valueOf(i * 50);
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setTextSize(12);
                canvas.drawText(value, x - 10, centerY + 20, mTextPaint);
            }
            
            if (y >= 0 && y < height) {
                canvas.drawLine(centerX - 5, y, centerX + 5, y, mPaint);
                // 绘制刻度值
                String value = String.valueOf(-i * 50); // Y轴向上为正
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setTextSize(12);
                canvas.drawText(value, centerX + 10, y + 4, mTextPaint);
            }
        }
        
        // 绘制轴标签
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(16);
        mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText("X", width - 20, centerY - 10, mTextPaint);
        canvas.drawText("Y", centerX + 10, 20, mTextPaint);
    }
    
    /**
     * 绘制触摸点
     */
    private void drawTouchPoints(Canvas canvas) {
        if (touchPoints.isEmpty()) {
            return;
        }
        
        // 绘制触摸点
        int touchCount = 0;
        for (Map.Entry<Integer, TouchPoint> entry : touchPoints.entrySet()) {
            int id = entry.getKey();
            TouchPoint point = entry.getValue();
            
            // 根据ID设置不同颜色
            int color = getTouchPointColor(id);
            mPaint.setColor(color);
            mPaint.setStrokeWidth(8);
            
            // 绘制触摸点
            canvas.drawCircle(point.x, point.y, 10, mPaint);
            
            // 绘制触摸点ID
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextSize(14);
            mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            canvas.drawText(String.valueOf(id), point.x + 15, point.y - 15, mTextPaint);
            
            // 绘制坐标值
            String coordText = String.format("(%.1f, %.1f)", point.x, point.y);
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextSize(12);
            mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT);
            canvas.drawText(coordText, point.x + 15, point.y + 5, mTextPaint);
            
            touchCount++;
        }
        
        // 绘制触摸点数量
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(16);
        mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText("Touch Points: " + touchPoints.size(), 20, 40, mTextPaint);
    }
    
    /**
     * 根据触摸点ID获取颜色
     */
    private int getTouchPointColor(int id) {
        int[] colors = {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.MAGENTA,
            Color.CYAN
        };
        return colors[id % colors.length];
    }
    
    /**
     * 绘制陀螺仪数据
     */
    private void drawGyroData(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // 绘制陀螺仪数据标题
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(16);
        mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText("Gyroscope Data", 20, height - 180, mTextPaint);
        
        // 绘制选中轴信息
        String axisText = "Selected Axis: " + getSelectedAxisName();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(14);
        mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText(axisText, 20, height - 150, mTextPaint);
        
        // 绘制选中轴的实时数值
        float selectedValue = getSelectedAxisValue();
        String valueText = String.format("Current Value: %.3f", selectedValue);
        mTextPaint.setColor(Color.CYAN);
        mTextPaint.setTextSize(16);
        mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText(valueText, 20, height - 120, mTextPaint);
        
        // 绘制所有三个轴的详细数值
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(12);
        mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText(String.format("X: %.3f", gyroX), 20, height - 90, mTextPaint);
        canvas.drawText(String.format("Y: %.3f", gyroY), 100, height - 90, mTextPaint);
        canvas.drawText(String.format("Z: %.3f", gyroZ), 180, height - 90, mTextPaint);
        
        // 绘制波形
        if (showWaveform) {
            drawWaveform(canvas, width, height);
        }
        
        // 绘制陀螺仪图标
        if (showGyroIcon) {
            drawGyroIcon(canvas, width, height);
        }
    }
    
    /**
     * 绘制波形
     */
    private void drawWaveform(Canvas canvas, int width, int height) {
        int waveformHeight = 100;
        int waveformTop = height - 280;
        int waveformBottom = waveformTop + waveformHeight;
        int waveformLeft = 20;
        int waveformRight = width - 20;
        
        // 绘制波形背景
        mPaint.setColor(Color.argb(64, 255, 255, 255));
        canvas.drawRect(waveformLeft, waveformTop, waveformRight, waveformBottom, mPaint);
        
        // 绘制中心线
        mPaint.setColor(Color.argb(128, 255, 255, 255));
        mPaint.setStrokeWidth(1);
        int centerY = waveformTop + waveformHeight / 2;
        canvas.drawLine(waveformLeft, centerY, waveformRight, centerY, mPaint);
        
        // 绘制波形
        mPaint.setColor(Color.CYAN);
        mPaint.setStrokeWidth(2);
        
        int stepX = (waveformRight - waveformLeft) / WAVEFORM_LENGTH;
        float scaleY = waveformHeight / 4.0f; // 假设最大值为±2
        
        for (int i = 0; i < WAVEFORM_LENGTH - 1; i++) {
            int index1 = (waveformIndex + i) % WAVEFORM_LENGTH;
            int index2 = (waveformIndex + i + 1) % WAVEFORM_LENGTH;
            
            float value1 = waveformData[index1];
            float value2 = waveformData[index2];
            
            int x1 = waveformLeft + i * stepX;
            int y1 = centerY - (int)(value1 * scaleY);
            int x2 = waveformLeft + (i + 1) * stepX;
            int y2 = centerY - (int)(value2 * scaleY);
            
            // 确保坐标在范围内
            y1 = Math.max(waveformTop, Math.min(waveformBottom, y1));
            y2 = Math.max(waveformTop, Math.min(waveformBottom, y2));
            
            canvas.drawLine(x1, y1, x2, y2, mPaint);
        }
    }
    

}
