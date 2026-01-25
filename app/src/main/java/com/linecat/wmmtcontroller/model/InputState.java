package com.linecat.wmmtcontroller.model;

import com.linecat.wmmtcontroller.control.ControlAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 输入状态类
 * 表示当前的输入状态
 */
public class InputState {
    // 按键状态
    private boolean buttonA = false;
    private boolean buttonB = false;
    private boolean buttonX = false;
    private boolean buttonY = false;
    private boolean shoulderL = false;
    private boolean shoulderR = false;
    private float triggerL = 0.0f;
    private float triggerR = 0.0f;

    // 鼠标状态
    private float mouseX = 0.0f;
    private float mouseY = 0.0f;
    private boolean mouseLeft = false;
    private boolean mouseRight = false;
    private boolean mouseMiddle = false;

    // 键盘按键状态
    private Set<String> keys = new HashSet<>();

    // 框架ID
    private long frameId = 0;

    // 运行时状态
    private String runtimeStatus = "";

    // 内部类定义
    public static class MouseState {
        private float x = 0.0f;
        private float y = 0.0f;
        private boolean left = false;
        private boolean right = false;
        private boolean middle = false;

        public float getX() { return x; }
        public void setX(float x) { this.x = x; }

        public float getY() { return y; }
        public void setY(float y) { this.y = y; }

        public boolean getLeft() { return left; }
        public boolean isLeft() { return left; }  // 添加 isLeft 方法
        public void setLeft(boolean left) { this.left = left; }

        public boolean getRight() { return right; }
        public boolean isRight() { return right; }  // 添加 isRight 方法
        public void setRight(boolean right) { this.right = right; }

        public boolean getMiddle() { return middle; }
        public boolean isMiddle() { return middle; }  // 添加 isMiddle 方法
        public void setMiddle(boolean middle) { this.middle = middle; }
    }

    public static class JoystickState {
        private float x = 0.0f;
        private float y = 0.0f;
        private float deadzone = 0.1f;
        private float smoothing = 0.0f;

        public JoystickState(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() { return x; }
        public void setX(float x) { this.x = x; }

        public float getY() { return y; }
        public void setY(float y) { this.y = y; }

        // 添加缺失的方法
        public float getDeadzone() { return deadzone; }
        public void setDeadzone(float deadzone) { this.deadzone = deadzone; }

        public float getSmoothing() { return smoothing; }
        public void setSmoothing(float smoothing) { this.smoothing = smoothing; }
    }

    private MouseState mouse = new MouseState();
    private Set<String> keyboard = new HashSet<>();
    private Set<String> gamepad = new HashSet<>(); // 添加游戏手柄字段
    private List<JoystickState> joysticks = new ArrayList<>();
    private JoystickState joystick = new JoystickState(0.0f, 0.0f); // 添加 joystick 字段

    public InputState() {
        // 初始化键盘按键状态
    }

    // 复制构造函数
    public InputState(InputState other) {
        if (other != null) {
            this.buttonA = other.buttonA;
            this.buttonB = other.buttonB;
            this.buttonX = other.buttonX;
            this.buttonY = other.buttonY;
            this.shoulderL = other.shoulderL;
            this.shoulderR = other.shoulderR;
            this.triggerL = other.triggerL;
            this.triggerR = other.triggerR;
            this.mouseX = other.mouseX;
            this.mouseY = other.mouseY;
            this.mouseLeft = other.mouseLeft;
            this.mouseRight = other.mouseRight;
            this.mouseMiddle = other.mouseMiddle;
            this.frameId = other.frameId;
            this.runtimeStatus = other.runtimeStatus != null ? other.runtimeStatus : "";

            if (other.keys != null) {
                this.keys.addAll(other.keys);
            }

            if (other.mouse != null) {
                this.mouse = new MouseState();
                this.mouse.setX(other.mouse.getX());
                this.mouse.setY(other.mouse.getY());
                this.mouse.setLeft(other.mouse.getLeft());
                this.mouse.setRight(other.mouse.getRight());
                this.mouse.setMiddle(other.mouse.getMiddle());
            }

            if (other.joystick != null) {
                this.joystick = new JoystickState(other.joystick.getX(), other.joystick.getY());
                this.joystick.setDeadzone(other.joystick.getDeadzone());
                this.joystick.setSmoothing(other.joystick.getSmoothing());
            }

            if (other.joysticks != null) {
                for (JoystickState js : other.joysticks) {
                    this.joysticks.add(new JoystickState(js.getX(), js.getY()));
                }
            }
        }
    }

    // 从 ControlAction 创建 InputState
    public static InputState fromControlAction(com.linecat.wmmtcontroller.control.operation.ControlAction action) {
        InputState state = new InputState();
        if (action != null) {
            // 这里可以根据需要实现从 ControlAction 到 InputState 的转换
        }
        return state;
    }

    // Getters and Setters
    public boolean getButtonA() { return buttonA; }
    public void setButtonA(boolean buttonA) { this.buttonA = buttonA; }

    public boolean getButtonB() { return buttonB; }
    public void setButtonB(boolean buttonB) { this.buttonB = buttonB; }

    public boolean getButtonX() { return buttonX; }
    public void setButtonX(boolean buttonX) { this.buttonX = buttonX; }

    public boolean getButtonY() { return buttonY; }
    public void setButtonY(boolean buttonY) { this.buttonY = buttonY; }

    public boolean getShoulderL() { return shoulderL; }
    public void setShoulderL(boolean shoulderL) { this.shoulderL = shoulderL; }

    public boolean getShoulderR() { return shoulderR; }
    public void setShoulderR(boolean shoulderR) { this.shoulderR = shoulderR; }

    // 触发器相关 getter/setter
    public float getTriggerL() { return triggerL; }
    public void setTriggerL(float triggerL) { this.triggerL = triggerL; }

    public float getTriggerR() { return triggerR; }
    public void setTriggerR(float triggerR) { this.triggerR = triggerR; }

    // 鼠标相关 getter/setter
    public float getMouseX() { return mouseX; }
    public void setMouseX(float mouseX) { this.mouseX = mouseX; }

    public float getMouseY() { return mouseY; }
    public void setMouseY(float mouseY) { this.mouseY = mouseY; }

    public boolean getMouseLeft() { return mouseLeft; }
    public void setMouseLeft(boolean mouseLeft) { this.mouseLeft = mouseLeft; }

    public boolean getMouseRight() { return mouseRight; }
    public void setMouseRight(boolean mouseRight) { this.mouseRight = mouseRight; }

    public boolean getMouseMiddle() { return mouseMiddle; }
    public void setMouseMiddle(boolean mouseMiddle) { this.mouseMiddle = mouseMiddle; }

    // 键盘按键相关 getter/setter
    public Set<String> getKeys() { return keys; }
    public void setKeys(Set<String> keys) {
        if (keys != null) {
            this.keys = keys;
        }
    }

    public void clearAllKeys() {
        keys.clear();
    }

    // 框架ID相关
    public long getFrameId() { return frameId; }
    public void setFrameId(long frameId) { this.frameId = frameId; }

    // 运行时状态相关
    public String getRuntimeStatus() { return runtimeStatus; }
    public void setRuntimeStatus(String runtimeStatus) { this.runtimeStatus = runtimeStatus; }

    // 新增方法
    public MouseState getMouse() { return mouse; }
    public void setMouse(MouseState mouse) { this.mouse = mouse; }

    public Set<String> getKeyboard() { return keyboard; }
    public void setKeyboard(Set<String> keyboard) { this.keyboard = keyboard; }

    public List<JoystickState> getJoysticks() { return joysticks; }
    public void setJoysticks(List<JoystickState> joysticks) { this.joysticks = joysticks; }

    public boolean isKeyPressed(String keycode) {
        return keyboard.contains(keycode);
    }

    public void pressKey(String keycode) {
        keyboard.add(keycode);
    }

    public void releaseKey(String keycode) {
        keyboard.remove(keycode);
    }

    public void clearKeys() {
        keyboard.clear();
    }

    public void addJoystick(JoystickState joystick) {
        joysticks.add(joystick);
    }

    // 添加缺失的 joystick 相关方法
    public JoystickState getJoystick() {
        return joystick;
    }

    public void setJoystick(JoystickState joystick) {
        this.joystick = joystick;
    }

    // 游戏手柄相关 getter/setter
    public Set<String> getGamepad() { return gamepad; }

    public void setGamepad(Set<String> gamepad) {
        if (gamepad != null) {
            this.gamepad = gamepad;
        }
    }

    public void addGamepadButton(String button) {
        gamepad.add(button);
    }

    public void removeGamepadButton(String button) {
        gamepad.remove(button);
    }

    public boolean isGamepadButtonPressed(String button) {
        return gamepad.contains(button);
    }

    public void clearGamepad() {
        gamepad.clear();
    }
}
