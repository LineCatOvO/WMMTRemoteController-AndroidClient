package com.linecat.wmmtcontroller.control.ui;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;

import java.util.Map;

/**
 * 按钮控制节点实现
 * 
 * 继承ControlNode，实现按钮类型的控制节点
 */
public class ButtonControlNode extends ControlNode {
    
    private boolean isPressed;
    private String operationType;
    
    public ButtonControlNode(String nodeId, String nodeName, 
                           float x, float y, float width, float height, 
                           String operationType) {
        super(nodeId, nodeName, NodeType.BUTTON, x, y, width, height);
        this.isPressed = false;
        this.operationType = operationType;
    }
    
    @Override
    public ControlAction processInput(RawInput rawInput, LayoutSnapshot layout) {
        // 检查触摸点是否在按钮区域内
        float normalizedX = rawInput.getTouchX() / layout.getScreenWidth();
        float normalizedY = rawInput.getTouchY() / layout.getScreenHeight();
        
        boolean inBounds = hitTest(normalizedX, normalizedY);
        boolean wasPressed = isPressed;
        
        // 更新按钮状态
        isPressed = inBounds && rawInput.isTouchPressed();
        
        // 创建控制动作
        ControlAction action = new ControlAction(getNodeId() + "_action", 
                                               ControlAction.ActionType.DIGITAL, 
                                               operationType);
        action.updateState(isPressed ? 1.0f : 0.0f, isPressed);
        
        return action;
    }
    
    @Override
    public void updateState(Map<String, Object> stateMap) {
        // 可以从状态映射中更新节点状态
        if (stateMap.containsKey("pressed")) {
            isPressed = (Boolean) stateMap.get("pressed");
        }
    }
    
    public boolean isButtonPressed() {
        return isPressed;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
}