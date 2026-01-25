package com.linecat.wmmtcontroller.control;

/**
 * 控制动作类
 * 定义各种控制输入的动作
 */
public class ControlAction {
    // 按键相关
    private Boolean buttonA;
    private Boolean buttonB;
    private Boolean buttonX;
    private Boolean buttonY;
    private Boolean shoulderL;
    private Boolean shoulderR;
    private Boolean triggerL;
    private Boolean triggerR;
    
    // 方向控制
    private Float steering; // 转向
    private Float throttle; // 油门
    private Float brake;    // 制动
    
    // 鼠标控制
    private Float mouseX;
    private Float mouseY;
    private Boolean mouseLeft;
    private Boolean mouseRight;
    
    // 陀螺仪控制
    private Float gyroX;
    private Float gyroY;
    private Float gyroZ;
    
    // 键盘按键
    private Boolean[] keys; // 数组存储键盘按键状态

    public ControlAction() {
        // 初始化默认值
        keys = new Boolean[256]; // 假设支持256个按键
        for (int i = 0; i < keys.length; i++) {
            keys[i] = false;
        }
    }

    // Getters and Setters
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

    public Boolean getTriggerL() { return triggerL; }
    public void setTriggerL(Boolean triggerL) { this.triggerL = triggerL; }

    public Boolean getTriggerR() { return triggerR; }
    public void setTriggerR(Boolean triggerR) { this.triggerR = triggerR; }

    public Float getSteering() { return steering; }
    public void setSteering(Float steering) { this.steering = steering; }

    public Float getThrottle() { return throttle; }
    public void setThrottle(Float throttle) { this.throttle = throttle; }

    public Float getBrake() { return brake; }
    public void setBrake(Float brake) { this.brake = brake; }

    public Float getMouseX() { return mouseX; }
    public void setMouseX(Float mouseX) { this.mouseX = mouseX; }

    public Float getMouseY() { return mouseY; }
    public void setMouseY(Float mouseY) { this.mouseY = mouseY; }

    public Boolean getMouseLeft() { return mouseLeft; }
    public void setMouseLeft(Boolean mouseLeft) { this.mouseLeft = mouseLeft; }

    public Boolean getMouseRight() { return mouseRight; }
    public void setMouseRight(Boolean mouseRight) { this.mouseRight = mouseRight; }

    public Float getGyroX() { return gyroX; }
    public void setGyroX(Float gyroX) { this.gyroX = gyroX; }

    public Float getGyroY() { return gyroY; }
    public void setGyroY(Float gyroY) { this.gyroY = gyroY; }

    public Float getGyroZ() { return gyroZ; }
    public void setGyroZ(Float gyroZ) { this.gyroZ = gyroZ; }

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