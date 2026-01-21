package com.linecat.wmmtcontroller.input;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 输入处理管线
 * 负责将原始输入转换为最终输入状态
 * 处理流程：RawInput → Filter → Smoothing → Deadzone → Curve → 语义分离 → 映射 → InputState
 */
public class InputPipeline {
    // 配置参数
    private float gyroDeadzone = 0.1f; // 陀螺仪死区
    private float gyroSmoothing = 0.5f; // 陀螺仪平滑系数
    private float joystickDeadzone = 0.1f; // 摇杆死区
    private float joystickSmoothing = 0.5f; // 摇杆平滑系数
    private float curveExponent = 2.0f; // 曲线映射指数

    // 上一次的输入状态，用于平滑处理
    private RawInput lastInput;
    
    // 键盘映射策略
    private KeyboardMapping keyboardMapping;

    /**
     * 构造函数
     */
    public InputPipeline() {
        this.keyboardMapping = new KeyboardMapping();
    }
    
    /**
     * 带自定义键盘映射的构造函数
     * @param keyboardMapping 自定义键盘映射
     */
    public InputPipeline(KeyboardMapping keyboardMapping) {
        this.keyboardMapping = keyboardMapping;
    }

    /**
     * 处理原始输入，生成最终输入状态
     * @param rawInput 原始输入
     * @return 最终输入状态
     */
    public InputState process(RawInput rawInput) {
        if (rawInput == null) {
            return new InputState();
        }

        // 1. 数据预处理阶段：Filter → Smoothing → Deadzone → Curve
        RawInput processedInput = preprocessInput(rawInput);
        
        // 2. 语义分离阶段：将处理后的输入分离为具体的游戏语义
        InputState inputState = new InputState();
        
        // 处理陀螺仪转向
        processSteeringInput(processedInput, inputState);
        
        // 处理按钮输入（油门、刹车等）
        processButtonInput(processedInput, inputState);
        
        // 处理触摸输入
        processTouchInput(processedInput, inputState);
        
        // 3. 设置原始传感器数据到输出状态
        setRawSensorData(processedInput, inputState);

        // 保存当前输入作为下一次的参考
        lastInput = rawInput;

        return inputState;
    }
    
    /**
     * 预处理输入数据
     * @param rawInput 原始输入
     * @return 预处理后的输入
     */
    private RawInput preprocessInput(RawInput rawInput) {
        // 1. Filter: 过滤原始数据
        RawInput filteredInput = filter(rawInput);

        // 2. Smoothing: 平滑处理
        RawInput smoothedInput = smooth(filteredInput);

        // 3. Deadzone: 应用死区
        RawInput deadzoneInput = applyDeadzone(smoothedInput);

        // 4. Curve: 曲线映射
        return applyCurve(deadzoneInput);
    }

    /**
     * 过滤原始数据
     * @param input 原始输入
     * @return 过滤后的输入
     */
    private RawInput filter(RawInput input) {
        // 这里可以添加过滤逻辑，如去除异常值
        RawInput filtered = new RawInput();
        // 复制所有属性
        filtered.setGyroPitch(clamp(input.getGyroPitch(), -180, 180));
        filtered.setGyroRoll(clamp(input.getGyroRoll(), -180, 180));
        filtered.setGyroYaw(clamp(input.getGyroYaw(), -180, 180));
        filtered.setTouchPressed(input.isTouchPressed());
        filtered.setTouchX(input.getTouchX());
        filtered.setTouchY(input.getTouchY());
        filtered.setButtonA(input.isButtonA());
        filtered.setButtonB(input.isButtonB());
        filtered.setButtonC(input.isButtonC());
        filtered.setButtonD(input.isButtonD());
        return filtered;
    }

    /**
     * 应用死区
     * @param input 输入数据
     * @return 应用死区后的数据
     */
    private RawInput applyDeadzone(RawInput input) {
        RawInput result = new RawInput();

        // 应用陀螺仪死区
        result.setGyroPitch(applyDeadzone(input.getGyroPitch(), gyroDeadzone));
        result.setGyroRoll(applyDeadzone(input.getGyroRoll(), gyroDeadzone));
        result.setGyroYaw(applyDeadzone(input.getGyroYaw(), gyroDeadzone));

        // 复制其他属性
        result.setTouchPressed(input.isTouchPressed());
        result.setTouchX(input.getTouchX());
        result.setTouchY(input.getTouchY());
        result.setButtonA(input.isButtonA());
        result.setButtonB(input.isButtonB());
        result.setButtonC(input.isButtonC());
        result.setButtonD(input.isButtonD());

        return result;
    }

    /**
     * 应用曲线映射
     * @param input 输入数据
     * @return 应用曲线映射后的数据
     */
    private RawInput applyCurve(RawInput input) {
        RawInput result = new RawInput();

        // 应用曲线映射，使输入更符合人体工学
        result.setGyroPitch(applyCurve(input.getGyroPitch(), curveExponent));
        result.setGyroRoll(applyCurve(input.getGyroRoll(), curveExponent));
        result.setGyroYaw(applyCurve(input.getGyroYaw(), curveExponent));

        // 复制其他属性
        result.setTouchPressed(input.isTouchPressed());
        result.setTouchX(input.getTouchX());
        result.setTouchY(input.getTouchY());
        result.setButtonA(input.isButtonA());
        result.setButtonB(input.isButtonB());
        result.setButtonC(input.isButtonC());
        result.setButtonD(input.isButtonD());

        return result;
    }

    /**
     * 平滑处理
     * @param input 当前输入
     * @return 平滑后的输入
     */
    private RawInput smooth(RawInput input) {
        RawInput result = new RawInput();

        if (lastInput == null) {
            // 第一次输入，直接返回
            return input;
        }

        // 应用平滑处理，使用指数移动平均
        result.setGyroPitch(lerp(lastInput.getGyroPitch(), input.getGyroPitch(), gyroSmoothing));
        result.setGyroRoll(lerp(lastInput.getGyroRoll(), input.getGyroRoll(), gyroSmoothing));
        result.setGyroYaw(lerp(lastInput.getGyroYaw(), input.getGyroYaw(), gyroSmoothing));

        // 复制其他属性（触摸和按钮不需要平滑）
        result.setTouchPressed(input.isTouchPressed());
        result.setTouchX(input.getTouchX());
        result.setTouchY(input.getTouchY());
        result.setButtonA(input.isButtonA());
        result.setButtonB(input.isButtonB());
        result.setButtonC(input.isButtonC());
        result.setButtonD(input.isButtonD());

        return result;
    }

    /**
     * 处理转向输入（陀螺仪）
     * 将陀螺仪数据映射为转向指令
     * @param input 处理后的输入
     * @param state 输入状态
     */
    private void processSteeringInput(RawInput input, InputState state) {
        float roll = input.getGyroRoll();

        if (roll > 0.5f) {
            state.getKeyboard().add(keyboardMapping.getRightKey()); // 右转向
        } else if (roll < -0.5f) {
            state.getKeyboard().add(keyboardMapping.getLeftKey()); // 左转向
        }
    }
    
    /**
     * 处理按钮输入
     * 将按钮状态映射为游戏指令（油门、刹车等）
     * @param input 处理后的输入
     * @param state 输入状态
     */
    private void processButtonInput(RawInput input, InputState state) {
        // 油门输入
        if (input.isButtonA()) {
            state.getKeyboard().add(keyboardMapping.getThrottleKey()); // 加速
        }
        
        // 刹车输入
        if (input.isButtonB()) {
            state.getKeyboard().add(keyboardMapping.getBrakeKey()); // 刹车
        }
        
        // 辅助按钮
        if (input.isButtonC()) {
            state.getKeyboard().add(keyboardMapping.getButtonCKey()); // 按钮C
        }
        
        if (input.isButtonD()) {
            state.getKeyboard().add(keyboardMapping.getButtonDKey()); // 按钮D
        }
    }
    
    /**
     * 处理触摸输入
     * 将触摸数据映射为鼠标指令
     * @param input 处理后的输入
     * @param state 输入状态
     */
    private void processTouchInput(RawInput input, InputState state) {
        state.getMouse().setX(input.getTouchX());
        state.getMouse().setY(input.getTouchY());
        state.getMouse().setLeft(input.isTouchPressed());
    }
    
    /**
     * 设置原始传感器数据到输出状态
     * @param input 处理后的输入
     * @param state 输入状态
     */
    private void setRawSensorData(RawInput input, InputState state) {
        state.getGyroscope().setPitch(input.getGyroPitch());
        state.getGyroscope().setRoll(input.getGyroRoll());
        state.getGyroscope().setYaw(input.getGyroYaw());
        state.getGyroscope().setDeadzone(gyroDeadzone);
        state.getGyroscope().setSmoothing(gyroSmoothing);
    }

    // 辅助方法：应用死区
    private float applyDeadzone(float value, float deadzone) {
        if (Math.abs(value) < deadzone) {
            return 0;
        }
        return value;
    }

    // 辅助方法：应用曲线映射
    private float applyCurve(float value, float exponent) {
        // 使用幂函数进行曲线映射
        // 保留符号
        float sign = Math.signum(value);
        float absValue = Math.abs(value);
        return sign * (float) Math.pow(absValue, exponent);
    }

    // 辅助方法：线性插值
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // 辅助方法：限制值在范围内
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // 配置参数的getter和setter
    public float getGyroDeadzone() {
        return gyroDeadzone;
    }

    public void setGyroDeadzone(float gyroDeadzone) {
        this.gyroDeadzone = gyroDeadzone;
    }

    public float getGyroSmoothing() {
        return gyroSmoothing;
    }

    public void setGyroSmoothing(float gyroSmoothing) {
        this.gyroSmoothing = gyroSmoothing;
    }

    public float getJoystickDeadzone() {
        return joystickDeadzone;
    }

    public void setJoystickDeadzone(float joystickDeadzone) {
        this.joystickDeadzone = joystickDeadzone;
    }

    public float getJoystickSmoothing() {
        return joystickSmoothing;
    }

    public void setJoystickSmoothing(float joystickSmoothing) {
        this.joystickSmoothing = joystickSmoothing;
    }

    public float getCurveExponent() {
        return curveExponent;
    }

    public void setCurveExponent(float curveExponent) {
        this.curveExponent = curveExponent;
    }
}
