package com.linecat.wmmtcontroller.control.operation;

/**
 * 控制动作类
 * 定义从控制节点输出的控制动作
 */
public class ControlAction {
    
    public enum ActionType {
        DIGITAL,    // 数字信号（开关）
        ANALOG,     // 模拟信号（范围值）
        GYRO,       // 陀螺仪信号
        GESTURE,    // 手势信号
        COMPOSITE   // 复合信号
    }
    
    private String actionId;
    private ActionType type;
    private String operationType;  // 对应的操作类型（如steering, throttle等）
    private float value;           // 动作值（0.0-1.0）
    private boolean digitalValue;  // 数字值（true/false）
    private long timestamp;        // 时间戳
    private float secondaryValue;  // 次值，用于复杂操作

    public ControlAction(String actionId, ActionType type, String operationType) {
        this.actionId = actionId;
        this.type = type;
        this.operationType = operationType;
        this.value = 0.0f;
        this.digitalValue = false;
        this.secondaryValue = 0.0f;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 更新动作状态
     * @param value 模拟值 (0.0-1.0)
     * @param digitalValue 数字值 (true/false)
     */
    public void updateState(float value, boolean digitalValue) {
        this.value = value;
        this.digitalValue = digitalValue;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 更新动作状态（带额外的第二个值）
     * @param primaryValue 主值
     * @param secondaryValue 次值
     * @param digitalValue 数字值
     */
    public void updateState(float primaryValue, float secondaryValue, boolean digitalValue) {
        // 这里可以将primaryValue作为主要值，或者取两者的某种组合
        this.value = primaryValue; // 使用主值
        this.secondaryValue = secondaryValue;
        this.digitalValue = digitalValue;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getActionId() {
        return actionId;
    }

    public ActionType getType() {
        return type;
    }
    
    public ActionType getActionType() {
        return type;
    }

    public String getOperationType() {
        return operationType;
    }

    public float getValue() {
        return value;
    }
    
    public float getProcessedValue() {
        return value;
    }
    
    public float getProcessedSecondaryValue() {
        return secondaryValue;
    }

    public boolean getDigitalValue() {
        return digitalValue;
    }
    
    public boolean isPressed() {
        return digitalValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setValue(float value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    public void setDigitalValue(boolean digitalValue) {
        this.digitalValue = digitalValue;
        this.timestamp = System.currentTimeMillis();
    }
    
    public float getSecondaryValue() {
        return secondaryValue;
    }
    
    public void setSecondaryValue(float secondaryValue) {
        this.secondaryValue = secondaryValue;
    }
}