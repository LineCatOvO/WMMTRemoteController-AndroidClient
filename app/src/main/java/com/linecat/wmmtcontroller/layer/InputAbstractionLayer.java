package com.linecat.wmmtcontroller.layer;

import android.content.Context;

import com.linecat.wmmtcontroller.debug.RawInputInspectorManager;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawDropEvent;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawPointerEvent;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawSensorEvent;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawWindowEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Input Abstraction Layer
 * <p>
 * 负责：归一化、状态机、合并、原语输出
 * 不负责：Android API、权限、overlay
 */
public final class InputAbstractionLayer implements PlatformAdaptationLayer.RawEventSink {
    private static final String TAG = "InputAbstractionLayer";
    
    // 目标合并频率：60Hz
    private static final long TARGET_FREQUENCY_NS = TimeUnit.SECONDS.toNanos(1) / 60;
    
    // 轴映射规则（必须与代码注释一致）
    // yawRate   = values[2]
    // pitchRate = values[0]
    // rollRate  = values[1]
    
    private final OutputSink sink;
    private final Map<Integer, PointerState> pointerStates;
    private final Set<Integer> changedIds;
    private final RawInputInspectorManager inspectorManager;
    
    // 当前显示 metrics
    private int displayWidthPx;
    private int displayHeightPx;
    private int displayRotation;
    
    // MOVE 合并相关
    private long lastMoveOutputTimeNs;
    private boolean hasPendingMove;
    
    // 陀螺仪数据
    private float lastYawRate;
    private float lastPitchRate;
    private float lastRollRate;
    private RawSensorEvent.Accuracy lastGyroAccuracy;
    private long lastGyroTimeNs;
    
    // 取消标志
    private boolean isCanceled;
    
    /**
     * 输入抽象层构造函数
     * @param sink 输出接收器
     */
    public InputAbstractionLayer(OutputSink sink) {
        this(null, sink);
    }
    
    /**
     * 输入抽象层构造函数
     * @param context 上下文
     * @param sink 输出接收器
     */
    public InputAbstractionLayer(Context context, OutputSink sink) {
        this.sink = sink;
        this.pointerStates = new ConcurrentHashMap<>();
        this.changedIds = ConcurrentHashMap.newKeySet();
        this.inspectorManager = context != null ? RawInputInspectorManager.getInstance(context) : null;
        
        // 初始化默认 metrics
        this.displayWidthPx = 1080;
        this.displayHeightPx = 1920;
        this.displayRotation = 0;
        
        // 初始化 MOVE 合并相关
        this.lastMoveOutputTimeNs = 0;
        this.hasPendingMove = false;
        
        // 初始化陀螺仪数据
        this.lastYawRate = 0.0f;
        this.lastPitchRate = 0.0f;
        this.lastRollRate = 0.0f;
        this.lastGyroAccuracy = RawSensorEvent.Accuracy.UNRELIABLE;
        this.lastGyroTimeNs = 0;
        
        // 初始化取消标志
        this.isCanceled = false;
    }
    
    /**
     * 接收 Raw 窗口事件
     */
    @Override
    public void onRawWindowEvent(RawWindowEvent e) {
        // 更新 metrics
        this.displayWidthPx = e.metrics.widthPx;
        this.displayHeightPx = e.metrics.heightPx;
        this.displayRotation = e.metrics.rotation;
        
        // 发送 metrics 变化事件（如果需要）
        // sink.onMetricsChanged(e.metrics);
    }
    
    /**
     * 接收 Raw 指针事件
     */
    @Override
    public void onRawPointerEvent(RawPointerEvent e) {
        long timeNanos = e.timeNanos;
        
        // 处理指针事件
        switch (e.action) {
            case DOWN:
                handlePointerDown(e);
                break;
            case MOVE:
                handlePointerMove(e);
                break;
            case UP:
                handlePointerUp(e);
                break;
            case CANCEL:
                handlePointerCancel(e);
                break;
        }
        
        // 更新原始输入检查器数据
        if (inspectorManager != null && !e.pointers.isEmpty()) {
            RawPointerEvent.Pointer pointer = e.pointers.get(0);
            inspectorManager.onTouchEvent(null); // 这里需要实际的MotionEvent，后续优化
            // 临时解决方案：直接更新坐标
            for (RawPointerEvent.Pointer p : e.pointers) {
                // 这里需要转换为实际的触摸事件，后续优化
                inspectorManager.updateTouchData(p.x, p.y);
            }
        }
        
        // 检查是否需要输出 PointerFrame
        if (e.action != RawPointerEvent.Action.MOVE || shouldOutputMove(timeNanos)) {
            outputPointerFrame(timeNanos);
        }
    }
    
    /**
     * 接收 Raw 传感器事件
     */
    @Override
    public void onRawSensorEvent(RawSensorEvent e) {
        if (e.sensorType == RawSensorEvent.SensorType.GYROSCOPE) {
            // 映射陀螺仪轴
            // yawRate   = values[2]
            // pitchRate = values[0]
            // rollRate  = values[1]
            float yawRate = e.values[2];
            float pitchRate = e.values[0];
            float rollRate = e.values[1];
            
            // 更新陀螺仪数据
            this.lastYawRate = yawRate;
            this.lastPitchRate = pitchRate;
            this.lastRollRate = rollRate;
            this.lastGyroAccuracy = e.accuracy;
            this.lastGyroTimeNs = e.timeNanos;
            
            // 更新原始输入检查器数据
            if (inspectorManager != null) {
                inspectorManager.updateGyroData(pitchRate, rollRate, yawRate);
            }
            
            // 输出 GyroFrame
            outputGyroFrame(e.timeNanos);
        }
    }
    
    /**
     * 接收 Raw 丢包事件
     */
    @Override
    public void onRawDropEvent(RawDropEvent e) {
        // 处理丢包事件
        // 目前只记录日志，不做其他处理
        // Log.d(TAG, "Dropped " + e.droppedCount + " " + e.kind + " events");
    }
    
    /**
     * 处理指针 DOWN 事件
     */
    private void handlePointerDown(RawPointerEvent e) {
        // 重置取消标志
        isCanceled = false;
        
        // 处理每个指针
        for (RawPointerEvent.Pointer pointer : e.pointers) {
            // 旋转坐标到基准方向，不归一化
            RotatedPoint rotated = rotatePoint(pointer.x, pointer.y, e.display.rotation);
            
            // 创建新的 PointerState
            PointerState state = new PointerState(
                    PointerState.Phase.DOWN,
                    rotated.x,
                    rotated.y
            );
            
            // 更新状态
            pointerStates.put(pointer.id, state);
            changedIds.add(pointer.id);
        }
        
        // 立即输出 DOWN 事件
        hasPendingMove = false;
    }
    
    /**
     * 处理指针 MOVE 事件
     */
    private void handlePointerMove(RawPointerEvent e) {
        // 如果已取消，忽略 MOVE 事件
        if (isCanceled) {
            return;
        }
        
        // 处理每个指针
        for (RawPointerEvent.Pointer pointer : e.pointers) {
            // 检查指针是否已存在
            if (pointerStates.containsKey(pointer.id)) {
                // 旋转坐标到基准方向，不归一化
                RotatedPoint rotated = rotatePoint(pointer.x, pointer.y, e.display.rotation);
                
                // 更新 PointerState
                PointerState state = pointerStates.get(pointer.id);
                state.phase = PointerState.Phase.MOVE;
                state.x = rotated.x;
                state.y = rotated.y;
                
                changedIds.add(pointer.id);
                hasPendingMove = true;
            }
        }
    }
    
    /**
     * 处理指针 UP 事件
     */
    private void handlePointerUp(RawPointerEvent e) {
        // 处理每个指针
        for (RawPointerEvent.Pointer pointer : e.pointers) {
            // 检查指针是否已存在
            if (pointerStates.containsKey(pointer.id)) {
                // 旋转坐标到基准方向（90° home-on-right landscape），不归一化
                RotatedPoint rotated = rotatePoint(pointer.x, pointer.y, e.display.rotation);
                
                // 更新 PointerState
                PointerState state = pointerStates.get(pointer.id);
                state.phase = PointerState.Phase.UP;
                state.x = rotated.x;
                state.y = rotated.y;
                
                changedIds.add(pointer.id);
            }
        }
        
        // 立即输出 UP 事件
        hasPendingMove = false;
    }
    
    /**
     * 处理指针 CANCEL 事件
     */
    private void handlePointerCancel(RawPointerEvent e) {
        // 标记为已取消
        isCanceled = true;
        
        // 清空所有指针状态
        pointerStates.clear();
        changedIds.clear();
        
        // 立即输出 CANCEL 事件
        hasPendingMove = false;
    }
    
    /**
     * 旋转坐标到基准方向（90° home-on-right landscape），不进行归一化
     */
    private RotatedPoint rotatePoint(float xPx, float yPx, int rotation) {
        // 只旋转坐标到90° home-on-right landscape，不进行归一化
        float rotatedX, rotatedY;
        switch (rotation) {
            case 0: // ROTATION_0 (Natural Portrait) → 90° landscape
                rotatedX = displayHeightPx - yPx;
                rotatedY = xPx;
                break;
            case 1: // ROTATION_90 (Landscape) → 90° landscape
                rotatedX = xPx;
                rotatedY = yPx;
                break;
            case 2: // ROTATION_180 (Upside Down Portrait) → 90° landscape
                rotatedX = yPx;
                rotatedY = displayWidthPx - xPx;
                break;
            case 3: // ROTATION_270 (Reverse Landscape) → 90° landscape
                rotatedX = displayWidthPx - xPx;
                rotatedY = displayHeightPx - yPx;
                break;
            default:
                rotatedX = xPx;
                rotatedY = yPx;
        }
        
        return new RotatedPoint(rotatedX, rotatedY);
    }
    
    /**
     * 限制值在指定范围内
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 检查是否需要输出 MOVE 事件
     */
    private boolean shouldOutputMove(long currentTimeNs) {
        if (!hasPendingMove) {
            return false;
        }
        
        if (currentTimeNs - lastMoveOutputTimeNs >= TARGET_FREQUENCY_NS) {
            lastMoveOutputTimeNs = currentTimeNs;
            hasPendingMove = false;
            return true;
        }
        
        return false;
    }
    
    /**
     * 输出 PointerFrame
     */
    private void outputPointerFrame(long timeNanos) {
        if (pointerStates.isEmpty() && !isCanceled) {
            return;
        }
        
        // 创建 CoordinateSpace
        CoordinateSpace space = new CoordinateSpace(
                displayWidthPx,
                displayHeightPx,
                CoordinateSpace.Basis.LANDSCAPE_90
        );
        
        // 创建 PointerFrame
        PointerFrame frame = new PointerFrame(
                timeNanos,
                new HashMap<>(pointerStates),
                new ArrayList<>(changedIds),
                isCanceled,
                space
        );
        
        // 发送到上层
        sink.onPointerFrame(frame);
        
        // 清理已 UP 的指针
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, PointerState> entry : pointerStates.entrySet()) {
            if (entry.getValue().phase == PointerState.Phase.UP) {
                toRemove.add(entry.getKey());
            }
        }
        for (int id : toRemove) {
            pointerStates.remove(id);
        }
        
        // 清理 changedIds
        changedIds.clear();
        
        // 重置取消标志
        if (isCanceled) {
            isCanceled = false;
        }
    }
    
    /**
     * 输出 GyroFrame
     */
    private void outputGyroFrame(long timeNanos) {
        // 创建 GyroFrame
        GyroFrame frame = new GyroFrame(
                timeNanos,
                lastYawRate,
                lastPitchRate,
                lastRollRate,
                convertGyroAccuracy(lastGyroAccuracy)
        );
        
        // 发送到上层
        sink.onGyroFrame(frame);
    }
    
    /**
     * 转换陀螺仪精度
     */
    private GyroFrame.Accuracy convertGyroAccuracy(RawSensorEvent.Accuracy androidAccuracy) {
        switch (androidAccuracy) {
            case LOW:
                return GyroFrame.Accuracy.LOW;
            case MEDIUM:
                return GyroFrame.Accuracy.MEDIUM;
            case HIGH:
                return GyroFrame.Accuracy.HIGH;
            default:
                return GyroFrame.Accuracy.UNRELIABLE;
        }
    }
    
    /**
     * 旋转后的像素点
     */
    private static class RotatedPoint {
        public final float x;
        public final float y;
        
        public RotatedPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
    
    // 输出接收器接口
    public interface OutputSink {
        void onPointerFrame(PointerFrame frame);
        void onGyroFrame(GyroFrame frame);
    }
    
    /**
     * 坐标空间
     */
    public static final class CoordinateSpace {
        public enum Basis {
            LANDSCAPE_90 // 90° home-on-right landscape
        }
        
        public final int widthPx;
        public final int heightPx;
        public final Basis basis;
        
        public CoordinateSpace(int widthPx, int heightPx, Basis basis) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.basis = basis;
        }
    }
    
    /**
     * 指针帧
     */
    public static final class PointerFrame {
        public final long timeNanos;
        public final Map<Integer, PointerState> pointersById;
        public final List<Integer> changedIds;
        public final boolean canceled;
        public final CoordinateSpace space;
        
        public PointerFrame(long timeNanos, Map<Integer, PointerState> pointersById, List<Integer> changedIds, boolean canceled, CoordinateSpace space) {
            this.timeNanos = timeNanos;
            this.pointersById = pointersById;
            this.changedIds = changedIds;
            this.canceled = canceled;
            this.space = space;
        }
        
        public boolean isCanceled() {
            return canceled;
        }
    }
    
    /**
     * 指针状态
     */
    public static final class PointerState {
        public enum Phase {
            DOWN,
            MOVE,
            UP
        }
        
        public Phase phase;
        public float x;
        public float y;
        
        public PointerState(Phase phase, float x, float y) {
            this.phase = phase;
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * 陀螺仪帧
     */
    public static final class GyroFrame {
        public final long timeNanos;
        public final float yawRate;
        public final float pitchRate;
        public final float rollRate;
        public final Accuracy accuracy;
        
        public enum Accuracy {
            UNRELIABLE,
            LOW,
            MEDIUM,
            HIGH
        }
        
        public GyroFrame(long timeNanos, float yawRate, float pitchRate, float rollRate, Accuracy accuracy) {
            this.timeNanos = timeNanos;
            this.yawRate = yawRate;
            this.pitchRate = pitchRate;
            this.rollRate = rollRate;
            this.accuracy = accuracy;
        }
    }
    
    /**
     * 构造函数
     */
    public InputAbstractionLayer() {
        // 默认构造函数，用于测试
        this(new OutputSink() {
            @Override
            public void onPointerFrame(PointerFrame frame) {
                // 空实现，用于测试
            }
            
            @Override
            public void onGyroFrame(GyroFrame frame) {
                // 空实现，用于测试
            }
        });
    }
}
