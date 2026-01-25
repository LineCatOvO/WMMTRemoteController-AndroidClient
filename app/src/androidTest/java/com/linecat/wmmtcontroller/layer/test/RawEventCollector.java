package com.linecat.wmmtcontroller.layer.test;

import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawDropEvent;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawPointerEvent;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawSensorEvent;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer.RawWindowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * RawEventCollector 是一个测试辅助类，用于收集和验证 RawEvent 事件
 */
public class RawEventCollector implements PlatformAdaptationLayer.RawEventSink {
    private final List<RawWindowEvent> windowEvents = new ArrayList<>();
    private final List<RawPointerEvent> pointerEvents = new ArrayList<>();
    private final List<RawSensorEvent> sensorEvents = new ArrayList<>();
    private final List<RawDropEvent> dropEvents = new ArrayList<>();
    private final Object lock = new Object();

    @Override
    public void onRawWindowEvent(RawWindowEvent e) {
        synchronized (lock) {
            windowEvents.add(e);
            lock.notifyAll();
        }
    }

    @Override
    public void onRawPointerEvent(RawPointerEvent e) {
        synchronized (lock) {
            pointerEvents.add(e);
            lock.notifyAll();
        }
    }

    @Override
    public void onRawSensorEvent(RawSensorEvent e) {
        synchronized (lock) {
            sensorEvents.add(e);
            lock.notifyAll();
        }
    }

    @Override
    public void onRawDropEvent(RawDropEvent e) {
        synchronized (lock) {
            dropEvents.add(e);
            lock.notifyAll();
        }
    }

    /**
     * 获取所有收集的 RawWindowEvent
     */
    public List<RawWindowEvent> getWindowEvents() {
        synchronized (lock) {
            return new ArrayList<>(windowEvents);
        }
    }

    /**
     * 获取所有收集的 RawPointerEvent
     */
    public List<RawPointerEvent> getPointerEvents() {
        synchronized (lock) {
            return new ArrayList<>(pointerEvents);
        }
    }

    /**
     * 获取所有收集的 RawSensorEvent
     */
    public List<RawSensorEvent> getSensorEvents() {
        synchronized (lock) {
            return new ArrayList<>(sensorEvents);
        }
    }

    /**
     * 获取所有收集的 RawDropEvent
     */
    public List<RawDropEvent> getDropEvents() {
        synchronized (lock) {
            return new ArrayList<>(dropEvents);
        }
    }

    /**
     * 等待特定类型的 RawPointerEvent
     */
    public RawPointerEvent awaitPointerEvent(Predicate<RawPointerEvent> predicate, long timeoutMs) throws InterruptedException {
        return awaitEvent(pointerEvents, predicate, timeoutMs);
    }

    /**
     * 等待特定类型的 RawSensorEvent
     */
    public RawSensorEvent awaitSensorEvent(Predicate<RawSensorEvent> predicate, long timeoutMs) throws InterruptedException {
        return awaitEvent(sensorEvents, predicate, timeoutMs);
    }

    /**
     * 通用事件等待方法
     */
    private <T> T awaitEvent(List<T> eventList, Predicate<T> predicate, long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        synchronized (lock) {
            // 检查是否已经有匹配的事件
            for (T event : eventList) {
                if (predicate.test(event)) {
                    return event;
                }
            }

            // 等待新事件
            long start = System.currentTimeMillis();
            long remaining = timeoutMs;

            while (remaining > 0) {
                lock.wait(remaining);
                
                for (T event : eventList) {
                    if (predicate.test(event)) {
                        return event;
                    }
                }
                
                remaining = timeoutMs - (System.currentTimeMillis() - start);
            }
        }

        return null;
    }

    /**
     * 清空收集的事件
     */
    public void clear() {
        synchronized (lock) {
            windowEvents.clear();
            pointerEvents.clear();
            sensorEvents.clear();
            dropEvents.clear();
        }
    }

    /**
     * 获取 RawPointerEvent 数量
     */
    public int getPointerEventCount() {
        synchronized (lock) {
            return pointerEvents.size();
        }
    }

    /**
     * 获取 RawSensorEvent 数量
     */
    public int getSensorEventCount() {
        synchronized (lock) {
            return sensorEvents.size();
        }
    }

    /**
     * 获取 RawWindowEvent 数量
     */
    public int getWindowEventCount() {
        synchronized (lock) {
            return windowEvents.size();
        }
    }

    /**
     * 获取 RawDropEvent 数量
     */
    public int getDropEventCount() {
        synchronized (lock) {
            return dropEvents.size();
        }
    }

    /**
     * 检查是否包含 Pointer DOWN 事件
     */
    public boolean hasPointerDownEvent() {
        synchronized (lock) {
            return pointerEvents.stream()
                    .anyMatch(event -> event.action == RawPointerEvent.Action.DOWN);
        }
    }

    /**
     * 检查是否包含 Pointer UP 事件
     */
    public boolean hasPointerUpEvent() {
        synchronized (lock) {
            return pointerEvents.stream()
                    .anyMatch(event -> event.action == RawPointerEvent.Action.UP);
        }
    }

    /**
     * 检查是否包含 Pointer MOVE 事件
     */
    public boolean hasPointerMoveEvent() {
        synchronized (lock) {
            return pointerEvents.stream()
                    .anyMatch(event -> event.action == RawPointerEvent.Action.MOVE);
        }
    }

    /**
     * 检查是否包含 Pointer CANCEL 事件
     */
    public boolean hasPointerCancelEvent() {
        synchronized (lock) {
            return pointerEvents.stream()
                    .anyMatch(event -> event.action == RawPointerEvent.Action.CANCEL);
        }
    }

    /**
     * 检查是否包含 Sensor 事件
     */
    public boolean hasSensorEvent() {
        synchronized (lock) {
            return !sensorEvents.isEmpty();
        }
    }

    /**
     * 检查是否包含 Window 事件
     */
    public boolean hasWindowEvent() {
        synchronized (lock) {
            return !windowEvents.isEmpty();
        }
    }

    /**
     * 检查是否包含 Drop 事件
     */
    public boolean hasDropEvent() {
        synchronized (lock) {
            return !dropEvents.isEmpty();
        }
    }
}
