package com.linecat.wmmtcontroller.control;

import java.util.Map;

/**
 * 控制动作类 (Control Action)
 * 
 * 对应三层架构中的 Operation 层，负责：
 * 1. 定义抽象控制语义（方向盘、油门、刹车、按钮类操作）
 * 2. 执行抽象控制语义处理
 * 3. 应用死区、平滑、曲线等处理算法
 * 
 * 这是输入处理的第二层，主要负责将控制节点的输出转换为抽象的操作意图
 */
public class ControlAction {
    
    private String actionId;
    private ActionType actionType;
    private String operationType; // 对应具体操作类型，如"STEERING", "THROTTLE", "BRAKE", "BUTTON_A"等
    
    // 动作参数
    private float value;           // 主要数值 (如轴值 -1.0 到 1.0)
    private float secondaryValue;  // 次要数值 (如另一个轴的值)
    private boolean pressed;       // 按钮按下状态
    private long timestamp;        // 时间戳
    
    // 处理参数
    private float deadzone;        // 死区
    private float smoothing;       // 平滑系数
    private float curveExponent;   // 曲线映射指数
    private float[] range;         // 输入范围
    private float[] outputRange;   // 输出范围
    
    public enum ActionType {
        ANALOG,    // 模拟动作（如摇杆、方向盘）
        DIGITAL,   // 数字动作（如按钮）
        GESTURE,   // 手势动作
        COMPOSITE  // 复合动作
    }
    
    public ControlAction(String actionId, ActionType actionType, String operationType) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.operationType = operationType;
        this.value = 0.0f;
        this.secondaryValue = 0.0f;
        this.pressed = false;
        this.timestamp = System.currentTimeMillis();
        
        // 默认处理参数
        this.deadzone = 0.0f;
        this.smoothing = 0.0f;
        this.curveExponent = 1.0f;
        this.range = new float[]{-1.0f, 1.0f};
        this.outputRange = new float[]{-1.0f, 1.0f};
    }
    
    /**
     * 执行动作处理，应用各种处理算法
     * @param inputValue 输入值
     * @return 处理后的值
     */
    public float processValue(float inputValue) {
        float result = inputValue;
        
        // 应用范围限制
        result = Math.max(range[0], Math.min(range[1], result));
        
        // 应用死区
        if (Math.abs(result) < deadzone) {
            result = 0.0f;
        } else {
            // 对于超过死区的值，重新映射到0-1范围
            if (result > 0) {
                result = (result - deadzone) / (1.0f - deadzone);
            } else {
                result = (result + deadzone) / (1.0f - deadzone);
            }
        }
        
        // 应用曲线映射
        if (curveExponent != 1.0f) {
            float sign = result >= 0 ? 1.0f : -1.0f;
            result = sign * (float) Math.pow(Math.abs(result), curveExponent);
        }
        
        // 应用输出范围映射
        if (outputRange[0] != -1.0f || outputRange[1] != 1.0f) {
            // 从 [-1, 1] 映射到指定输出范围
            result = outputRange[0] + (result + 1.0f) * (outputRange[1] - outputRange[0]) / 2.0f;
        }
        
        return result;
    }
    
    /**
     * 更新动作状态
     * @param newValue 新的值
     * @param newPressed 新的按下状态
     */
    public void updateState(float newValue, boolean newPressed) {
        this.value = processValue(newValue);
        this.pressed = newPressed;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 更新动作状态（双轴值）
     * @param primaryValue 主要值
     * @param secondaryValue 次要值
     * @param newPressed 新的按下状态
     */
    public void updateState(float primaryValue, float secondaryValue, boolean newPressed) {
        this.value = processValue(primaryValue);
        this.secondaryValue = processValue(secondaryValue);
        this.pressed = newPressed;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取处理后的动作值
     * @return 处理后的值
     */
    public float getProcessedValue() {
        return processValue(value);
    }
    
    /**
     * 获取处理后的次要动作值
     * @return 处理后的次要值
     */
    public float getProcessedSecondaryValue() {
        return processValue(secondaryValue);
    }
    
    // Getters and Setters
    public String getActionId() {
        return actionId;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public float getValue() {
        return value;
    }
    
    public void setValue(float value) {
        this.value = value;
    }
    
    public float getSecondaryValue() {
        return secondaryValue;
    }
    
    public void setSecondaryValue(float secondaryValue) {
        this.secondaryValue = secondaryValue;
    }
    
    public boolean isPressed() {
        return pressed;
    }
    
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public float getDeadzone() {
        return deadzone;
    }
    
    public void setDeadzone(float deadzone) {
        this.deadzone = deadzone;
    }
    
    public float getSmoothing() {
        return smoothing;
    }
    
    public void setSmoothing(float smoothing) {
        this.smoothing = smoothing;
    }
    
    public float getCurveExponent() {
        return curveExponent;
    }
    
    public void setCurveExponent(float curveExponent) {
        this.curveExponent = curveExponent;
    }
    
    public float[] getRange() {
        return range;
    }
    
    public void setRange(float[] range) {
        this.range = range;
    }
    
    public float[] getOutputRange() {
        return outputRange;
    }
    
    public void setOutputRange(float[] outputRange) {
        this.outputRange = outputRange;
    }
    
    /**
     * 将控制动作转换为映射参数
     * @return 包含动作参数的映射
     */
    public Map<String, Object> toMappingParams() {
        return new java.util.HashMap<String, Object>() {{
            put("operationType", operationType);
            put("value", getProcessedValue());
            put("secondaryValue", getProcessedSecondaryValue());
            put("pressed", isPressed());
            put("timestamp", getTimestamp());
        }};
    }
    
    @Override
    public String toString() {
        return "ControlAction{" +
                "actionId='" + actionId + '\'' +
                ", actionType=" + actionType +
                ", operationType='" + operationType + '\'' +
                ", value=" + value +
                ", secondaryValue=" + secondaryValue +
                ", pressed=" + pressed +
                ", timestamp=" + timestamp +
                '}';
    }
}