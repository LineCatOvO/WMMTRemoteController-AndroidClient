package com.linecat.wmmtcontroller.control.operation;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.control.ui.ButtonControlNode;
import com.linecat.wmmtcontroller.control.ui.ControlNode;
import com.linecat.wmmtcontroller.control.mapping.DeviceMapping;
import com.linecat.wmmtcontroller.control.mapping.ThreeTierControlManager;

import java.util.List;

/**
 * 三层控制架构使用示例
 * 
 * 演示如何使用新的三层架构管理器
 */
public class ControlArchitectureExample {
    
    private ThreeTierControlManager controlManager;
    
    public ControlArchitectureExample() {
        this.controlManager = new ThreeTierControlManager();
        
        // 初始化示例数据
        initializeExampleData();
    }
    
    /**
     * 初始化示例数据
     */
    private void initializeExampleData() {
        // 创建设备映射
        DeviceMapping keyboardMapping = new DeviceMapping("keyboard_mapping", "键盘映射", DeviceMapping.MappingType.KEYBOARD);
        keyboardMapping.addOperationToKeyMapping("STEERING_LEFT", "KeyA");
        keyboardMapping.addOperationToKeyMapping("STEERING_RIGHT", "KeyD");
        keyboardMapping.addOperationToKeyMapping("THROTTLE", "KeyW");
        keyboardMapping.addOperationToKeyMapping("BRAKE", "KeyS");
        keyboardMapping.addOperationToKeyMapping("GEAR_UP", "KeyQ");
        keyboardMapping.addOperationToKeyMapping("GEAR_DOWN", "KeyE");
        
        // 添加设备映射到Mapping层
        controlManager.addDeviceMapping(keyboardMapping);
        controlManager.setActiveDeviceMapping(keyboardMapping);
        
        // 创建控制节点
        ControlNode steeringNode = new ButtonControlNode(
            "steering_btn", 
            "方向盘按钮", 
            0.1f, 0.7f, 0.2f, 0.2f, 
            "STEERING_LEFT"
        );
        
        ControlNode throttleNode = new ButtonControlNode(
            "throttle_btn", 
            "油门按钮", 
            0.7f, 0.5f, 0.2f, 0.3f, 
            "THROTTLE"
        );
        
        ControlNode brakeNode = new ButtonControlNode(
            "brake_btn", 
            "刹车按钮", 
            0.4f, 0.5f, 0.2f, 0.3f, 
            "BRAKE"
        );
        
        // 添加控制节点到UI层
        controlManager.addControlNode(steeringNode);
        controlManager.addControlNode(throttleNode);
        controlManager.addControlNode(brakeNode);
    }
    
    /**
     * 处理输入示例
     * @param rawInput 原始输入
     * @param frameId 帧ID
     * @return 处理后的输入状态
     */
    public InputState processInput(RawInput rawInput, long frameId) {
        return controlManager.processInputFlow(rawInput, frameId);
    }
    
    /**
     * 设置布局示例
     * @param layout 布局快照
     */
    public void setLayout(LayoutSnapshot layout) {
        controlManager.setLayout(layout);
    }
    
    /**
     * 获取控制管理器
     * @return 三层控制管理器
     */
    public ThreeTierControlManager getControlManager() {
        return controlManager;
    }
    
    /**
     * 完整的使用示例
     */
    public static void demonstrateUsage() {
        // 创建示例
        ControlArchitectureExample example = new ControlArchitectureExample();
        
        // 模拟原始输入
        RawInput rawInput = new RawInput();
        rawInput.setTouchX(100); // 假设触摸坐标
        rawInput.setTouchY(1500);
        rawInput.setTouchPressed(true);
        
        // 处理输入
        InputState inputState = example.processInput(rawInput, System.currentTimeMillis());
        
        // 输出结果
        System.out.println("Input State Keys: " + inputState.getKeys());
        // 以下方法已从InputState中移除，后续将在GameInputState中实现
        // System.out.println("Steering: " + inputState.getSteering());
        // System.out.println("Throttle: " + inputState.getThrottle());
        // System.out.println("Brake: " + inputState.getBrake());
    }
    
    public static void main(String[] args) {
        demonstrateUsage();
    }
}