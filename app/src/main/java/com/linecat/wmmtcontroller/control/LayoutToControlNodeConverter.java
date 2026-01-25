package com.linecat.wmmtcontroller.control;

import com.linecat.wmmtcontroller.model.layout.LayoutConfiguration;
import com.linecat.wmmtcontroller.model.layout.UiElement;
import com.linecat.wmmtcontroller.model.layout.Operation;

import java.util.ArrayList;
import java.util.List;

/**
 * 布局配置到控制节点的转换器
 * 将布局配置转换为相应的控制节点
 */
public class LayoutToControlNodeConverter {

    /**
     * 将布局配置转换为控制节点列表
     *
     * @param config 布局配置
     * @return 控制节点列表
     */
    public static List<ControlNode> convertToControlNodes(LayoutConfiguration config) {
        List<ControlNode> controlNodes = new ArrayList<>();

        // 将 UI 元素转换为控制节点
        if (config.getUi() != null) {
            for (int i = 0; i < config.getUi().size(); i++) {
                UiElement uiElement = config.getUi().get(i);
                ControlNode controlNode = createControlNodeFromUiElement(uiElement, i);
                if (controlNode != null) {
                    controlNodes.add(controlNode);
                }
            }
        }

        // 将操作转换为控制节点
        if (config.getOperation() != null) {
            for (int i = 0; i < config.getOperation().size(); i++) {
                Operation operation = config.getOperation().get(i);
                ControlNode controlNode = createControlNodeFromOperation(operation, i);
                if (controlNode != null) {
                    controlNodes.add(controlNode);
                }
            }
        }

        return controlNodes;
    }

    /**
     * 从 UI 元素创建控制节点
     *
     * @param uiElement UI 元素
     * @param index     索引
     * @return 控制节点
     */
    private static ControlNode createControlNodeFromUiElement(UiElement uiElement, int index) {
        if (uiElement == null) {
            return null;
        }

        // 根据 UI 元素的资源或类型确定控制节点类型
        String resourceId = uiElement.getResource();
        if (resourceId != null) {
            if (resourceId.contains("button") || resourceId.contains("btn")) {
                // 创建按钮控制节点
                return new ButtonControlNode(uiElement.getId(), "button_" + index);
            } else if (resourceId.contains("joystick") || resourceId.contains("stick")) {
                // 创建轴控制节点
                return new AxisControlNode(uiElement.getId(), "axis_" + index);
            } else if (resourceId.contains("gyro") || resourceId.contains("sensor")) {
                // 创建陀螺仪控制节点
                return new GyroControlNode(uiElement.getId(), "gyro_" + index);
            }
        }

        // 默认创建按钮控制节点
        return new ButtonControlNode(uiElement.getId(), "button_default");
    }

    /**
     * 从操作创建控制节点
     *
     * @param operation 操作
     * @param index     索引
     * @return 控制节点
     */
    private static ControlNode createControlNodeFromOperation(Operation operation, int index) {
        if (operation == null) {
            return null;
        }

        String type = operation.getType();
        if (type != null) {
            switch (type.toLowerCase()) {
                case "button":
                    return new ButtonControlNode(operation.getId(), "button_op_" + index);
                case "steering":
                case "throttle":
                case "brake":
                case "axis":
                    return new AxisControlNode(operation.getId(), type.toLowerCase());
                case "gyro":
                    return new GyroControlNode(operation.getId(), "gyro_op_" + index);
                default:
                    // 根据默认值判断类型
                    if (operation.getDefaultVal() != null && operation.getDefaultVal() == 0.0f) {
                        return new AxisControlNode(operation.getId(), "axis_op_" + index);
                    } else {
                        return new ButtonControlNode(operation.getId(), "button_op_" + index);
                    }
            }
        }

        // 默认创建按钮控制节点
        return new ButtonControlNode(operation.getId(), "op_default");
    }

    /**
     * 将布局配置转换为控制动作
     *
     * @param config 布局配置
     * @return 控制动作
     */
    public static ControlAction convertToControlAction(LayoutConfiguration config) {
        ControlAction action = new ControlAction();
        List<ControlNode> controlNodes = convertToControlNodes(config);

        // 这里可以根据具体需求填充 ControlAction 的各个字段
        for (ControlNode node : controlNodes) {
            if (node instanceof ButtonControlNode) {
                ButtonControlNode btnNode = (ButtonControlNode) node;
                switch (btnNode.getButtonType().toLowerCase()) {
                    case "a":
                        action.setButtonA(btnNode.isPressed());
                        break;
                    case "b":
                        action.setButtonB(btnNode.isPressed());
                        break;
                    case "x":
                        action.setButtonX(btnNode.isPressed());
                        break;
                    case "y":
                        action.setButtonY(btnNode.isPressed());
                        break;
                    case "shoulder_l":
                        action.setShoulderL(btnNode.isPressed());
                        break;
                    case "shoulder_r":
                        action.setShoulderR(btnNode.isPressed());
                        break;
                }
            } else if (node instanceof AxisControlNode) {
                AxisControlNode axisNode = (AxisControlNode) node;
                switch (axisNode.getAxisType().toLowerCase()) {
                    case "steering":
                        action.setSteering(axisNode.getValue());
                        break;
                    case "throttle":
                        action.setThrottle(axisNode.getValue());
                        break;
                    case "brake":
                        action.setBrake(axisNode.getValue());
                        break;
                }
            } else if (node instanceof GyroControlNode) {
                GyroControlNode gyroNode = (GyroControlNode) node;
                switch (gyroNode.getGyroType().toLowerCase()) {
                    case "gyro_x":
                        action.setGyroX(gyroNode.getValue());
                        break;
                    case "gyro_y":
                        action.setGyroY(gyroNode.getValue());
                        break;
                    case "gyro_z":
                        action.setGyroZ(gyroNode.getValue());
                        break;
                }
            }
        }

        return action;
    }
}