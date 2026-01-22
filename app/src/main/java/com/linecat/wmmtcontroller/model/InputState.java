package com.linecat.wmmtcontroller.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 最终输入状态模型
 * 与服务端定义的TypeScript类型匹配
 * 用于发送到服务端的最终输入状态
 */
public class InputState {
    private long frameId;
    private String runtimeStatus;
    private List<String> keyboard;
    private MouseState mouse;
    private JoystickState joystick;
    private GyroscopeState gyroscope;

    // 构造函数
    public InputState() {
        this.frameId = 0;
        this.runtimeStatus = "ok";
        this.keyboard = new ArrayList<>();
        this.mouse = new MouseState();
        this.joystick = new JoystickState();
        this.gyroscope = new GyroscopeState();
    }
    
    // Copy构造函数
    public InputState(InputState other) {
        this.frameId = other.frameId;
        this.runtimeStatus = other.runtimeStatus;
        this.keyboard = new ArrayList<>(other.keyboard);
        
        // 深拷贝内部类
        this.mouse = new MouseState();
        this.mouse.setX(other.mouse.getX());
        this.mouse.setY(other.mouse.getY());
        this.mouse.setLeft(other.mouse.isLeft());
        this.mouse.setRight(other.mouse.isRight());
        this.mouse.setMiddle(other.mouse.isMiddle());
        
        this.joystick = new JoystickState();
        this.joystick.setX(other.joystick.getX());
        this.joystick.setY(other.joystick.getY());
        this.joystick.setDeadzone(other.joystick.getDeadzone());
        this.joystick.setSmoothing(other.joystick.getSmoothing());
        
        this.gyroscope = new GyroscopeState();
        this.gyroscope.setPitch(other.gyroscope.getPitch());
        this.gyroscope.setRoll(other.gyroscope.getRoll());
        this.gyroscope.setYaw(other.gyroscope.getYaw());
        this.gyroscope.setDeadzone(other.gyroscope.getDeadzone());
        this.gyroscope.setSmoothing(other.gyroscope.getSmoothing());
    }

    // getter和setter
    public long getFrameId() {
        return frameId;
    }

    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }

    public String getRuntimeStatus() {
        return runtimeStatus;
    }

    public void setRuntimeStatus(String runtimeStatus) {
        this.runtimeStatus = runtimeStatus;
    }

    public List<String> getKeyboard() {
        return keyboard;
    }

    public void setKeyboard(List<String> keyboard) {
        this.keyboard = keyboard;
    }

    public MouseState getMouse() {
        return mouse;
    }

    public void setMouse(MouseState mouse) {
        this.mouse = mouse;
    }

    public JoystickState getJoystick() {
        return joystick;
    }

    public void setJoystick(JoystickState joystick) {
        this.joystick = joystick;
    }

    public GyroscopeState getGyroscope() {
        return gyroscope;
    }

    public void setGyroscope(GyroscopeState gyroscope) {
        this.gyroscope = gyroscope;
    }
    
    /**
     * 清除所有键盘按键
     * 用于防止粘键问题
     */
    public void clearAllKeys() {
        this.keyboard.clear();
    }

    // 内部类：鼠标状态
    public static class MouseState {
        private float x;
        private float y;
        private boolean left;
        private boolean right;
        private boolean middle;

        // getter和setter
        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public boolean isLeft() {
            return left;
        }

        public void setLeft(boolean left) {
            this.left = left;
        }

        public boolean isRight() {
            return right;
        }

        public void setRight(boolean right) {
            this.right = right;
        }

        public boolean isMiddle() {
            return middle;
        }

        public void setMiddle(boolean middle) {
            this.middle = middle;
        }
    }

    // 内部类：摇杆状态
    public static class JoystickState {
        private float x;
        private float y;
        private float deadzone;
        private float smoothing;

        // getter和setter
        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
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
    }

    // 内部类：陀螺仪状态
    public static class GyroscopeState {
        private float pitch;
        private float roll;
        private float yaw;
        private float deadzone;
        private float smoothing;

        // getter和setter
        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getRoll() {
            return roll;
        }

        public void setRoll(float roll) {
            this.roll = roll;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
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
    }

    @Override
    public String toString() {
        return "InputState{" +
                "frameId=" + frameId +
                ", runtimeStatus='" + runtimeStatus + '\'' +
                ", keyboard=" + keyboard +
                ", mouse=" + mouse +
                ", joystick=" + joystick +
                ", gyroscope=" + gyroscope +
                '}';
    }
}