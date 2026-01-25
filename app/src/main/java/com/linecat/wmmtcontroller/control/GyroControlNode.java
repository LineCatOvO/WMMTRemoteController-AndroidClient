package com.linecat.wmmtcontroller.control;

/**
 * 陀螺仪控制节点
 * 处理陀螺仪类型的控制输入
 */
public class GyroControlNode implements ControlNode {
    private String id;
    private String gyroType; // 陀螺仪轴类型，如 gyro_x, gyro_y, gyro_z
    private float value;

    public GyroControlNode(String id, String gyroType) {
        this.id = id;
        this.gyroType = gyroType;
        this.value = 0.0f;
    }

    @Override
    public void handleAction(ControlAction action) {
        switch (gyroType.toLowerCase()) {
            case "gyro_x":
                if (action.getGyroX() != null) {
                    this.value = action.getGyroX();
                }
                break;
            case "gyro_y":
                if (action.getGyroY() != null) {
                    this.value = action.getGyroY();
                }
                break;
            case "gyro_z":
                if (action.getGyroZ() != null) {
                    this.value = action.getGyroZ();
                }
                break;
            default:
                // 可能是其他自定义陀螺仪轴
                break;
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void update() {
        // 陀螺仪节点不需要特殊更新逻辑
    }

    public float getValue() {
        return value;
    }

    public String getGyroType() {
        return gyroType;
    }

    public void setValue(float value) {
        this.value = value;
    }
}