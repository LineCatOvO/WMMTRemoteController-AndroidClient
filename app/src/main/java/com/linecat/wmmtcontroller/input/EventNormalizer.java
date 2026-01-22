package com.linecat.wmmtcontroller.input;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 事件标准化器
 * 核心职责：
 * 1. 确保事件格式一致性
 * 2. 过滤无效事件
 * 3. 合并重复事件
 * 4. 优化事件流
 * 
 * 遵循设计原则：
 * - 确保发送到 JS 的事件是干净、一致、语义化的
 * - 减少不必要的事件，提高性能
 * - 确保事件的时序正确性
 */
public class EventNormalizer {
    
    private static final String TAG = "EventNormalizer";
    
    // 事件队列，用于缓冲和处理事件
    private final Queue<NormalizedEvent> eventQueue;
    
    // 上一个事件，用于合并重复事件
    private NormalizedEvent lastEvent;
    
    // 事件间隔阈值（毫秒），用于过滤过于频繁的事件
    private static final long EVENT_INTERVAL_THRESHOLD = 10;
    
    /**
     * 构造函数
     */
    public EventNormalizer() {
        this.eventQueue = new LinkedList<>();
        this.lastEvent = null;
    }
    
    /**
     * 处理输入解释器生成的标准化事件
     * @param event 标准化事件
     */
    public void processEvent(NormalizedEvent event) {
        if (event == null) {
            return;
        }
        
        // 1. 验证事件有效性
        if (!isValidEvent(event)) {
            Log.w(TAG, "Invalid event: " + event);
            return;
        }
        
        // 2. 过滤过于频繁的重复事件
        if (isDuplicateEvent(event)) {
            Log.d(TAG, "Duplicate event skipped: " + event);
            return;
        }
        
        // 3. 标准化事件（根据需要调整事件属性）
        NormalizedEvent normalizedEvent = normalizeEvent(event);
        
        // 4. 将事件加入队列
        eventQueue.offer(normalizedEvent);
        
        // 5. 更新上一个事件
        lastEvent = normalizedEvent;
    }
    
    /**
     * 获取下一个处理后的事件
     * @return 处理后的标准化事件，如果队列为空则返回null
     */
    public NormalizedEvent getNextEvent() {
        return eventQueue.poll();
    }
    
    /**
     * 检查队列是否为空
     * @return 如果队列为空则返回true，否则返回false
     */
    public boolean isEmpty() {
        return eventQueue.isEmpty();
    }
    
    /**
     * 清除事件队列
     */
    public void clear() {
        eventQueue.clear();
        lastEvent = null;
    }
    
    /**
     * 验证事件有效性
     * @param event 标准化事件
     * @return 如果事件有效则返回true，否则返回false
     */
    private boolean isValidEvent(NormalizedEvent event) {
        // 检查事件基本属性
        if (event.getRegionId() == null || event.getRegionId().trim().isEmpty()) {
            return false;
        }
        
        // 检查事件类型
        if (event.getType() == null) {
            return false;
        }
        
        // 根据事件类型进行特定验证
        switch (event.getType()) {
            case BUTTON:
                return isValidButtonEvent((ButtonEvent) event);
            case AXIS:
                return isValidAxisEvent((AxisEvent) event);
            case GESTURE:
                return isValidGestureEvent((GestureEvent) event);
            default:
                return false;
        }
    }
    
    /**
     * 验证按钮事件有效性
     */
    private boolean isValidButtonEvent(ButtonEvent event) {
        // 按钮事件没有特殊验证，只要基本属性有效即可
        return true;
    }
    
    /**
     * 验证轴事件有效性
     */
    private boolean isValidAxisEvent(AxisEvent event) {
        // 检查轴值是否在有效范围内
        float valueX = event.getValueX();
        float valueY = event.getValueY();
        
        return valueX >= -1.0f && valueX <= 1.0f && 
               valueY >= -1.0f && valueY <= 1.0f;
    }
    
    /**
     * 验证手势事件有效性
     */
    private boolean isValidGestureEvent(GestureEvent event) {
        // 检查手势类型
        return event.getGestureType() != null;
    }
    
    /**
     * 检查是否为重复事件
     * @param event 标准化事件
     * @return 如果是重复事件则返回true，否则返回false
     */
    private boolean isDuplicateEvent(NormalizedEvent event) {
        if (lastEvent == null) {
            return false;
        }
        
        // 检查事件类型是否相同
        if (event.getType() != lastEvent.getType()) {
            return false;
        }
        
        // 检查区域ID是否相同
        if (!event.getRegionId().equals(lastEvent.getRegionId())) {
            return false;
        }
        
        // 检查时间间隔是否小于阈值
        long timeDiff = event.getTimestamp() - lastEvent.getTimestamp();
        if (timeDiff > EVENT_INTERVAL_THRESHOLD) {
            return false;
        }
        
        // 根据事件类型检查是否为重复事件
        switch (event.getType()) {
            case BUTTON:
                return isDuplicateButtonEvent((ButtonEvent) event, (ButtonEvent) lastEvent);
            case AXIS:
                return isDuplicateAxisEvent((AxisEvent) event, (AxisEvent) lastEvent);
            case GESTURE:
                return isDuplicateGestureEvent((GestureEvent) event, (GestureEvent) lastEvent);
            default:
                return false;
        }
    }
    
    /**
     * 检查是否为重复按钮事件
     */
    private boolean isDuplicateButtonEvent(ButtonEvent event, ButtonEvent lastEvent) {
        // 如果按钮状态相同，则认为是重复事件
        return event.isPressed() == lastEvent.isPressed();
    }
    
    /**
     * 检查是否为重复轴事件
     */
    private boolean isDuplicateAxisEvent(AxisEvent event, AxisEvent lastEvent) {
        // 如果轴值变化很小，则认为是重复事件
        float deltaX = Math.abs(event.getValueX() - lastEvent.getValueX());
        float deltaY = Math.abs(event.getValueY() - lastEvent.getValueY());
        
        // 轴值变化阈值（0.01 = 1%）
        return deltaX < 0.01f && deltaY < 0.01f;
    }
    
    /**
     * 检查是否为重复手势事件
     */
    private boolean isDuplicateGestureEvent(GestureEvent event, GestureEvent lastEvent) {
        // 如果手势类型相同，则认为是重复事件
        return event.getGestureType() == lastEvent.getGestureType();
    }
    
    /**
     * 标准化事件
     * @param event 标准化事件
     * @return 标准化后的事件
     */
    private NormalizedEvent normalizeEvent(NormalizedEvent event) {
        // 根据事件类型进行标准化处理
        switch (event.getType()) {
            case BUTTON:
                return normalizeButtonEvent((ButtonEvent) event);
            case AXIS:
                return normalizeAxisEvent((AxisEvent) event);
            case GESTURE:
                return normalizeGestureEvent((GestureEvent) event);
            default:
                return event;
        }
    }
    
    /**
     * 标准化按钮事件
     */
    private NormalizedEvent normalizeButtonEvent(ButtonEvent event) {
        // 按钮事件通常不需要特殊标准化，直接返回
        return event;
    }
    
    /**
     * 标准化轴事件
     */
    private NormalizedEvent normalizeAxisEvent(AxisEvent event) {
        // 确保轴值在 -1.0 到 1.0 范围内
        float valueX = Math.max(-1.0f, Math.min(1.0f, event.getValueX()));
        float valueY = Math.max(-1.0f, Math.min(1.0f, event.getValueY()));
        
        // 如果轴值变化很小，使用上一个轴值，减少抖动
        if (lastEvent != null && lastEvent.getType() == NormalizedEvent.EventType.AXIS) {
            AxisEvent lastAxisEvent = (AxisEvent) lastEvent;
            
            float deltaX = Math.abs(valueX - lastAxisEvent.getValueX());
            float deltaY = Math.abs(valueY - lastAxisEvent.getValueY());
            
            // 轴值抖动阈值（0.005 = 0.5%）
            if (deltaX < 0.005f) {
                valueX = lastAxisEvent.getValueX();
            }
            if (deltaY < 0.005f) {
                valueY = lastAxisEvent.getValueY();
            }
        }
        
        return AxisEvent.create(event.getRegionId(), valueX, valueY);
    }
    
    /**
     * 标准化手势事件
     */
    private NormalizedEvent normalizeGestureEvent(GestureEvent event) {
        // 手势事件通常不需要特殊标准化，直接返回
        return event;
    }
    
    /**
     * 获取事件队列大小
     * @return 事件队列大小
     */
    public int getEventQueueSize() {
        return eventQueue.size();
    }
    
    /**
     * 重置事件标准化器
     */
    public void reset() {
        clear();
        lastEvent = null;
    }
}
