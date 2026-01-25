package com.linecat.wmmtcontroller.control;

/**
 * 轴控制节点
 * 处理轴类型的控制输入（如转向、油门、制动等）
 */
public class AxisControlNode implements ControlNode {
    private String id;
    private String axisType; // 轴类型，如 steering, throttle, brake 等
    private float value;

    public AxisControlNode(String id, String axisType) {
        this.id = id;
        this.axisType = axisType;
        this.value = 0.0f;
    }

    @Override
    public void handleAction(ControlAction action) {
        switch (axisType.toLowerCase()) {
            case "steering":
                if (action.getSteering() != null) {
                    this.value = action.getSteering();
                }
                break;
            case "throttle":
                if (action.getThrottle() != null) {
                    this.value = action.getThrottle();
                }
                break;
            case "brake":
                if (action.getBrake() != null) {
                    this.value = action.getBrake();
                }
                break;
            case "mouse_x":
                if (action.getMouseX() != null) {
                    this.value = action.getMouseX();
                }
                break;
            case "mouse_y":
                if (action.getMouseY() != null) {
                    this.value = action.getMouseY();
                }
                break;
            default:
                // 可能是其他自定义轴
                break;
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void update() {
        // 轴节点不需要特殊更新逻辑
    }

    public float getValue() {
        return value;
    }

    public String getAxisType() {
        return axisType;
    }

    public void setValue(float value) {
        this.value = value;
    }
}