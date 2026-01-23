package com.linecat.wmmtcontroller.control;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;

import java.util.List;
import java.util.ArrayList;

/**
 * 控制层协调器
 * 
 * 演示ControlNode、ControlAction和DeviceMapping三个类如何协同工作
 * 实现三层架构的数据流转：UI层 -> Operation层 -> Mapping层
 */
public class ControlLayerCoordinator {
    
    private List<ControlNode> controlNodes;
    private DeviceMapping deviceMapping;
    
    public ControlLayerCoordinator(DeviceMapping deviceMapping) {
        this.controlNodes = new ArrayList<>();
        this.deviceMapping = deviceMapping;
    }
    
    /**
     * 添加控制节点
     * @param controlNode 控制节点
     */
    public void addControlNode(ControlNode controlNode) {
        controlNodes.add(controlNode);
    }
    
    /**
     * 处理输入并生成最终的输入状态
     * @param rawInput 原始输入
     * @param layout 布局快照
     * @param inputState 输入状态
     */
    public void processInput(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        if (rawInput == null || layout == null || inputState == null) {
            return;
        }
        
        // 清除之前的状态
        deviceMapping.clearPreviousState(inputState);
        
        // 存储生成的控制动作
        List<ControlAction> actions = new ArrayList<>();
        
        // 第一层：UI层处理 - ControlNode处理原始输入
        for (ControlNode node : controlNodes) {
            if (node.isActive() && node.isVisible()) {
                // 检查节点是否被激活（例如，触摸点是否在节点区域内）
                if (node.hitTest(rawInput.getTouchX() / layout.getScreenWidth(), 
                               rawInput.getTouchY() / layout.getScreenHeight())) {
                    // 处理输入并生成控制动作
                    ControlAction action = node.processInput(rawInput, layout);
                    if (action != null) {
                        actions.add(action);
                    }
                } else {
                    // 对于非触摸激活的节点，可能需要持续处理（如陀螺仪节点）
                    ControlAction action = node.processInput(rawInput, layout);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        }
        
        // 第二层和第三层：应用控制动作到输入状态
        // ControlAction已经包含了处理逻辑，DeviceMapping负责映射到具体设备
        deviceMapping.applyActionsToState(actions, inputState);
    }
    
    /**
     * 获取所有控制节点
     * @return 控制节点列表
     */
    public List<ControlNode> getControlNodes() {
        return new ArrayList<>(controlNodes);
    }
    
    /**
     * 设置设备映射
     * @param deviceMapping 设备映射
     */
    public void setDeviceMapping(DeviceMapping deviceMapping) {
        this.deviceMapping = deviceMapping;
    }
    
    /**
     * 获取当前设备映射
     * @return 设备映射
     */
    public DeviceMapping getDeviceMapping() {
        return deviceMapping;
    }
    
    /**
     * 清空所有控制节点
     */
    public void clearControlNodes() {
        controlNodes.clear();
    }
}