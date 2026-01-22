package com.linecat.wmmtcontroller.input;

import android.util.Log;

import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 输入解释器
 * 核心职责：将原始输入转换为标准化事件
 * 
 * 遵循设计原则：
 * 1. 原始输入只存在于 Java 层
 * 2. JS 永远不处理 pointer / 坐标
 * 3. Java 负责输入解释，JS 负责输入意义
 * 4. 区域命中、状态机、指针生命周期全部在 Java 层处理
 */
public class InputInterpreter {
    
    private static final String TAG = "InputInterpreter";
    
    // 屏幕尺寸（默认值，实际使用时应从系统获取）
    private static final float DEFAULT_SCREEN_WIDTH = 1080f;
    private static final float DEFAULT_SCREEN_HEIGHT = 1920f;
    
    // 输入状态跟踪（用于管理指针生命周期和状态机）
    private final InputStateTracker stateTracker;
    
    /**
     * 构造函数
     */
    public InputInterpreter() {
        this.stateTracker = new InputStateTracker();
    }
    
    /**
     * 解释原始输入，生成标准化事件
     * @param rawInput 原始输入数据
     * @param layout 布局快照
     * @return 标准化事件，如果没有可解释的事件则返回null
     */
    public NormalizedEvent interpret(RawInput rawInput, LayoutSnapshot layout) {
        if (rawInput == null || layout == null) {
            return null;
        }
        
        // 更新输入状态跟踪
        stateTracker.update(rawInput);
        
        // 检查触摸输入
        if (rawInput.isTouchPressed()) {
            return interpretTouchInput(rawInput, layout);
        }
        
        // 检查陀螺仪输入（示例：如果陀螺仪数据有变化，生成事件）
        if (hasGyroDataChanged(rawInput)) {
            return interpretGyroInput(rawInput, layout);
        }
        
        // 检查按键输入
        if (hasButtonInput(rawInput)) {
            return interpretKeyInput(rawInput, layout);
        }
        
        // 检查游戏手柄输入
        if (hasGamepadInput(rawInput)) {
            return interpretGamepadInput(rawInput, layout);
        }
        
        return null;
    }
    
    /**
     * 解释触摸输入
     */
    private NormalizedEvent interpretTouchInput(RawInput rawInput, LayoutSnapshot layout) {
        // 归一化触摸坐标（使用默认屏幕尺寸，实际应从系统获取）
        float normalizedX = rawInput.getTouchX() / DEFAULT_SCREEN_WIDTH;
        float normalizedY = rawInput.getTouchY() / DEFAULT_SCREEN_HEIGHT;
        
        // 确保坐标在0.0-1.0范围内
        normalizedX = Math.max(0f, Math.min(1f, normalizedX));
        normalizedY = Math.max(0f, Math.min(1f, normalizedY));
        
        // 区域命中检测
        Region region = layout.hitTest(normalizedX, normalizedY);
        if (region == null) {
            return null;
        }
        
        // 根据区域类型生成不同的事件
        switch (region.type()) {
            case BUTTON:
                return interpretTouchButton(rawInput, region);
            case AXIS:
                return interpretTouchAxis(rawInput, region, normalizedX, normalizedY);
            case GESTURE:
                return interpretTouchGesture(rawInput, region, normalizedX, normalizedY);
            default:
                Log.w(TAG, "Unknown region type: " + region.type());
                return null;
        }
    }
    
    /**
     * 解释触摸按钮输入
     */
    private NormalizedEvent interpretTouchButton(RawInput rawInput, Region region) {
        // 对于按钮区域，根据触摸状态生成按下/释放事件
        boolean pressed = rawInput.isTouchPressed();
        
        return ButtonEvent.create(region.id(), pressed);
    }
    
    /**
     * 解释触摸轴输入（如虚拟摇杆）
     */
    private NormalizedEvent interpretTouchAxis(RawInput rawInput, Region region, float normalizedX, float normalizedY) {
        // 获取区域中心
        float[] center = region.getCenter();
        
        // 计算相对中心的偏移量
        float deltaX = normalizedX - center[0];
        float deltaY = normalizedY - center[1];
        
        // 计算区域的宽高
        float width = region.right() - region.left();
        float height = region.bottom() - region.top();
        
        // 将偏移量归一化到 -1.0 到 1.0 范围
        float valueX = deltaX / (width / 2f);
        float valueY = deltaY / (height / 2f);
        
        // 限制值在 -1.0 到 1.0 范围内
        valueX = Math.max(-1f, Math.min(1f, valueX));
        valueY = Math.max(-1f, Math.min(1f, valueY));
        
        return AxisEvent.create(region.id(), valueX, valueY);
    }
    
    /**
     * 解释触摸手势输入
     */
    private NormalizedEvent interpretTouchGesture(RawInput rawInput, Region region, float normalizedX, float normalizedY) {
        // 目前仅支持基本的触摸按下手势，其他手势需要更复杂的状态跟踪
        // 这里根据触摸按下状态生成长按手势事件（示例）
        if (rawInput.isTouchPressed()) {
            // 实际应用中，这里应该有长按检测逻辑
            // 暂时简单返回长按手势
            return GestureEvent.createLongPress(region.id());
        }
        return null;
    }
    
    /**
     * 解释按键输入
     */
    private NormalizedEvent interpretKeyInput(RawInput rawInput, LayoutSnapshot layout) {
        // 按键输入直接映射到对应的区域事件
        // 这里需要根据按键状态查找对应的区域ID
        // 暂时返回null，需要后续实现按键映射表
        return null;
    }
    
    /**
     * 解释游戏手柄输入
     */
    private NormalizedEvent interpretGamepadInput(RawInput rawInput, LayoutSnapshot layout) {
        // 游戏手柄输入直接映射到对应的区域事件
        // 这里需要根据游戏手柄按钮/轴代码查找对应的区域ID
        // 暂时返回null，需要后续实现游戏手柄映射表
        return null;
    }
    
    /**
     * 解释陀螺仪输入
     */
    private NormalizedEvent interpretGyroInput(RawInput rawInput, LayoutSnapshot layout) {
        // 陀螺仪输入通常映射到轴事件
        // 这里简单将陀螺仪数据映射到一个固定区域的轴事件
        // 实际应用中，应该根据陀螺仪数据和布局生成合适的事件
        
        // 示例：将陀螺仪的Pitch和Roll映射到"gyro"区域的轴事件
        Region gyroRegion = layout.getRegionById("gyro");
        if (gyroRegion != null) {
            // 归一化陀螺仪数据到 -1.0 到 1.0 范围
            // 假设陀螺仪数据范围是 -180 到 180
            float valueX = rawInput.getGyroRoll() / 180f;
            float valueY = rawInput.getGyroPitch() / 180f;
            
            return AxisEvent.create(gyroRegion.id(), valueX, valueY);
        }
        
        return null;
    }
    
    /**
     * 检查陀螺仪数据是否有变化
     * @param rawInput 原始输入数据
     * @return 如果陀螺仪数据有变化则返回true，否则返回false
     */
    private boolean hasGyroDataChanged(RawInput rawInput) {
        // 简单实现：如果陀螺仪数据不为0，则认为有变化
        // 实际应用中，应该有更复杂的变化检测逻辑
        return rawInput.getGyroPitch() != 0 || 
               rawInput.getGyroRoll() != 0 || 
               rawInput.getGyroYaw() != 0;
    }
    
    /**
     * 检查是否有按键输入
     * @param rawInput 原始输入数据
     * @return 如果有按键输入则返回true，否则返回false
     */
    private boolean hasButtonInput(RawInput rawInput) {
        return rawInput.isButtonA() || 
               rawInput.isButtonB() || 
               rawInput.isButtonC() || 
               rawInput.isButtonD();
    }
    
    /**
     * 检查是否有游戏手柄输入
     * @param rawInput 原始输入数据
     * @return 如果有游戏手柄输入则返回true，否则返回false
     */
    private boolean hasGamepadInput(RawInput rawInput) {
        // 检查游戏手柄是否有输入
        RawInput.GamepadData gamepad = rawInput.getGamepad();
        return gamepad != null && 
               (!gamepad.getAxes().isEmpty() || !gamepad.getButtons().isEmpty());
    }
    
    /**
     * 重置输入解释器
     * 清理状态跟踪器，重置状态机
     */
    public void reset() {
        stateTracker.reset();
    }
    
    /**
     * 输入状态跟踪器（内部类）
     * 负责管理指针生命周期和状态机
     */
    private static class InputStateTracker {
        
        /**
         * 更新输入状态跟踪
         * @param rawInput 原始输入数据
         */
        public void update(RawInput rawInput) {
            // 实现指针生命周期管理和状态机逻辑
            // 目前仅作为占位符，后续需要实现完整的状态跟踪
        }
        
        /**
         * 重置状态跟踪器
         */
        public void reset() {
            // 清理所有状态跟踪数据
        }
    }
}
