package com.linecat.wmmtcontroller.control.ui;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;

import java.util.Map;

/**
 * 轴控制节点实现
 * 
 * 继承ControlNode，实现轴类型的控制节点（如摇杆、滑块等）
 */
public class AxisControlNode extends ControlNode {
    
    private float currentValue;
    private float minValue;
    private float maxValue;
    private String operationType;
    
    public AxisControlNode(String nodeId, String nodeName, 
                          float x, float y, float width, float height, 
                          float minValue, float maxValue, String operationType) {
        super(nodeId, nodeName, NodeType.AXIS, x, y, width, height);
        this.currentValue = 0.0f;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.operationType = operationType;
    }
    
    @Override
    public ControlAction processInput(RawInput rawInput, LayoutSnapshot layout) {
        // 检查触摸点是否在轴区域内
        float normalizedX = rawInput.getTouchX() / layout.getScreenWidth();
        float normalizedY = rawInput.getTouchY() / layout.getScreenHeight();
        
        if (hitTest(normalizedX, normalizedY) && rawInput.isTouchPressed()) {
            // 计算触摸点在轴区域内的相对位置
            float relativeX = (normalizedX - x) / width;
            float relativeY = (normalizedY - y) / height;
            
            // 根据轴的方向计算值
            // 对于水平轴，使用X位置；对于垂直轴，使用Y位置
            // 这里简化为根据宽高比判断轴向，或者可以引入专门的轴方向参数
            if (width >= height) {
                // 水平轴
                currentValue = minValue + (maxValue - minValue) * relativeX;
            } else {
                // 垂直轴
                currentValue = minValue + (maxValue - minValue) * (1.0f - relativeY); // Y轴翻转，底部为最大值
            }
            
            // 将值限制在范围内
            currentValue = Math.max(minValue, Math.min(maxValue, currentValue));
        } else if (!rawInput.isTouchPressed()) {
            // 触摸释放时，将值归零（可根据需要调整行为）
            currentValue = 0.0f;
        }
        
        // 创建控制动作
        ControlAction action = new ControlAction(getNodeId() + "_action", 
                                               ControlAction.ActionType.ANALOG, 
                                               operationType);
        action.updateState(remapToNormalizedRange(currentValue), false);
        
        return action;
    }
    
    /**
     * 将当前值映射到-1.0到1.0的标准化范围
     * @param value 原始值
     * @return 标准化后的值
     */
    private float remapToNormalizedRange(float value) {
        if (minValue == maxValue) {
            return 0.0f;
        }
        
        // 将值从[minValue, maxValue]映射到[-1.0, 1.0]
        float normalized = 2.0f * (value - minValue) / (maxValue - minValue) - 1.0f;
        return Math.max(-1.0f, Math.min(1.0f, normalized));
    }
    
    @Override
    public void updateState(Map<String, Object> stateMap) {
        // 可以从状态映射中更新节点状态
        if (stateMap.containsKey("value")) {
            currentValue = ((Number) stateMap.get("value")).floatValue();
        }
    }
    
    public float getCurrentValue() {
        return currentValue;
    }
    
    public float getMinValue() {
        return minValue;
    }
    
    public float getMaxValue() {
        return maxValue;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    /**
     * 设置当前值
     * @param value 新值
     */
    public void setCurrentValue(float value) {
        this.currentValue = Math.max(minValue, Math.min(maxValue, value));
    }
}