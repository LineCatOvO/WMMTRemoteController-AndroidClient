package com.linecat.wmmtcontroller.layer.test;

import com.linecat.wmmtcontroller.layer.InputAbstractionLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * PrimitiveCollector 是一个测试辅助类，用于收集和验证 PointerFrame 和 GyroFrame 事件
 */
public class PrimitiveCollector implements InputAbstractionLayer.OutputSink {
    private final List<InputAbstractionLayer.PointerFrame> pointerFrames = new ArrayList<>();
    private final List<InputAbstractionLayer.GyroFrame> gyroFrames = new ArrayList<>();
    private final Object lock = new Object();

    @Override
    public void onPointerFrame(InputAbstractionLayer.PointerFrame frame) {
        synchronized (lock) {
            pointerFrames.add(frame);
            lock.notifyAll();
        }
    }

    @Override
    public void onGyroFrame(InputAbstractionLayer.GyroFrame frame) {
        synchronized (lock) {
            gyroFrames.add(frame);
            lock.notifyAll();
        }
    }

    /**
     * 获取所有收集的 PointerFrame
     */
    public List<InputAbstractionLayer.PointerFrame> getPointerFrames() {
        synchronized (lock) {
            return new ArrayList<>(pointerFrames);
        }
    }

    /**
     * 获取所有收集的 GyroFrame
     */
    public List<InputAbstractionLayer.GyroFrame> getGyroFrames() {
        synchronized (lock) {
            return new ArrayList<>(gyroFrames);
        }
    }

    /**
     * 等待特定类型的 PointerFrame
     * @param predicate 事件匹配条件
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否在超时前找到匹配的事件
     */
    public boolean awaitPointerFrame(Predicate<InputAbstractionLayer.PointerFrame> predicate, long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(1);

        synchronized (lock) {
            // 检查是否已经有匹配的事件
            for (InputAbstractionLayer.PointerFrame event : pointerFrames) {
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
                    
                    for (InputAbstractionLayer.PointerFrame event : pointerFrames) {
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
     * 等待特定类型的 GyroFrame
     * @param predicate 事件匹配条件
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否在超时前找到匹配的事件
     */
    public boolean awaitGyroFrame(Predicate<InputAbstractionLayer.GyroFrame> predicate, long timeoutMs) {
        return awaitEvent(gyroFrames, predicate, timeoutMs);
    }

    /**
     * 通用事件等待方法
     */
    private <T> boolean awaitEvent(List<T> eventList, Predicate<T> predicate, long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(1);

        synchronized (lock) {
            // 检查是否已经有匹配的事件
            for (T event : eventList) {
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
                    
                    for (T event : eventList) {
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
            pointerFrames.clear();
            gyroFrames.clear();
        }
    }

    /**
     * 获取 PointerFrame 数量
     */
    public int getPointerFrameCount() {
        synchronized (lock) {
            return pointerFrames.size();
        }
    }

    /**
     * 获取 GyroFrame 数量
     */
    public int getGyroFrameCount() {
        synchronized (lock) {
            return gyroFrames.size();
        }
    }

    /**
     * 检查是否包含 canceled 的 PointerFrame
     */
    public boolean containsCanceledPointerFrame() {
        synchronized (lock) {
            for (InputAbstractionLayer.PointerFrame frame : pointerFrames) {
                if (frame.canceled) {
                    return true;
                }
            }
            return false;
        }
    }
}
