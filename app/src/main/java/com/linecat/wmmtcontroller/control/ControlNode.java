package com.linecat.wmmtcontroller.control;

/**
 * 控制节点接口
 * 定义控制节点的基本行为
 */
public interface ControlNode {
    /**
     * 处理控制动作
     * @param action 控制动作
     */
    void handleAction(ControlAction action);

    /**
     * 获取控制节点的ID
     * @return 节点ID
     */
    String getId();

    /**
     * 更新控制节点状态
     */
    void update();
}