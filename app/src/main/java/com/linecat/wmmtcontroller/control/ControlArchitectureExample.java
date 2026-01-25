package com.linecat.wmmtcontroller.control;

import com.linecat.wmmtcontroller.model.DeviceMapping;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.control.operation.ControlAction;

/**
 * 控制架构示例
 * 演示如何使用控制节点架构
 */
public class ControlArchitectureExample {

    public static void main(String[] args) {
        System.out.println("Control Architecture Example");
        
        // 创建控制动作
        ControlAction action = new ControlAction("test_action", ControlAction.ActionType.ANALOG, "steering");
        action.updateState(0.5f, true); // 更新状态，0.5f为值，true为数字值
        
        // 创建设备映射
        DeviceMapping deviceMapping = DeviceMapping.fromControlAction(action);
        System.out.println("Device mapping created from control action");
        System.out.println("Steering: " + deviceMapping.getSteering());
        System.out.println("Throttle: " + deviceMapping.getThrottle());

        // 创建输入状态
        InputState inputState = InputState.fromControlAction(action);
        System.out.println("Input state created from control action");
    }
}