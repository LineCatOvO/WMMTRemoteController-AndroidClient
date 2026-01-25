package com.linecat.wmmtcontroller.model;

import com.linecat.wmmtcontroller.control.operation.ControlAction;

/**
 * 设备映射类
 * 定义输入如何映射到输出设备
 */
public class DeviceMapping {
    private float steering; // 转向值
    private float throttle; // 油门值
    private float brake;    // 制动值
    private float gyroX;    // 陀螺仪X轴
    private float gyroY;    // 陀螺仪Y轴
    private float gyroZ;    // 陀螺仪Z轴
    private Boolean buttonA;
    private Boolean buttonB;
    private Boolean buttonX;
    private Boolean buttonY;
    private Boolean shoulderL;
    private Boolean shoulderR;
    private float triggerL;
    private float triggerR;
    private float mouseX;
    private float mouseY;
    private Boolean mouseLeft;
    private Boolean mouseRight;
    private Boolean[] keys; // 键盘按键数组

    public DeviceMapping() {
        // 初始化默认值
        this.steering = 0.0f;
        this.throttle = 0.0f;
        this.brake = 0.0f;
        this.gyroX = 0.0f;
        this.gyroY = 0.0f;
        this.gyroZ = 0.0f;
        this.buttonA = false;
        this.buttonB = false;
        this.buttonX = false;
        this.buttonY = false;
        this.shoulderL = false;
        this.shoulderR = false;
        this.triggerL = 0.0f;
        this.triggerR = 0.0f;
        this.mouseX = 0.0f;
        this.mouseY = 0.0f;
        this.mouseLeft = false;
        this.mouseRight = false;
        this.keys = new Boolean[256];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = false;
        }
    }

    // 从 ControlAction 创建 DeviceMapping
    public static DeviceMapping fromControlAction(ControlAction action) {
        DeviceMapping mapping = new DeviceMapping();
        if (action != null) {
            // 根据操作类型设置相应的值
            String operationType = action.getOperationType();
            float value = action.getProcessedValue();
            boolean pressed = action.isPressed();
            
            switch (operationType.toLowerCase()) {
                case "steering":
                    mapping.setSteering(value);
                    break;
                case "throttle":
                    mapping.setThrottle(value);
                    break;
                case "brake":
                    mapping.setBrake(value);
                    break;
                case "gyro_x":
                    mapping.setGyroX(value);
                    break;
                case "gyro_y":
                    mapping.setGyroY(value);
                    break;
                case "gyro_z":
                    mapping.setGyroZ(value);
                    break;
                case "button_a":
                    mapping.setButtonA(pressed);
                    break;
                case "button_b":
                    mapping.setButtonB(pressed);
                    break;
                case "button_x":
                    mapping.setButtonX(pressed);
                    break;
                case "button_y":
                    mapping.setButtonY(pressed);
                    break;
                case "shoulder_l":
                    mapping.setShoulderL(pressed);
                    break;
                case "shoulder_r":
                    mapping.setShoulderR(pressed);
                    break;
                case "trigger_l":
                    mapping.setTriggerL(value);
                    break;
                case "trigger_r":
                    mapping.setTriggerR(value);
                    break;
                default:
                    // 对于其他操作类型，可以根据需要进行处理
                    break;
            }
        }
        return mapping;
    }

    // Getters and Setters
    public float getSteering() { return steering; }
    public void setSteering(float steering) { this.steering = steering; }

    public float getThrottle() { return throttle; }
    public void setThrottle(float throttle) { this.throttle = throttle; }

    public float getBrake() { return brake; }
    public void setBrake(float brake) { this.brake = brake; }

    public float getGyroX() { return gyroX; }
    public void setGyroX(float gyroX) { this.gyroX = gyroX; }

    public float getGyroY() { return gyroY; }
    public void setGyroY(float gyroY) { this.gyroY = gyroY; }

    public float getGyroZ() { return gyroZ; }
    public void setGyroZ(float gyroZ) { this.gyroZ = gyroZ; }

    public Boolean getButtonA() { return buttonA; }
    public void setButtonA(Boolean buttonA) { this.buttonA = buttonA; }

    public Boolean getButtonB() { return buttonB; }
    public void setButtonB(Boolean buttonB) { this.buttonB = buttonB; }

    public Boolean getButtonX() { return buttonX; }
    public void setButtonX(Boolean buttonX) { this.buttonX = buttonX; }

    public Boolean getButtonY() { return buttonY; }
    public void setButtonY(Boolean buttonY) { this.buttonY = buttonY; }

    public Boolean getShoulderL() { return shoulderL; }
    public void setShoulderL(Boolean shoulderL) { this.shoulderL = shoulderL; }

    public Boolean getShoulderR() { return shoulderR; }
    public void setShoulderR(Boolean shoulderR) { this.shoulderR = shoulderR; }

    public float getTriggerL() { return triggerL; }
    public void setTriggerL(float triggerL) { this.triggerL = triggerL; }

    public float getTriggerR() { return triggerR; }
    public void setTriggerR(float triggerR) { this.triggerR = triggerR; }

    public float getMouseX() { return mouseX; }
    public void setMouseX(float mouseX) { this.mouseX = mouseX; }

    public float getMouseY() { return mouseY; }
    public void setMouseY(float mouseY) { this.mouseY = mouseY; }

    public Boolean getMouseLeft() { return mouseLeft; }
    public void setMouseLeft(Boolean mouseLeft) { this.mouseLeft = mouseLeft; }

    public Boolean getMouseRight() { return mouseRight; }
    public void setMouseRight(Boolean mouseRight) { this.mouseRight = mouseRight; }

    public Boolean[] getKeys() { return keys; }
    public void setKeys(Boolean[] keys) { 
        if (keys != null) {
            this.keys = keys;
        }
    }
    
    public boolean getKey(int index) {
        if (index >= 0 && index < keys.length) {
            return keys[index];
        }
        return false;
    }
    
    public void setKey(int index, boolean value) {
        if (index >= 0 && index < keys.length) {
            keys[index] = value;
        }
    }
}