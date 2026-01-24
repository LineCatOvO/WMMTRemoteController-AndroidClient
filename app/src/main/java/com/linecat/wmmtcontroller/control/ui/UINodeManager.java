package com.linecat.wmmtcontroller.control.ui;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.control.operation.ControlAction;

import java.util.ArrayList;
import java.util.List;

/**
 * UI节点管理器
 * 
 * 作为UI层的总控，负责：
 * 1. 管理所有ControlNode节点
 * 2. 处理节点的渲染和事件
 * 3. 统一向Operation层传递数据
 */
public class UINodeManager {
    
    private List<ControlNode> controlNodes;
    private LayoutSnapshot currentLayout;
    
    public UINodeManager() {
        this.controlNodes = new ArrayList<>();
    }
    
    /**
     * 添加控制节点
     * @param node 控制节点
     */
    public void addControlNode(ControlNode node) {
        if (node != null) {
            controlNodes.add(node);
        }
    }
    
    /**
     * 移除控制节点
     * @param node 控制节点
     */
    public void removeControlNode(ControlNode node) {
        if (node != null) {
            controlNodes.remove(node);
        }
    }
    
    /**
     * 清空所有控制节点
     */
    public void clearControlNodes() {
        controlNodes.clear();
    }
    
    /**
     * 获取所有控制节点
     * @return 控制节点列表
     */
    public List<ControlNode> getControlNodes() {
        return new ArrayList<>(controlNodes);
    }
    
    /**
     * 设置当前布局
     * @param layout 当前布局快照
     */
    public void setLayout(LayoutSnapshot layout) {
        this.currentLayout = layout;
    }
    
    /**
     * 处理原始输入，生成控制动作列表
     * @param rawInput 原始输入
     * @return 控制动作列表
     */
    public List<ControlAction> processInput(RawInput rawInput) {
        List<ControlAction> actions = new ArrayList<>();
        
        // 遍历所有控制节点，处理输入并生成控制动作
        for (ControlNode node : controlNodes) {
            if (node.isActive() && node.isVisible()) {
                ControlAction action = node.processInput(rawInput, currentLayout);
                if (action != null) {
                    actions.add(action);
                }
            }
        }
        
        return actions;
    }
    
    /**
     * 根据坐标查找命中的节点
     * @param normalizedX 归一化X坐标 (0.0-1.0)
     * @param normalizedY 归一化Y坐标 (0.0-1.0)
     * @return 命中的节点，如果没命中则返回null
     */
    public ControlNode hitTest(float normalizedX, float normalizedY) {
        for (ControlNode node : controlNodes) {
            if (node.isActive() && node.isVisible() && node.hitTest(normalizedX, normalizedY)) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * 更新所有节点的状态
     */
    public void updateAllNodes() {
        for (ControlNode node : controlNodes) {
            node.updateState(null); // 可以根据需要传递状态参数
        }
    }
    
    /**
     * 获取控制节点数量
     * @return 节点数量
     */
    public int getNodeCount() {
        return controlNodes.size();
    }
}