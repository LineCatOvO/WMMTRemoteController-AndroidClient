package com.linecat.wmmtcontroller.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 原始输入数据模型
 * 包含从传感器采集的原始数据
 */
public class RawInput {
    // 陀螺仪数据（角度）
    private float gyroPitch;
    private float gyroRoll;
    private float gyroYaw;
    
    // 加速度计数据（m/s²）
    private float accelX;
    private float accelY;
    private float accelZ;

    // 触摸数据
    private boolean touchPressed;
    private float touchX;
    private float touchY;

    // 按键数据
    private boolean buttonA;
    private boolean buttonB;
    private boolean buttonC;
    private boolean buttonD;
    
    // 游戏手柄数据
    private GamepadData gamepad;
    
    // 构造函数
    public RawInput() {
        this.gamepad = new GamepadData();
    }
    
    // Copy构造函数
    public RawInput(RawInput other) {
        this.gyroPitch = other.gyroPitch;
        this.gyroRoll = other.gyroRoll;
        this.gyroYaw = other.gyroYaw;
        this.accelX = other.accelX;
        this.accelY = other.accelY;
        this.accelZ = other.accelZ;
        this.touchPressed = other.touchPressed;
        this.touchX = other.touchX;
        this.touchY = other.touchY;
        this.buttonA = other.buttonA;
        this.buttonB = other.buttonB;
        this.buttonC = other.buttonC;
        this.buttonD = other.buttonD;
        this.gamepad = new GamepadData();
        this.gamepad.getAxes().putAll(other.gamepad.getAxes());
        this.gamepad.getButtons().putAll(other.gamepad.getButtons());
    }

    // 陀螺仪数据 getter/setter
    public float getGyroPitch() {
        return gyroPitch;
    }

    public void setGyroPitch(float gyroPitch) {
        this.gyroPitch = gyroPitch;
    }

    public float getGyroRoll() {
        return gyroRoll;
    }

    public void setGyroRoll(float gyroRoll) {
        this.gyroRoll = gyroRoll;
    }

    public float getGyroYaw() {
        return gyroYaw;
    }

    public void setGyroYaw(float gyroYaw) {
        this.gyroYaw = gyroYaw;
    }
    
    // 为了兼容性，提供X/Y/Z形式的陀螺仪方法
    public float getGyroX() {
        return gyroRoll; // Roll 对应 X 轴旋转
    }
    
    public float getGyroY() {
        return gyroPitch; // Pitch 对应 Y 轴旋转
    }
    
    public float getGyroZ() {
        return gyroYaw; // Yaw 对应 Z 轴旋转
    }
    
    // 加速度计数据 getter/setter
    public float getAccelX() {
        return accelX;
    }
    
    public void setAccelX(float accelX) {
        this.accelX = accelX;
    }
    
    public float getAccelY() {
        return accelY;
    }
    
    public void setAccelY(float accelY) {
        this.accelY = accelY;
    }
    
    public float getAccelZ() {
        return accelZ;
    }
    
    public void setAccelZ(float accelZ) {
        this.accelZ = accelZ;
    }

    // 触摸数据 getter/setter
    public boolean isTouchPressed() {
        return touchPressed;
    }

    public void setTouchPressed(boolean touchPressed) {
        this.touchPressed = touchPressed;
    }

    public float getTouchX() {
        return touchX;
    }

    public void setTouchX(float touchX) {
        this.touchX = touchX;
    }

    public float getTouchY() {
        return touchY;
    }

    public void setTouchY(float touchY) {
        this.touchY = touchY;
    }

    // 按键数据 getter/setter
    public boolean isButtonA() {
        return buttonA;
    }

    public void setButtonA(boolean buttonA) {
        this.buttonA = buttonA;
    }

    public boolean isButtonB() {
        return buttonB;
    }

    public void setButtonB(boolean buttonB) {
        this.buttonB = buttonB;
    }

    public boolean isButtonC() {
        return buttonC;
    }

    public void setButtonC(boolean buttonC) {
        this.buttonC = buttonC;
    }

    public boolean isButtonD() {
        return buttonD;
    }

    public void setButtonD(boolean buttonD) {
        this.buttonD = buttonD;
    }
    
    // 游戏手柄数据 getter/setter
    public GamepadData getGamepad() {
        return gamepad;
    }
    
    public void setGamepad(GamepadData gamepad) {
        this.gamepad = gamepad;
    }

    @Override
    public String toString() {
        return "RawInput{" +
                "gyroPitch=" + gyroPitch +
                ", gyroRoll=" + gyroRoll +
                ", gyroYaw=" + gyroYaw +
                ", accelX=" + accelX +
                ", accelY=" + accelY +
                ", accelZ=" + accelZ +
                ", touchPressed=" + touchPressed +
                ", touchX=" + touchX +
                ", touchY=" + touchY +
                ", buttonA=" + buttonA +
                ", buttonB=" + buttonB +
                ", buttonC=" + buttonC +
                ", buttonD=" + buttonD +
                ", gamepad=" + gamepad +
                '}';
    }
    
    /**
     * 游戏手柄数据类
     * 包含游戏手柄的按键和摇杆数据
     */
    public static class GamepadData {
        // 摇杆轴数据（-1.0 到 1.0）
        private Map<String, Float> axes;
        
        // 按键状态
        private Map<String, Boolean> buttons;
        
        // 构造函数
        public GamepadData() {
            this.axes = new HashMap<>();
            this.buttons = new HashMap<>();
        }
        
        /**
         * 获取摇杆轴数据
         * @return 摇杆轴数据映射
         */
        public Map<String, Float> getAxes() {
            return axes;
        }
        
        /**
         * 设置摇杆轴数据
         * @param axes 摇杆轴数据映射
         */
        public void setAxes(Map<String, Float> axes) {
            this.axes = axes;
        }
        
        /**
         * 获取指定轴的值
         * @param axisName 轴名称
         * @return 轴值，默认为 0.0
         */
        public float getAxis(String axisName) {
            return axes.getOrDefault(axisName, 0.0f);
        }
        
        /**
         * 设置指定轴的值
         * @param axisName 轴名称
         * @param value 轴值
         */
        public void setAxis(String axisName, float value) {
            axes.put(axisName, value);
        }
        
        /**
         * 获取按键状态
         * @return 按键状态映射
         */
        public Map<String, Boolean> getButtons() {
            return buttons;
        }
        
        /**
         * 设置按键状态
         * @param buttons 按键状态映射
         */
        public void setButtons(Map<String, Boolean> buttons) {
            this.buttons = buttons;
        }
        
        /**
         * 获取指定按键的状态
         * @param buttonName 按键名称
         * @return 按键状态，默认为 false
         */
        public boolean getButton(String buttonName) {
            return buttons.getOrDefault(buttonName, false);
        }
        
        /**
         * 设置指定按键的状态
         * @param buttonName 按键名称
         * @param pressed 按键状态
         */
        public void setButton(String buttonName, boolean pressed) {
            buttons.put(buttonName, pressed);
        }
        
        @Override
        public String toString() {
            return "GamepadData{" +
                    "axes=" + axes +
                    ", buttons=" + buttons +
                    '}';
        }
    }
}