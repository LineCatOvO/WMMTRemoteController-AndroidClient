package com.linecat.wmmtcontroller.migration;

import com.linecat.wmmtcontroller.control.ControlNode;
import com.linecat.wmmtcontroller.control.ButtonControlNode;
import com.linecat.wmmtcontroller.control.AxisControlNode;
import com.linecat.wmmtcontroller.control.GyroControlNode;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.input.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * 布局到控制节点的转换器
 * 用于将现有的Region布局转换为新的ControlNode体系
 */
public class LayoutToControlNodeConverter {
    
    /**
     * 将布局快照转换为控制节点列表
     * @param layoutSnapshot 原始布局快照
     * @return 控制节点列表
     */
    public static List<ControlNode> convertLayoutToControlNodes(LayoutSnapshot layoutSnapshot) {
        List<ControlNode> controlNodes = new ArrayList<>();
        
        if (layoutSnapshot == null || layoutSnapshot.getRegions() == null) {
            return controlNodes;
        }
        
        for (Region region : layoutSnapshot.getRegions()) {
            ControlNode controlNode = convertRegionToControlNode(region);
            if (controlNode != null) {
                controlNodes.add(controlNode);
            }
        }
        
        return controlNodes;
    }
    
    /**
     * 将单个区域转换为控制节点
     * @param region 原始区域
     * @return 控制节点
     */
    private static ControlNode convertRegionToControlNode(Region region) {
        // 根据区域类型创建对应的控制节点
        switch (region.getType()) {
            case BUTTON:
                return new ButtonControlNode(
                    region.getId(),
                    region.getId(), // 使用ID作为名称
                    region.getLeft(),
                    region.getTop(),
                    region.getRight() - region.getLeft(), // 宽度
                    region.getBottom() - region.getTop(), // 高度
                    region.getOperationType() != null ? region.getOperationType().name() : "BUTTON"
                );
            case AXIS:
                return new AxisControlNode(
                    region.getId(),
                    region.getId(),
                    region.getLeft(),
                    region.getTop(),
                    region.getRight() - region.getLeft(), // 宽度
                    region.getBottom() - region.getTop(), // 高度
                    region.getRange() != null && region.getRange().length > 0 ? region.getRange()[0] : -1.0f, // 最小值
                    region.getRange() != null && region.getRange().length > 1 ? region.getRange()[1] : 1.0f, // 最大值
                    region.getOperationType() != null ? region.getOperationType().name() : "AXIS"
                );
            case GESTURE:
                // TODO: 实现手势控制节点
                return null;
            case GYROSCOPE:
                return new GyroControlNode(
                    region.getId(),
                    region.getId(),
                    region.getLeft(),
                    region.getTop(),
                    region.getRight() - region.getLeft(), // 宽度
                    region.getBottom() - region.getTop(), // 高度
                    region.getOperationType() != null ? region.getOperationType().name() : "GYRO",
                    region.getOutputRange() != null && region.getOutputRange().length > 0 ? region.getOutputRange()[0] : 1.0f // 灵敏度
                );
            case OPERATION:
                // TODO: 实现操作控制节点
                return null;
            case MAPPING:
                // TODO: 实现映射控制节点
                return null;
            default:
                return new ButtonControlNode(
                    region.getId(),
                    region.getId(),
                    region.getLeft(),
                    region.getTop(),
                    region.getRight() - region.getLeft(), // 宽度
                    region.getBottom() - region.getTop(), // 高度
                    region.getOperationType() != null ? region.getOperationType().name() : "CUSTOM"
                );
        }
    }
    
    /**
     * 将控制节点列表转换回布局快照（反向转换）
     * @param controlNodes 控制节点列表
     * @return 布局快照
     */
    public static LayoutSnapshot convertControlNodesToLayout(List<ControlNode> controlNodes) {
        if (controlNodes == null) {
            return new LayoutSnapshot(new ArrayList<>());
        }
        
        List<Region> regions = new ArrayList<>();
        
        for (ControlNode controlNode : controlNodes) {
            Region region = convertControlNodeToRegion(controlNode);
            if (region != null) {
                regions.add(region);
            }
        }
        
        return new LayoutSnapshot(regions);
    }
    
    /**
     * 将单个控制节点转换为区域（反向转换）
     * @param controlNode 控制节点
     * @return 区域
     */
    private static Region convertControlNodeToRegion(ControlNode controlNode) {
        float[] bounds = controlNode.getBounds();
        
        // 根据控制节点类型创建对应的区域
        switch (controlNode.getNodeType()) {
            case BUTTON:
                // 使用反射或工厂模式来创建具体类型的区域
                return new Region(
                    controlNode.getNodeId(),
                    Region.RegionType.BUTTON,
                    bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3],
                    controlNode instanceof ButtonControlNode ? 10 : 0, // zIndex
                    controlNode instanceof ButtonControlNode ? 0.0f : 0.1f, // deadzone
                    "linear", // curve
                    new float[]{-1.0f, 1.0f}, // range
                    new float[]{-1.0f, 1.0f}, // outputRange
                    Region.OperationType.BUTTON, // operationType
                    Region.MappingType.KEYBOARD, // mappingType
                    null, // mappingKey
                    null, // mappingAxis
                    null, // mappingButton
                    null, // customMappingTarget
                    null // customData
                );
            default:
                return new Region(
                    controlNode.getNodeId(),
                    Region.RegionType.BUTTON,
                    bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3],
                    0, // zIndex
                    0.0f, // deadzone
                    "linear", // curve
                    new float[]{-1.0f, 1.0f}, // range
                    new float[]{-1.0f, 1.0f}, // outputRange
                    Region.OperationType.CUSTOM, // operationType
                    Region.MappingType.CUSTOM, // mappingType
                    null, // mappingKey
                    null, // mappingAxis
                    null, // mappingButton
                    null, // customMappingTarget
                    null // customData
                );
        }
    }
}