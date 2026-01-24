package com.linecat.wmmtcontroller.control.mapping;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.control.operation.ControlAction;
import com.linecat.wmmtcontroller.control.mapping.DeviceMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapping节点管理器
 * 
 * 作为Mapping层的总控，负责：
 * 1. 管理所有DeviceMapping映射
 * 2. 处理设备映射和适配
 * 3. 统一生成最终的输入状态
 */
public class MappingNodeManager {
    
    private List<DeviceMapping> deviceMappings;
    private DeviceMapping activeMapping; // 当前活动的映射
    
    public MappingNodeManager() {
        this.deviceMappings = new ArrayList<>();
    }
    
    /**
     * 添加设备映射
     * @param mapping 设备映射
     */
    public void addDeviceMapping(DeviceMapping mapping) {
        if (mapping != null) {
            deviceMappings.add(mapping);
            
            // 如果还没有活动映射，则将第一个添加的作为活动映射
            if (activeMapping == null) {
                activeMapping = mapping;
            }
        }
    }
    
    /**
     * 移除设备映射
     * @param mapping 设备映射
     */
    public void removeDeviceMapping(DeviceMapping mapping) {
        if (mapping != null) {
            deviceMappings.remove(mapping);
            
            // 如果移除的是当前活动映射，则选择另一个作为活动映射
            if (mapping.equals(activeMapping) && !deviceMappings.isEmpty()) {
                activeMapping = deviceMappings.get(0);
            } else if (deviceMappings.isEmpty()) {
                activeMapping = null;
            }
        }
    }
    
    /**
     * 清空所有设备映射
     */
    public void clearDeviceMappings() {
        deviceMappings.clear();
        activeMapping = null;
    }
    
    /**
     * 获取所有设备映射
     * @return 设备映射列表
     */
    public List<DeviceMapping> getDeviceMappings() {
        return new ArrayList<>(deviceMappings);
    }
    
    /**
     * 设置当前活动的设备映射
     * @param mapping 设备映射
     */
    public void setActiveMapping(DeviceMapping mapping) {
        if (deviceMappings.contains(mapping)) {
            activeMapping = mapping;
        }
    }
    
    /**
     * 获取当前活动的设备映射
     * @return 当前活动的设备映射
     */
    public DeviceMapping getActiveMapping() {
        return activeMapping;
    }
    
    /**
     * 应用控制动作到输入状态
     * @param actions 控制动作列表
     * @param inputState 输入状态
     */
    public void applyActionsToState(List<ControlAction> actions, InputState inputState) {
        if (activeMapping != null) {
            activeMapping.applyActionsToState(actions, inputState);
        }
    }
    
    /**
     * 应用单个控制动作到输入状态
     * @param action 控制动作
     * @param inputState 输入状态
     */
    public void applyActionToState(ControlAction action, InputState inputState) {
        if (activeMapping != null) {
            activeMapping.applyActionToState(action, inputState);
        }
    }
    
    /**
     * 清除输入状态
     * @param inputState 输入状态
     */
    public void clearState(InputState inputState) {
        if (activeMapping != null) {
            activeMapping.clearPreviousState(inputState);
        }
    }
    
    /**
     * 获取设备映射数量
     * @return 映射数量
     */
    public int getMappingCount() {
        return deviceMappings.size();
    }
    
    /**
     * 根据映射类型获取设备映射
     * @param type 映射类型
     * @return 符合条件的映射列表
     */
    public List<DeviceMapping> getMappingsByType(DeviceMapping.MappingType type) {
        List<DeviceMapping> result = new ArrayList<>();
        for (DeviceMapping mapping : deviceMappings) {
            if (mapping != null && type.equals(mapping.getMappingType())) {
                result.add(mapping);
            }
        }
        return result;
    }
    
    /**
     * 切换到下一个可用的映射
     */
    public void switchToNextMapping() {
        if (deviceMappings.size() <= 1) {
            return; // 只有一个或没有映射，无需切换
        }
        
        int currentIndex = deviceMappings.indexOf(activeMapping);
        int nextIndex = (currentIndex + 1) % deviceMappings.size();
        activeMapping = deviceMappings.get(nextIndex);
    }
    
    /**
     * 切换到上一个可用的映射
     */
    public void switchToPrevMapping() {
        if (deviceMappings.size() <= 1) {
            return; // 只有一个或没有映射，无需切换
        }
        
        int currentIndex = deviceMappings.indexOf(activeMapping);
        int prevIndex = (currentIndex - 1 + deviceMappings.size()) % deviceMappings.size();
        activeMapping = deviceMappings.get(prevIndex);
    }
}