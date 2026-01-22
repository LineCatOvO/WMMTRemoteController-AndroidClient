package com.linecat.wmmtcontroller.monitor;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统状态监控器
 * 负责收集和管理系统状态，包括：
 * - 控制状态
 * - 布局状态
 * - RTT（Round Trip Time）
 * - 丢包率
 * - 安全状态
 */
public class SystemMonitor {
    private static final String TAG = "SystemMonitor";
    private static SystemMonitor instance;

    // 系统状态枚举
    public enum ControlState {
        IDLE,       // 空闲
        RUNNING,    // 运行中
        DISABLED,   // 已禁用
        ERROR       // 错误
    }

    public enum SafetyState {
        SAFE,       // 安全
        WARNING,    // 警告
        UNSAFE      // 不安全
    }

    // 系统状态
    private ControlState controlState = ControlState.IDLE;
    private String currentLayout = "Unknown";
    private long rtt = 0;
    private double packetLossRate = 0.0;
    private SafetyState safetyState = SafetyState.SAFE;
    private long lastUpdateTime = System.currentTimeMillis();

    // 状态变更监听器
    private Map<String, StateChangeListener> stateChangeListeners = new HashMap<>();

    private SystemMonitor() {
        // 私有构造函数，单例模式
    }

    /**
     * 获取系统监控器实例
     */
    public static synchronized SystemMonitor getInstance() {
        if (instance == null) {
            instance = new SystemMonitor();
        }
        return instance;
    }

    /**
     * 设置控制状态
     */
    public synchronized void setControlState(ControlState state) {
        if (this.controlState != state) {
            ControlState oldState = this.controlState;
            this.controlState = state;
            this.lastUpdateTime = System.currentTimeMillis();
            Log.d(TAG, "Control state changed: " + oldState + " → " + state);
            notifyStateChange("controlState", oldState, state);
        }
    }

    /**
     * 获取控制状态
     */
    public synchronized ControlState getControlState() {
        return controlState;
    }

    /**
     * 设置当前布局
     */
    public synchronized void setCurrentLayout(String layoutName) {
        if (!this.currentLayout.equals(layoutName)) {
            String oldLayout = this.currentLayout;
            this.currentLayout = layoutName;
            this.lastUpdateTime = System.currentTimeMillis();
            Log.d(TAG, "Layout changed: " + oldLayout + " → " + layoutName);
            notifyStateChange("currentLayout", oldLayout, layoutName);
        }
    }

    /**
     * 获取当前布局
     */
    public synchronized String getCurrentLayout() {
        return currentLayout;
    }

    /**
     * 设置 RTT（Round Trip Time）
     */
    public synchronized void setRtt(long rtt) {
        this.rtt = rtt;
        this.lastUpdateTime = System.currentTimeMillis();
        notifyStateChange("rtt", null, rtt);
    }

    /**
     * 获取 RTT
     */
    public synchronized long getRtt() {
        return rtt;
    }

    /**
     * 设置丢包率
     */
    public synchronized void setPacketLossRate(double packetLossRate) {
        this.packetLossRate = packetLossRate;
        this.lastUpdateTime = System.currentTimeMillis();
        notifyStateChange("packetLossRate", null, packetLossRate);
    }

    /**
     * 获取丢包率
     */
    public synchronized double getPacketLossRate() {
        return packetLossRate;
    }

    /**
     * 设置安全状态
     */
    public synchronized void setSafetyState(SafetyState state) {
        if (this.safetyState != state) {
            SafetyState oldState = this.safetyState;
            this.safetyState = state;
            this.lastUpdateTime = System.currentTimeMillis();
            Log.d(TAG, "Safety state changed: " + oldState + " → " + state);
            notifyStateChange("safetyState", oldState, state);
        }
    }

    /**
     * 获取安全状态
     */
    public synchronized SafetyState getSafetyState() {
        return safetyState;
    }

    /**
     * 获取最后更新时间
     */
    public synchronized long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * 获取所有系统状态
     */
    public synchronized Map<String, Object> getAllStates() {
        Map<String, Object> states = new HashMap<>();
        states.put("controlState", controlState);
        states.put("currentLayout", currentLayout);
        states.put("rtt", rtt);
        states.put("packetLossRate", packetLossRate);
        states.put("safetyState", safetyState);
        states.put("lastUpdateTime", lastUpdateTime);
        return states;
    }

    /**
     * 注册状态变更监听器
     */
    public synchronized void registerStateChangeListener(String listenerId, StateChangeListener listener) {
        stateChangeListeners.put(listenerId, listener);
        Log.d(TAG, "State change listener registered: " + listenerId);
    }

    /**
     * 注销状态变更监听器
     */
    public synchronized void unregisterStateChangeListener(String listenerId) {
        stateChangeListeners.remove(listenerId);
        Log.d(TAG, "State change listener unregistered: " + listenerId);
    }

    /**
     * 通知状态变更
     */
    private synchronized void notifyStateChange(String stateName, Object oldValue, Object newValue) {
        for (StateChangeListener listener : stateChangeListeners.values()) {
            try {
                listener.onStateChanged(stateName, oldValue, newValue);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying state change listener", e);
            }
        }
    }

    /**
     * 状态变更监听器接口
     */
    public interface StateChangeListener {
        /**
         * 状态变更回调
         * @param stateName 状态名称
         * @param oldValue 旧值
         * @param newValue 新值
         */
        void onStateChanged(String stateName, Object oldValue, Object newValue);
    }
}