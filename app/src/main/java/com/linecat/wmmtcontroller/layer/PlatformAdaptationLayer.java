package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Platform Adaptation Layer
 * <p>
 * 负责：overlay、生存、触摸、传感器、raw 事件
 * 不负责：坐标归一、状态机、语义
 */
public final class PlatformAdaptationLayer {
    private static final String TAG = "PlatformAdaptationLayer";
    private static final int MAX_QUEUE_SIZE = 4096;

    public enum OverlayMode {
        SYSTEM_OVERLAY,     // TYPE_APPLICATION_OVERLAY (production)
        ACTIVITY_PANEL      // TYPE_APPLICATION_PANEL (instrumentation tests)
    }

    private final Context context;
    private final RawEventSink sink;
    private final SensorManager sensorManager;
    private final Sensor gyroscopeSensor;
    private volatile OverlayView overlayView; // 延迟创建
    private final WindowManager windowManager;
    private final ConcurrentLinkedQueue<RawEvent> eventQueue;
    private final SensorEventListener sensorEventListener;
    private final OverlayMode overlayMode;
    private final android.os.IBinder hostWindowToken; // nullable

    private boolean isOverlayRunning = false;
    private int sensorDropCount = 0;

    // 构造函数
    public PlatformAdaptationLayer(Context context, RawEventSink sink) {
        this(context, sink, OverlayMode.SYSTEM_OVERLAY, null);
    }

    // Test-only ctor (used by androidTest)
    public PlatformAdaptationLayer(
            Context context,
            RawEventSink sink,
            OverlayMode overlayMode,
            android.os.IBinder hostWindowToken
    ) {
        this.context = context.getApplicationContext();
        this.sink = sink;
        this.overlayMode = overlayMode;
        this.hostWindowToken = hostWindowToken;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.overlayView = null; // 延迟到startOverlay时创建
        
        // 初始化传感器事件监听器
        this.sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                handleSensorEvent(event);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // 处理传感器精度变化
            }
        };
    }

    /**
     * 启动 overlay
     */
    public void startOverlay() {
        if (isOverlayRunning) {
            return;
        }

        // 能力检查与可运行失败处理
        if (overlayMode == OverlayMode.SYSTEM_OVERLAY) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                // 关键：不要崩溃，让测试/上层可以观测到失败原因
                sendRawWindowEvent(RawWindowEvent.Kind.ATTACH_FAILED, getCurrentMetrics());
                return;
            }
        } else {
            if (hostWindowToken == null) {
                sendRawWindowEvent(RawWindowEvent.Kind.ATTACH_FAILED, getCurrentMetrics());
                return;
            }
        }

        // 同步创建OverlayView，确保在主线程中创建，避免Handler创建异常
        if (overlayView == null) {
            overlayView = new OverlayView(context);
        }

        // 创建 overlay 窗口
        WindowManager.LayoutParams params = createOverlayParams();
        windowManager.addView(overlayView, params);
        
        // 注册传感器监听器（最高采样率）
        sensorManager.registerListener(
                sensorEventListener,
                gyroscopeSensor,
                SensorManager.SENSOR_DELAY_FASTEST
        );

        isOverlayRunning = true;
        
        // 发送窗口附加事件
        sendRawWindowEvent(RawWindowEvent.Kind.ATTACHED, getCurrentMetrics());
    }

    /**
     * 停止 overlay
     */
    public void stopOverlay() {
        if (!isOverlayRunning) {
            return;
        }

        // 移除 overlay 窗口
        windowManager.removeView(overlayView);
        
        // 注销传感器监听器
        sensorManager.unregisterListener(sensorEventListener);

        isOverlayRunning = false;
        
        // 发送窗口分离事件
        sendRawWindowEvent(RawWindowEvent.Kind.DETACHED, getCurrentMetrics());
        
        // 清空事件队列
        eventQueue.clear();
        sensorDropCount = 0;
    }

    /**
     * 创建 overlay 窗口参数
     */
    private WindowManager.LayoutParams createOverlayParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (overlayMode == OverlayMode.SYSTEM_OVERLAY) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            params.token = hostWindowToken; // 关键：绑定 Activity window token
        }

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.format = android.graphics.PixelFormat.TRANSLUCENT;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        return params;
    }

    /**
     * 获取当前显示 metrics
     */
    private RawWindowEvent.Metrics getCurrentMetrics() {
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int rotation = windowManager.getDefaultDisplay().getRotation();
        
        return new RawWindowEvent.Metrics(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.densityDpi,
                rotation
        );
    }

    /**
     * 处理触摸事件
     */
    private void handleTouchEvent(MotionEvent event) {
        long timeNanos = event.getEventTime() * 1000000; // 转换为纳秒
        RawPointerEvent.Action action;
        int changedId = -1;
        List<RawPointerEvent.Pointer> pointers = new ArrayList<>();

        // 转换 MotionEvent.Action 为 RawPointerEvent.Action
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                action = RawPointerEvent.Action.DOWN;
                changedId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                action = RawPointerEvent.Action.MOVE;
                break;
            case MotionEvent.ACTION_UP:
                action = RawPointerEvent.Action.UP;
                changedId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_CANCEL:
                action = RawPointerEvent.Action.CANCEL;
                break;
            default:
                return;
        }

        // 收集所有指针信息
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            float x = event.getX(i);
            float y = event.getY(i);
            pointers.add(new RawPointerEvent.Pointer(pointerId, x, y));
        }

        // 创建 RawPointerEvent
        RawPointerEvent rawPointerEvent = new RawPointerEvent(
                timeNanos,
                action,
                changedId,
                pointers,
                getCurrentMetrics()
        );

        // 发送 RawPointerEvent（关键事件，永不丢）
        sendRawEvent(rawPointerEvent);
    }

    /**
     * 处理传感器事件
     */
    private void handleSensorEvent(SensorEvent event) {
        long timeNanos = System.nanoTime();
        
        // 转换传感器精度
        RawSensorEvent.Accuracy accuracy = RawSensorEvent.Accuracy.fromAndroidAccuracy(event.accuracy);
        
        // 创建 RawSensorEvent
        RawSensorEvent rawSensorEvent = new RawSensorEvent(
                timeNanos,
                RawSensorEvent.SensorType.GYROSCOPE,
                event.values.clone(), // 复制数组，避免并发问题
                accuracy
        );

        // 发送 RawSensorEvent（可丢）
        boolean sent = sendRawEvent(rawSensorEvent);
        if (!sent) {
            sensorDropCount++;
            // 每 100 个丢包发送一次丢包事件
            if (sensorDropCount % 100 == 0) {
                sendRawDropEvent(RawDropEvent.Kind.SENSOR, sensorDropCount);
                sensorDropCount = 0;
            }
        }
    }

    /**
     * 发送 Raw 事件
     */
    private boolean sendRawEvent(RawEvent event) {
        // 检查队列大小，实现背压
        if (eventQueue.size() >= MAX_QUEUE_SIZE) {
            // 如果是关键事件（Pointer DOWN/UP/CANCEL），则移除最旧的非关键事件
            if (event instanceof RawPointerEvent) {
                RawPointerEvent pointerEvent = (RawPointerEvent) event;
                if (pointerEvent.action == RawPointerEvent.Action.DOWN ||
                    pointerEvent.action == RawPointerEvent.Action.UP ||
                    pointerEvent.action == RawPointerEvent.Action.CANCEL) {
                    // 移除最旧的事件，直到队列有空间
                    while (eventQueue.size() >= MAX_QUEUE_SIZE) {
                        RawEvent oldest = eventQueue.poll();
                        // 如果移除的是关键事件，则重新添加
                        if (oldest instanceof RawPointerEvent) {
                            RawPointerEvent oldestPointer = (RawPointerEvent) oldest;
                            if (oldestPointer.action == RawPointerEvent.Action.DOWN ||
                                oldestPointer.action == RawPointerEvent.Action.UP ||
                                oldestPointer.action == RawPointerEvent.Action.CANCEL) {
                                eventQueue.add(oldest);
                            }
                        }
                    }
                } else {
                    // 非关键事件，直接丢弃
                    return false;
                }
            } else {
                // 非指针事件，直接丢弃
                return false;
            }
        }
        
        // 添加到队列并处理
        eventQueue.add(event);
        processEventQueue();
        return true;
    }

    /**
     * 处理事件队列
     */
    private void processEventQueue() {
        RawEvent event;
        while ((event = eventQueue.poll()) != null) {
            if (event instanceof RawWindowEvent) {
                sink.onRawWindowEvent((RawWindowEvent) event);
            } else if (event instanceof RawPointerEvent) {
                sink.onRawPointerEvent((RawPointerEvent) event);
            } else if (event instanceof RawSensorEvent) {
                sink.onRawSensorEvent((RawSensorEvent) event);
            } else if (event instanceof RawDropEvent) {
                sink.onRawDropEvent((RawDropEvent) event);
            }
        }
    }

    /**
     * 发送 RawWindowEvent
     */
    private void sendRawWindowEvent(RawWindowEvent.Kind kind, RawWindowEvent.Metrics metrics) {
        RawWindowEvent event = new RawWindowEvent(System.nanoTime(), kind, metrics);
        sendRawEvent(event);
    }

    /**
     * 发送 RawDropEvent
     */
    private void sendRawDropEvent(RawDropEvent.Kind kind, int droppedCount) {
        RawDropEvent event = new RawDropEvent(System.nanoTime(), kind, droppedCount);
        sendRawEvent(event);
    }

    // 事件类型标记接口
    public interface RawEvent {}

    // Raw 事件接收器接口
    public interface RawEventSink {
        void onRawWindowEvent(RawWindowEvent e);
        void onRawPointerEvent(RawPointerEvent e);
        void onRawSensorEvent(RawSensorEvent e);
        void onRawDropEvent(RawDropEvent e);
    }

    // Raw 窗口事件
    public static final class RawWindowEvent implements RawEvent {
        public final long timeNanos;
        public final Kind kind;
        public final Metrics metrics;

        public enum Kind {
            ATTACHED,
            DETACHED,
            METRICS_CHANGED,
            ATTACH_FAILED
        }

        public static final class Metrics {
            public final int widthPx;
            public final int heightPx;
            public final int densityDpi;
            public final int rotation;

            public Metrics(int widthPx, int heightPx, int densityDpi, int rotation) {
                this.widthPx = widthPx;
                this.heightPx = heightPx;
                this.densityDpi = densityDpi;
                this.rotation = rotation;
            }
        }

        public RawWindowEvent(long timeNanos, Kind kind, Metrics metrics) {
            this.timeNanos = timeNanos;
            this.kind = kind;
            this.metrics = metrics;
        }
    }

    // Raw 指针事件
    public static final class RawPointerEvent implements RawEvent {
        public final long timeNanos;
        public final Action action;
        public final int changedId;
        public final List<Pointer> pointers;
        public final RawWindowEvent.Metrics display;

        public enum Action {
            DOWN,
            MOVE,
            UP,
            CANCEL
        }

        public static final class Pointer {
            public final int id;
            public final float x;
            public final float y;

            public Pointer(int id, float x, float y) {
                this.id = id;
                this.x = x;
                this.y = y;
            }
        }

        public RawPointerEvent(long timeNanos, Action action, int changedId, List<Pointer> pointers, RawWindowEvent.Metrics display) {
            this.timeNanos = timeNanos;
            this.action = action;
            this.changedId = changedId;
            this.pointers = pointers;
            this.display = display;
        }
    }

    // Raw 传感器事件
    public static final class RawSensorEvent implements RawEvent {
        public final long timeNanos;
        public final SensorType sensorType;
        public final float[] values;
        public final Accuracy accuracy;

        public enum SensorType {
            GYROSCOPE
        }

        public enum Accuracy {
            UNRELIABLE,
            LOW,
            MEDIUM,
            HIGH;

            public static Accuracy fromAndroidAccuracy(int androidAccuracy) {
                // 使用整数常量值替代 SensorManager 常量
                switch (androidAccuracy) {
                    case 1: // SENSOR_STATUS_LOW_ACCURACY
                        return LOW;
                    case 2: // SENSOR_STATUS_MEDIUM_ACCURACY
                        return MEDIUM;
                    case 3: // SENSOR_STATUS_HIGH_ACCURACY
                        return HIGH;
                    default:
                        return UNRELIABLE;
                }
            }
        }

        public RawSensorEvent(long timeNanos, SensorType sensorType, float[] values, Accuracy accuracy) {
            this.timeNanos = timeNanos;
            this.sensorType = sensorType;
            this.values = values;
            this.accuracy = accuracy;
        }
    }

    // Raw 丢包事件
    public static final class RawDropEvent implements RawEvent {
        public final long timeNanos;
        public final Kind kind;
        public final int droppedCount;

        public enum Kind {
            SENSOR
        }

        public RawDropEvent(long timeNanos, Kind kind, int droppedCount) {
            this.timeNanos = timeNanos;
            this.kind = kind;
            this.droppedCount = droppedCount;
        }
    }

    // Overlay View 类
    private class OverlayView extends View {
        public OverlayView(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            handleTouchEvent(event);
            return true;
        }
    }
}