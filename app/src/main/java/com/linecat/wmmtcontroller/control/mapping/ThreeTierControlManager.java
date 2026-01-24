package com.linecat.wmmtcontroller.control.mapping;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.control.ui.UINodeManager;
import com.linecat.wmmtcontroller.control.operation.OperationNodeManager;
import com.linecat.wmmtcontroller.control.mapping.MappingNodeManager;
import com.linecat.wmmtcontroller.control.ui.ControlNode;
import com.linecat.wmmtcontroller.control.operation.ControlAction;
import com.linecat.wmmtcontroller.control.mapping.DeviceMapping;

import java.util.List;

/**
 * 三层控制管理器
 * 
 * 统一管理UI层、Operation层和Mapping层，提供完整的三层架构控制
 */
public class ThreeTierControlManager {
    
    private UINodeManager uiNodeManager;
    private OperationNodeManager operationNodeManager;
    private MappingNodeManager mappingNodeManager;
    
    public ThreeTierControlManager() {
        this.uiNodeManager = new UINodeManager();
        this.operationNodeManager = new OperationNodeManager();
        this.mappingNodeManager = new MappingNodeManager();
    }
    
    /**
     * 处理完整的三层架构输入流程
     * @param rawInput 原始输入
     * @param frameId 帧ID
     * @return 处理后的输入状态
     */
    public InputState processInputFlow(RawInput rawInput, long frameId) {
        // 创建输入状态
        InputState inputState = new InputState();
        inputState.setFrameId(frameId);
        
        // 第一步：UI层处理 - 处理原始输入，生成控制动作
        List<ControlAction> controlActions = uiNodeManager.processInput(rawInput);
        
        // 第二步：Operation层处理 - 对控制动作进行处理（应用死区、平滑、曲线等算法）
        List<ControlAction> processedActions = operationNodeManager.processActions(controlActions);
        
        // 第三步：Mapping层处理 - 将处理后的动作映射到设备输出
        mappingNodeManager.applyActionsToState(processedActions, inputState);
        
        return inputState;
    }
    
    /**
     * 设置当前布局
     * @param layout 布局快照
     */
    public void setLayout(LayoutSnapshot layout) {
        uiNodeManager.setLayout(layout);
    }
    
    /**
     * 获取UI节点管理器
     * @return UI节点管理器
     */
    public UINodeManager getUINodeManager() {
        return uiNodeManager;
    }
    
    /**
     * 获取Operation节点管理器
     * @return Operation节点管理器
     */
    public OperationNodeManager getOperationNodeManager() {
        return operationNodeManager;
    }
    
    /**
     * 获取Mapping节点管理器
     * @return Mapping节点管理器
     */
    public MappingNodeManager getMappingNodeManager() {
        return mappingNodeManager;
    }
    
    /**
     * 添加控制节点到UI层
     * @param node 控制节点
     */
    public void addControlNode(ControlNode node) {
        uiNodeManager.addControlNode(node);
    }
    
    /**
     * 添加控制动作到Operation层
     * @param action 控制动作
     */
    public void addControlAction(ControlAction action) {
        operationNodeManager.addControlAction(action);
    }
    
    /**
     * 添加设备映射到Mapping层
     * @param mapping 设备映射
     */
    public void addDeviceMapping(DeviceMapping mapping) {
        mappingNodeManager.addDeviceMapping(mapping);
    }
    
    /**
     * 设置当前活动的设备映射
     * @param mapping 设备映射
     */
    public void setActiveDeviceMapping(DeviceMapping mapping) {
        mappingNodeManager.setActiveMapping(mapping);
    }
    
    /**
     * 清空所有层的数据
     */
    public void clearAllLayers() {
        uiNodeManager.clearControlNodes();
        operationNodeManager.clearControlActions();
        mappingNodeManager.clearDeviceMappings();
    }
    
    /**
     * 更新所有层的状态
     */
    public void updateAllLayers() {
        uiNodeManager.updateAllNodes();
        operationNodeManager.updateAllActions();
    }
    
    /**
     * 获取总的控制节点数量
     * @return 节点总数
     */
    public int getTotalNodeCount() {
        return uiNodeManager.getNodeCount();
    }
    
    /**
     * 获取总的控制动作数量
     * @return 动作总数
     */
    public int getTotalActionCount() {
        return operationNodeManager.getActionCount();
    }
    
    /**
     * 获取总的设备映射数量
     * @return 映射总数
     */
    public int getTotalMappingCount() {
        return mappingNodeManager.getMappingCount();
    }
    
    /**
     * 重置整个三层架构管理器
     */
    public void reset() {
        clearAllLayers();
    }
}