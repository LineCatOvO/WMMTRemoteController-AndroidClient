package com.linecat.wmmtcontroller.control.mapping;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.control.operation.ControlAction;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 设备映射类 (Device Mapping)
 * 
 * 对应三层架构中的 Mapping 层，负责：
 * 1. 将抽象语义映射到设备输出
 * 2. 执行设备适配
 * 3. 将控制动作映射到具体的设备控制指令
 * 
 * 这是输入处理的第三层，主要负责将抽象的操作意图转换为具体设备可以理解的指令
 */
public class DeviceMapping {
    
    private String mappingId;
    private String mappingName;
    private MappingType mappingType;
    
    // 映射配置
    private Map<String, String> operationToKeyMap;  // 操作类型到按键的映射
    private Map<String, String> operationToAxisMap; // 操作类型到轴的映射
    private Map<String, String> operationToButtonMap; // 操作类型到按钮的映射
    
    // 设备特定配置
    private String targetDevice;  // 目标设备类型
    private Map<String, Object> deviceSpecificParams; // 设备特定参数
    
    public enum MappingType {
        KEYBOARD,    // 键盘映射
        GAMEPAD,     // 游戏手柄映射
        MOUSE,       // 鼠标映射
        CUSTOM       // 自定义映射
    }
    
    public DeviceMapping(String mappingId, String mappingName, MappingType mappingType) {
        this.mappingId = mappingId;
        this.mappingName = mappingName;
        this.mappingType = mappingType;
        
        this.operationToKeyMap = new HashMap<>();
        this.operationToAxisMap = new HashMap<>();
        this.operationToButtonMap = new HashMap<>();
        this.deviceSpecificParams = new HashMap<>();
        
        // 设置默认目标设备
        switch (mappingType) {
            case KEYBOARD:
                this.targetDevice = "keyboard";
                break;
            case GAMEPAD:
                this.targetDevice = "gamepad";
                break;
            case MOUSE:
                this.targetDevice = "mouse";
                break;
            default:
                this.targetDevice = "custom";
                break;
        }
    }
    
    /**
     * 添加操作到按键的映射
     * @param operationType 操作类型
     * @param keyCode 按键码
     */
    public void addOperationToKeyMapping(String operationType, String keyCode) {
        operationToKeyMap.put(operationType, keyCode);
    }
    
    /**
     * 添加操作到轴的映射
     * @param operationType 操作类型
     * @param axisName 轴名称
     */
    public void addOperationToAxisMapping(String operationType, String axisName) {
        operationToAxisMap.put(operationType, axisName);
    }
    
    /**
     * 添加操作到按钮的映射
     * @param operationType 操作类型
     * @param buttonName 按钮名称
     */
    public void addOperationToButtonMapping(String operationType, String buttonName) {
        operationToButtonMap.put(operationType, buttonName);
    }
    
    /**
     * 移除操作映射
     * @param operationType 操作类型
     */
    public void removeOperationMapping(String operationType) {
        operationToKeyMap.remove(operationType);
        operationToAxisMap.remove(operationType);
        operationToButtonMap.remove(operationType);
    }
    
    /**
     * 应用控制动作到输入状态
     * @param action 控制动作
     * @param inputState 输入状态
     */
    public void applyActionToState(ControlAction action, InputState inputState) {
        if (action == null || inputState == null) {
            return;
        }
        
        String operationType = action.getOperationType();
        
        // 根据操作类型和映射配置更新输入状态
        switch (action.getActionType()) {
            case ANALOG:
                handleAnalogAction(operationType, action, inputState);
                break;
            case DIGITAL:
                handleDigitalAction(operationType, action, inputState);
                break;
            case GESTURE:
                handleGestureAction(operationType, action, inputState);
                break;
            case COMPOSITE:
                handleCompositeAction(operationType, action, inputState);
                break;
        }
    }
    
    /**
     * 处理模拟动作
     * @param operationType 操作类型
     * @param action 控制动作
     * @param inputState 输入状态
     */
    private void handleAnalogAction(String operationType, ControlAction action, InputState inputState) {
        // 检查是否存在轴映射
        if (operationToAxisMap.containsKey(operationType)) {
            String axisName = operationToAxisMap.get(operationType);
            float value = action.getProcessedValue();
            
            // 根据轴名称设置相应的输入状态值
            switch (axisName.toLowerCase()) {
                case "steering":
                case "x_axis":
                case "lx":
                    inputState.setSteering(value);
                    break;
                case "throttle":
                case "y_axis":
                case "ly":
                    inputState.setThrottle(value);
                    break;
                case "brake":
                case "rx":
                    inputState.setBrake(value);
                    break;
                case "gyro_x":
                    inputState.setGyroX(value);
                    break;
                case "gyro_y":
                    inputState.setGyroY(value);
                    break;
                case "gyro_z":
                    inputState.setGyroZ(value);
                    break;
                default:
                    // 对于未知轴，可以考虑添加到自定义轴映射中
                    break;
            }
        }
    }
    
    /**
     * 处理数字动作
     * @param operationType 操作类型
     * @param action 控制动作
     * @param inputState 输入状态
     */
    private void handleDigitalAction(String operationType, ControlAction action, InputState inputState) {
        boolean pressed = action.isPressed();
        
        // 检查是否存在按键映射
        if (operationToKeyMap.containsKey(operationType)) {
            String keyCode = operationToKeyMap.get(operationType);
            
            // 将操作类型映射到具体的键盘按键
            switch (keyCode.toUpperCase()) {
                case "KEY_W":
                    if (pressed) inputState.getKeys().add("KeyW");
                    else inputState.getKeys().remove("KeyW");
                    break;
                case "KEY_A":
                    if (pressed) inputState.getKeys().add("KeyA");
                    else inputState.getKeys().remove("KeyA");
                    break;
                case "KEY_S":
                    if (pressed) inputState.getKeys().add("KeyS");
                    else inputState.getKeys().remove("KeyS");
                    break;
                case "KEY_D":
                    if (pressed) inputState.getKeys().add("KeyD");
                    else inputState.getKeys().remove("KeyD");
                    break;
                case "SPACE":
                    if (pressed) inputState.getKeys().add("Space");
                    else inputState.getKeys().remove("Space");
                    break;
                case "ENTER":
                    if (pressed) inputState.getKeys().add("Enter");
                    else inputState.getKeys().remove("Enter");
                    break;
                case "SHIFT":
                    if (pressed) inputState.getKeys().add("ShiftLeft");
                    else inputState.getKeys().remove("ShiftLeft");
                    break;
                case "CTRL":
                    if (pressed) inputState.getKeys().add("ControlLeft");
                    else inputState.getKeys().remove("ControlLeft");
                    break;
                case "ALT":
                    if (pressed) inputState.getKeys().add("AltLeft");
                    else inputState.getKeys().remove("AltLeft");
                    break;
                default:
                    // 对于其他按键，可以直接添加到按键列表
                    if (pressed) inputState.getKeys().add(keyCode);
                    else inputState.getKeys().remove(keyCode);
                    break;
            }
        }
        
        // 检查是否存在按钮映射
        if (operationToButtonMap.containsKey(operationType)) {
            String buttonName = operationToButtonMap.get(operationType);
            // 可以在这里处理游戏手柄按钮或其他类型的按钮
            switch (buttonName.toLowerCase()) {
                case "a":
                case "button_a":
                    inputState.setButtonA(pressed);
                    break;
                case "b":
                case "button_b":
                    inputState.setButtonB(pressed);
                    break;
                case "x":
                case "button_x":
                    inputState.setButtonX(pressed);
                    break;
                case "y":
                case "button_y":
                    inputState.setButtonY(pressed);
                    break;
                case "lb":
                case "button_lb":
                    inputState.setShoulderL(pressed);
                    break;
                case "rb":
                case "button_rb":
                    inputState.setShoulderR(pressed);
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * 处理手势动作
     * @param operationType 操作类型
     * @param action 控制动作
     * @param inputState 输入状态
     */
    private void handleGestureAction(String operationType, ControlAction action, InputState inputState) {
        // 手势通常会产生复合效果，可能同时影响多个输入状态
        float value = action.getProcessedValue();
        float secondaryValue = action.getProcessedSecondaryValue();
        
        // 示例：根据手势类型设置相应的输入状态
        switch (operationType.toLowerCase()) {
            case "swipe_left_right":
                inputState.setSteering(value);
                break;
            case "swipe_up_down":
                inputState.setThrottle(value);
                break;
            case "two_finger_swipe":
                inputState.setGyroX(value);
                inputState.setGyroY(secondaryValue);
                break;
            default:
                // 可能需要扩展以支持更多手势类型
                break;
        }
    }
    
    /**
     * 处理复合动作
     * @param operationType 操作类型
     * @param action 控制动作
     * @param inputState 输入状态
     */
    private void handleCompositeAction(String operationType, ControlAction action, InputState inputState) {
        // 复合动作可能需要同时更新多个输入状态属性
        float value = action.getProcessedValue();
        
        // 示例：复合操作可能同时影响多个轴
        if (operationType.contains("steering_and_throttle")) {
            inputState.setSteering(value);
            inputState.setThrottle(Math.abs(value) > 0.5f ? value : 0); // 只有当值较大时才影响油门
        } else if (operationType.contains("combined_brake")) {
            inputState.setBrake(Math.abs(value));
        }
    }
    
    /**
     * 批量应用控制动作到输入状态
     * @param actions 控制动作列表
     * @param inputState 输入状态
     */
    public void applyActionsToState(List<ControlAction> actions, InputState inputState) {
        if (actions == null || inputState == null) {
            return;
        }
        
        // 清除之前的状态（可选，取决于具体需求）
        // clearPreviousState(inputState);
        
        for (ControlAction action : actions) {
            applyActionToState(action, inputState);
        }
    }
    
    /**
     * 清除输入状态的先前值
     * @param inputState 输入状态
     */
    public void clearPreviousState(InputState inputState) {
        // 根据映射类型清除相应的状态
        switch (mappingType) {
            case KEYBOARD:
                inputState.getKeys().clear();
                break;
            case GAMEPAD:
                // 清除游戏手柄相关状态
                inputState.setButtonA(false);
                inputState.setButtonB(false);
                inputState.setButtonX(false);
                inputState.setButtonY(false);
                inputState.setShoulderL(false);
                inputState.setShoulderR(false);
                inputState.setTriggerL(0.0f);
                inputState.setTriggerR(0.0f);
                inputState.setSteering(0.0f);
                inputState.setThrottle(0.0f);
                inputState.setBrake(0.0f);
                break;
            case MOUSE:
                // 清除鼠标相关状态
                inputState.setMouseX(0.0f);
                inputState.setMouseY(0.0f);
                inputState.setMouseLeft(false);
                inputState.setMouseRight(false);
                break;
            case CUSTOM:
                // 清除自定义状态
                inputState.setSteering(0.0f);
                inputState.setThrottle(0.0f);
                inputState.setBrake(0.0f);
                inputState.setGyroX(0.0f);
                inputState.setGyroY(0.0f);
                inputState.setGyroZ(0.0f);
                break;
        }
    }
    
    // Getters and Setters
    public String getMappingId() {
        return mappingId;
    }
    
    public String getMappingName() {
        return mappingName;
    }
    
    public MappingType getMappingType() {
        return mappingType;
    }
    
    public String getTargetDevice() {
        return targetDevice;
    }
    
    public void setTargetDevice(String targetDevice) {
        this.targetDevice = targetDevice;
    }
    
    public Map<String, String> getOperationToKeyMap() {
        return operationToKeyMap;
    }
    
    public Map<String, String> getOperationToAxisMap() {
        return operationToAxisMap;
    }
    
    public Map<String, String> getOperationToButtonMap() {
        return operationToButtonMap;
    }
    
    public Map<String, Object> getDeviceSpecificParams() {
        return deviceSpecificParams;
    }
    
    public void setDeviceSpecificParams(Map<String, Object> deviceSpecificParams) {
        this.deviceSpecificParams = deviceSpecificParams;
    }
    
    /**
     * 获取映射配置的摘要信息
     * @return 映射配置摘要
     */
    public String getMappingSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceMapping{");
        sb.append("mappingId='").append(mappingId).append('\'');
        sb.append(", mappingType=").append(mappingType);
        sb.append(", targetDevice='").append(targetDevice).append('\'');
        sb.append(", keyMappings=").append(operationToKeyMap.size());
        sb.append(", axisMappings=").append(operationToAxisMap.size());
        sb.append(", buttonMappings=").append(operationToButtonMap.size());
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getMappingSummary();
    }
}