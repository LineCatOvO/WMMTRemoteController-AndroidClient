package com.linecat.wmmtcontroller.control.ui;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.control.operation.ControlAction;

import java.util.Map;

/**
 * 陀螺仪控制节点实现
 * 
 * 继承ControlNode，实现陀螺仪类型的控制节点
 */
public class GyroControlNode extends ControlNode {
    
    private float gyroX;
    private float gyroY;
    private float gyroZ;
    private String operationType;
    private float sensitivity;
    
    public GyroControlNode(String nodeId, String nodeName, 
                          float x, float y, float width, float height, 
                          String operationType, float sensitivity) {
        super(nodeId, nodeName, NodeType.GYROSCOPE, x, y, width, height);
        this.gyroX = 0.0f;
        this.gyroY = 0.0f;
        this.gyroZ = 0.0f;
        this.operationType = operationType;
        this.sensitivity = sensitivity > 0 ? sensitivity : 1.0f;
    }
    
    @Override
    public ControlAction processInput(RawInput rawInput, LayoutSnapshot layout) {
        // 检查是否在陀螺仪区域内（虽然陀螺仪是全局感应，但可以限制在特定区域内生效）
        float normalizedX = rawInput.getTouchX() / layout.getScreenWidth();
        float normalizedY = rawInput.getTouchY() / layout.getScreenHeight();
        
        boolean inRegion = hitTest(normalizedX, normalizedY);
        
        if (inRegion) {
            // 使用原始输入中的陀螺仪数据
            this.gyroX = rawInput.getGyroX() * sensitivity;
            this.gyroY = rawInput.getGyroY() * sensitivity;
            this.gyroZ = rawInput.getGyroZ() * sensitivity;
        } else {
            // 区域外则将值设为0
            this.gyroX = 0.0f;
            this.gyroY = 0.0f;
            this.gyroZ = 0.0f;
        }
        
        // 创建控制动作
        ControlAction action = new ControlAction(getNodeId() + "_gyro_action", 
                                               ControlAction.ActionType.ANALOG, 
                                               operationType);
        // 使用X轴值作为主要值，Y轴值作为次值
        action.updateState(gyroX, gyroY, false);
        
        return action;
    }
    
    @Override
    public void updateState(Map<String, Object> stateMap) {
        // 可以从状态映射中更新节点状态
        if (stateMap.containsKey("gyroX")) {
            gyroX = ((Number) stateMap.get("gyroX")).floatValue();
        }
        if (stateMap.containsKey("gyroY")) {
            gyroY = ((Number) stateMap.get("gyroY")).floatValue();
        }
        if (stateMap.containsKey("gyroZ")) {
            gyroZ = ((Number) stateMap.get("gyroZ")).floatValue();
        }
    }
    
    public float getGyroX() {
        return gyroX;
    }
    
    public float getGyroY() {
        return gyroY;
    }
    
    public float getGyroZ() {
        return gyroZ;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public float getSensitivity() {
        return sensitivity;
    }
    
    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity > 0 ? sensitivity : 1.0f;
    }
    
    /**
     * 设置陀螺仪值
     * @param x X轴值
     * @param y Y轴值
     * @param z Z轴值
     */
    public void setGyroValues(float x, float y, float z) {
        this.gyroX = x;
        this.gyroY = y;
        this.gyroZ = z;
    }
}