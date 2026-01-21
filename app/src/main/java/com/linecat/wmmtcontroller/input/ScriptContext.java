package com.linecat.wmmtcontroller.input;

import com.linecat.wmmtcontroller.annotation.Experimental;
import com.linecat.wmmtcontroller.annotation.Stable;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

import java.util.HashSet;
import java.util.Set;

/**
 * 脚本执行上下文
 * 作为Java代码和JavaScript运行时之间的桥梁
 */
@Stable(version = "1.0.0")
public class ScriptContext implements RawAccess, StateMutator, HostServices {
    
    // 原始输入数据
    private RawInput rawInput;
    
    // 输入状态（用于输出）
    private InputState inputState;
    
    // 键盘按键状态
    private Set<String> heldKeys;
    
    // 帧ID和时间戳
    private long frameId;
    private long timestamp;
    
    /**
     * 构造函数
     */
    public ScriptContext() {
        this.heldKeys = new HashSet<>();
    }
    
    /**
     * 初始化上下文
     * @param rawInput 原始输入数据
     * @param inputState 输入状态
     */
    public void init(RawInput rawInput, InputState inputState) {
        this.rawInput = rawInput;
        this.inputState = inputState;
        this.heldKeys.clear();
        this.frameId = 0;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 设置当前帧ID
     * @param frameId 帧ID
     */
    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }
    
    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取原始输入数据
     * @return 原始输入数据
     */
    @Stable
    public RawInput getRawInput() {
        return rawInput;
    }
    
    /**
     * 获取输入状态
     * @return 输入状态
     */
    @Stable
    public InputState getInputState() {
        return inputState;
    }
    
    /**
     * 按下并保持键盘按键
     * @param key 按键名称
     */
    @Stable
    public void holdKey(String key) {
        heldKeys.add(key);
    }
    
    /**
     * 释放键盘按键
     * @param key 按键名称
     */
    @Stable
    public void releaseKey(String key) {
        heldKeys.remove(key);
    }
    
    /**
     * 释放所有键盘按键
     */
    @Stable
    public void releaseAllKeys() {
        heldKeys.clear();
    }
    
    /**
     * 检查按键是否被按下
     * @param key 按键名称
     * @return 是否被按下
     */
    @Stable
    public boolean isKeyHeld(String key) {
        return heldKeys.contains(key);
    }
    
    /**
     * 应用当前按键状态到输入状态
     */
    @Stable
    public void applyKeyStates() {
        if (inputState != null) {
            inputState.getKeyboard().clear();
            inputState.getKeyboard().addAll(heldKeys);
        }
    }
    
    /**
     * 设置鼠标位置
     * @param x X坐标
     * @param y Y坐标
     */
    @Experimental
    public void setMousePosition(float x, float y) {
        if (inputState != null) {
            inputState.getMouse().setX(x);
            inputState.getMouse().setY(y);
        }
    }
    
    /**
     * 设置鼠标按键状态
     * @param button 鼠标按键名称（left, right, middle）
     * @param pressed 是否按下
     */
    @Experimental
    public void setMouseButton(String button, boolean pressed) {
        if (inputState != null) {
            switch (button.toLowerCase()) {
                case "left":
                    inputState.getMouse().setLeft(pressed);
                    break;
                case "right":
                    inputState.getMouse().setRight(pressed);
                    break;
                case "middle":
                    inputState.getMouse().setMiddle(pressed);
                    break;
            }
        }
    }
    
    /**
     * 获取摇杆轴值
     * @param axisName 轴名称
     * @return 轴值
     */
    @Stable
    public float getAxis(String axisName) {
        if (rawInput != null && rawInput.getGamepad() != null) {
            return rawInput.getGamepad().getAxis(axisName);
        }
        return 0.0f;
    }
    
    /**
     * 检查游戏手柄按钮是否被按下
     * @param buttonName 按钮名称
     * @return 是否被按下
     */
    @Stable
    public boolean isGamepadButtonPressed(String buttonName) {
        if (rawInput != null && rawInput.getGamepad() != null) {
            return rawInput.getGamepad().getButton(buttonName);
        }
        return false;
    }
    
    /**
     * 获取陀螺仪数据
     * @return 陀螺仪数据对象
     */
    @Experimental
    public GyroData getGyro() {
        if (rawInput != null) {
            return new GyroData(rawInput.getGyroPitch(), rawInput.getGyroRoll(), rawInput.getGyroYaw());
        }
        return new GyroData(0.0f, 0.0f, 0.0f);
    }
    
    /**
     * 获取触摸数据
     * @return 触摸数据对象
     */
    @Experimental
    public TouchData getTouch() {
        if (rawInput != null) {
            return new TouchData(rawInput.isTouchPressed(), rawInput.getTouchX(), rawInput.getTouchY());
        }
        return new TouchData(false, 0.0f, 0.0f);
    }
    
    // === RawAccess 接口实现 ===
    @Override
    public long getFrameId() {
        return frameId;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    // === StateMutator 接口实现 ===
    @Override
    public void pushEvent(String eventType, Object eventData) {
        // 事件推送实现，目前仅记录日志
        log("Event pushed: " + eventType + " - " + eventData);
    }
    
    // === HostServices 接口实现 ===
    @Override
    public void log(String message) {
        System.out.println("[SCRIPT] " + message);
    }
    
    @Override
    public void debug(String message) {
        System.out.println("[SCRIPT_DEBUG] " + message);
    }
    
    @Override
    public void error(String message, Throwable error) {
        System.err.println("[SCRIPT_ERROR] " + message);
        if (error != null) {
            error.printStackTrace(System.err);
        }
    }
    
    @Override
    public Object getProfileMetadata(String key) {
        // 目前返回null，后续可扩展为从profile中读取元信息
        return null;
    }
    
    @Override
    public boolean requestPermission(String permission) {
        // 目前返回false，后续可扩展为真正的权限请求
        return false;
    }
    
    /**
     * 陀螺仪数据类
     * 提供更友好的陀螺仪数据访问接口
     */
    public static class GyroData {
        private float pitch;
        private float roll;
        private float yaw;
        
        public GyroData(float pitch, float roll, float yaw) {
            this.pitch = pitch;
            this.roll = roll;
            this.yaw = yaw;
        }
        
        public float getPitch() { return pitch; }
        public float getRoll() { return roll; }
        public float getYaw() { return yaw; }
    }
    
    /**
     * 触摸数据类
     * 提供更友好的触摸数据访问接口
     */
    public static class TouchData {
        private boolean pressed;
        private float x;
        private float y;
        
        public TouchData(boolean pressed, float x, float y) {
            this.pressed = pressed;
            this.x = x;
            this.y = y;
        }
        
        public boolean isPressed() { return pressed; }
        public float getX() { return x; }
        public float getY() { return y; }
    }
}