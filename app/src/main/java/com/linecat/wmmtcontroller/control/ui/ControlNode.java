package com.linecat.wmmtcontroller.control.ui;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.control.operation.ControlAction;

import java.util.Map;

/**
 * 控制节点类 (Control Node)
 * 
 * 对应三层架构中的 UI 层，负责：
 * 1. 接收原始输入（触控、陀螺仪等）
 * 2. 执行传感器级输入处理
 * 3. 输出归一化后的抽象值
 * 4. 将结果绑定到 Operation
 * 
 * 这是输入处理的第一层，主要负责将物理输入转换为逻辑上的控制意图
 */
public abstract class ControlNode {
    
    protected String nodeId;
    protected String nodeName;
    protected NodeType nodeType;
    
    // 节点位置和大小（归一化坐标，0.0-1.0）
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    
    // 节点状态
    protected boolean isActive;
    protected boolean isVisible;
    
    public enum NodeType {
        BUTTON,      // 按钮节点
        AXIS,        // 轴节点（如摇杆）
        GESTURE,     // 手势节点
        GYROSCOPE,   // 陀螺仪节点
        CUSTOM       // 自定义节点
    }
    
    public ControlNode(String nodeId, String nodeName, NodeType nodeType, 
                      float x, float y, float width, float height) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isActive = true;
        this.isVisible = true;
    }
    
    /**
     * 检查指定坐标是否在节点区域内
     * @param touchX 归一化X坐标 (0.0-1.0)
     * @param touchY 归一化Y坐标 (0.0-1.0)
     * @return 是否在节点区域内
     */
    public boolean hitTest(float touchX, float touchY) {
        return touchX >= x && touchX <= (x + width) && 
               touchY >= y && touchY <= (y + height);
    }
    
    /**
     * 处理原始输入，生成控制动作
     * @param rawInput 原始输入数据
     * @param layout 当前布局快照
     * @return 控制动作对象
     */
    public abstract ControlAction processInput(RawInput rawInput, LayoutSnapshot layout);
    
    /**
     * 更新节点状态
     * @param stateMap 状态映射
     */
    public abstract void updateState(Map<String, Object> stateMap);
    
    /**
     * 获取节点激活状态
     * @return 激活状态
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 设置节点激活状态
     * @param active 激活状态
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * 获取节点可见性
     * @return 可见性状态
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 设置节点可见性
     * @param visible 可见性状态
     */
    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }
    
    /**
     * 获取节点ID
     * @return 节点ID
     */
    public String getNodeId() {
        return nodeId;
    }
    
    /**
     * 获取节点名称
     * @return 节点名称
     */
    public String getNodeName() {
        return nodeName;
    }
    
    /**
     * 获取节点类型
     * @return 节点类型
     */
    public NodeType getNodeType() {
        return nodeType;
    }
    
    /**
     * 获取节点位置和尺寸信息
     * @return 包含x, y, width, height的数组
     */
    public float[] getBounds() {
        return new float[]{x, y, width, height};
    }
    
    /**
     * 设置节点位置
     * @param x X坐标 (0.0-1.0)
     * @param y Y坐标 (0.0-1.0)
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * 设置节点尺寸
     * @param width 宽度 (0.0-1.0)
     * @param height 高度 (0.0-1.0)
     */
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }
}