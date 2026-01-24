package com.linecat.wmmtcontroller.core.layout;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.migration.LayoutToControlNodeConverter;
import com.linecat.wmmtcontroller.control.ui.ControlNode;
import com.linecat.wmmtcontroller.control.mapping.DeviceMapping;
import com.linecat.wmmtcontroller.control.mapping.ThreeTierControlManager;

import java.util.List;

/**
 * 增强版布局引擎
 * 使用新的三层架构管理器（UI层、Operation层、Mapping层）
 */
public class EnhancedLayoutEngine {
    
    private ThreeTierControlManager controlManager;
    private LayoutSnapshot currentLayout;
    
    public EnhancedLayoutEngine(DeviceMapping deviceMapping) {
        this.controlManager = new ThreeTierControlManager();
        
        // 添加传入的设备映射并设置为活动映射
        if (deviceMapping != null) {
            controlManager.addDeviceMapping(deviceMapping);
            controlManager.setActiveDeviceMapping(deviceMapping);
        }
    }
    
    /**
     * 从布局快照加载控制节点
     * @param layoutSnapshot 布局快照
     */
    public void loadLayoutFromSnapshot(LayoutSnapshot layoutSnapshot) {
        this.currentLayout = layoutSnapshot;
        
        // 使用转换器将布局转换为控制节点
        List<ControlNode> controlNodes = LayoutToControlNodeConverter.convertLayoutToControlNodes(layoutSnapshot);
        
        // 清除现有的控制节点
        controlManager.getUINodeManager().clearControlNodes();
        
        // 添加新的控制节点到UI层
        for (ControlNode node : controlNodes) {
            controlManager.addControlNode(node);
        }
        
        // 设置当前布局
        controlManager.setLayout(layoutSnapshot);
    }
    
    /**
     * 执行布局处理
     * @param rawInput 原始输入
     * @param frameId 帧ID
     * @return 输入状态
     */
    public InputState executeLayout(RawInput rawInput, long frameId) {
        // 使用三层架构管理器处理输入流程
        return controlManager.processInputFlow(rawInput, frameId);
    }
    
    /**
     * 获取当前使用的三层控制管理器
     * @return 三层控制管理器
     */
    public ThreeTierControlManager getControlManager() {
        return controlManager;
    }
    
    /**
     * 更新设备映射
     * @param deviceMapping 新的设备映射
     */
    public void updateDeviceMapping(DeviceMapping deviceMapping) {
        if (deviceMapping != null) {
            // 添加新的设备映射
            controlManager.addDeviceMapping(deviceMapping);
            // 设置为活动映射
            controlManager.setActiveDeviceMapping(deviceMapping);
        }
    }
    
    /**
     * 重置布局引擎
     */
    public void reset() {
        controlManager.reset();
        this.currentLayout = null;
    }
    
    /**
     * 获取当前布局
     * @return 当前布局快照
     */
    public LayoutSnapshot getCurrentLayout() {
        return currentLayout;
    }
    
    /**
     * 添加控制节点
     * @param node 控制节点
     */
    public void addControlNode(ControlNode node) {
        controlManager.addControlNode(node);
    }
    
    /**
     * 添加设备映射
     * @param mapping 设备映射
     */
    public void addDeviceMapping(DeviceMapping mapping) {
        controlManager.addDeviceMapping(mapping);
    }
    
    /**
     * 设置活动设备映射
     * @param mapping 设备映射
     */
    public void setActiveDeviceMapping(DeviceMapping mapping) {
        controlManager.setActiveDeviceMapping(mapping);
    }
}