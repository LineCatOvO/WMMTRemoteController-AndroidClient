package com.linecat.wmmtcontroller.layer.test;

import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * RawEventCollector 是一个测试辅助类，用于收集和验证 RawEvent 事件
 */
public class RawEventCollector implements PlatformAdaptationLayer.RawEventSink {
    private final List<PlatformAdaptationLayer.RawEvent> events = new ArrayList<>();
    private final Object lock = new Object();

    @Override
    public void onRawWindowEvent(PlatformAdaptationLayer.RawWindowEvent e) {
        synchronized (lock) {
            events.add(e);
            lock.notifyAll();
        }
    }

    @Override
    public void onRawPointerEvent(PlatformAdaptationLayer.RawPointerEvent e) {
        synchronized (lock) {
            events.add(e);
            lock.notifyAll();
        }
    }

    @Override
    public void onRawSensorEvent(PlatformAdaptationLayer.RawSensorEvent e) {
        synchronized (lock) {
            events.add(e);
            lock.notifyAll();
        }
    }

    @Override
    public void onRawDropEvent(PlatformAdaptationLayer.RawDropEvent e) {
        synchronized (lock) {
            events.add(e);
            lock.notifyAll();
        }
    }

    /**
     * 获取所有收集的事件
     */
    public List<PlatformAdaptationLayer.RawEvent> getEvents() {
        synchronized (lock) {
            return new ArrayList<>(events);
        }
    }

    /**
     * 等待特定类型的事件
     * @param predicate 事件匹配条件
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否在超时前找到匹配的事件
     */
    public boolean awaitEvent(Predicate<PlatformAdaptationLayer.RawEvent> predicate, long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(1);

        synchronized (lock) {
            // 检查是否已经有匹配的事件
            for (PlatformAdaptationLayer.RawEvent event : events) {
                if (predicate.test(event)) {
                    return true;
                }
            }

            // 等待新事件
            try {
                long start = System.currentTimeMillis();
                long remaining = timeoutMs;

                while (remaining > 0) {
                    lock.wait(remaining);
                    
                    for (PlatformAdaptationLayer.RawEvent event : events) {
                        if (predicate.test(event)) {
                            return true;
                        }
                    }
                    
                    remaining = timeoutMs - (System.currentTimeMillis() - start);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * 清空收集的事件
     */
    public void clear() {
        synchronized (lock) {
            events.clear();
        }
    }

    /**
     * 获取事件数量
     */
    public int size() {
        synchronized (lock) {
            return events.size();
        }
    }

    /**
     * 检查是否包含特定类型的事件
     */
    public boolean containsEventOfType(Class<?> eventType) {
        synchronized (lock) {
            for (PlatformAdaptationLayer.RawEvent event : events) {
                if (eventType.isInstance(event)) {
                    return true;
                }
            }
            return false;
        }
    }
}