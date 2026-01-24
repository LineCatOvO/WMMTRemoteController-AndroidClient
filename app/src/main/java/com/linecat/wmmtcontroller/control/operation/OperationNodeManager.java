package com.linecat.wmmtcontroller.control.operation;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;

import java.util.ArrayList;
import java.util.List;

/**
 * Operation节点管理器
 * 
 * 作为Operation层的总控，负责：
 * 1. 管理所有ControlAction动作
 * 2. 处理动作的语义转换和处理算法
 * 3. 统一向Mapping层传递数据
 */
public class OperationNodeManager {
    
    private List<ControlAction> controlActions;
    
    public OperationNodeManager() {
        this.controlActions = new ArrayList<>();
    }
    
    /**
     * 添加控制动作
     * @param action 控制动作
     */
    public void addControlAction(ControlAction action) {
        if (action != null) {
            controlActions.add(action);
        }
    }
    
    /**
     * 移除控制动作
     * @param action 控制动作
     */
    public void removeControlAction(ControlAction action) {
        if (action != null) {
            controlActions.remove(action);
        }
    }
    
    /**
     * 清空所有控制动作
     */
    public void clearControlActions() {
        controlActions.clear();
    }
    
    /**
     * 获取所有控制动作
     * @return 控制动作列表
     */
    public List<ControlAction> getControlActions() {
        return new ArrayList<>(controlActions);
    }
    
    /**
     * 处理控制动作列表，应用处理算法（死区、平滑、曲线等）
     * @param inputActions 输入的控制动作列表
     * @return 处理后的控制动作列表
     */
    public List<ControlAction> processActions(List<ControlAction> inputActions) {
        List<ControlAction> processedActions = new ArrayList<>();
        
        for (ControlAction action : inputActions) {
            if (action != null) {
                // 应用处理算法（死区、平滑、曲线等）
                ControlAction processedAction = processSingleAction(action);
                if (processedAction != null) {
                    processedActions.add(processedAction);
                }
            }
        }
        
        return processedActions;
    }
    
    /**
     * 处理单个控制动作
     * @param action 输入的控制动作
     * @return 处理后的控制动作
     */
    private ControlAction processSingleAction(ControlAction action) {
        // 这里可以应用各种处理算法
        // 目前直接返回原动作，可以根据需要添加处理逻辑
        return action;
    }
    
    /**
     * 更新所有动作的状态
     */
    public void updateAllActions() {
        for (ControlAction action : controlActions) {
            // 可以根据需要更新动作状态
            // 这里暂时不执行任何操作，可根据具体需求实现
        }
    }
    
    /**
     * 获取控制动作数量
     * @return 动作数量
     */
    public int getActionCount() {
        return controlActions.size();
    }
    
    /**
     * 根据操作类型获取控制动作
     * @param operationType 操作类型
     * @return 符合条件的动作列表
     */
    public List<ControlAction> getActionsByType(String operationType) {
        List<ControlAction> result = new ArrayList<>();
        for (ControlAction action : controlActions) {
            if (action != null && operationType.equals(action.getOperationType())) {
                result.add(action);
            }
        }
        return result;
    }
}