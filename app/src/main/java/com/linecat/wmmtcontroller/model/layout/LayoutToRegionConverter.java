package com.linecat.wmmtcontroller.model.layout;

import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.input.Region;
import com.linecat.wmmtcontroller.model.DeviceMapping;
import java.util.ArrayList;
import java.util.List;

/**
 * 布局配置到区域的转换器
 * 将新的 LayoutConfiguration 对象转换为现有的 Region 列表格式
 */
public class LayoutToRegionConverter {

    /**
     * 将 LayoutConfiguration 转换为 Region 列表
     *
     * @param config 布局配置对象
     * @return Region 列表
     */
    public static List<Region> convertToRegions(LayoutConfiguration config) {
        List<Region> regions = new ArrayList<>();

        if (config.getUi() != null) {
            // 将 UI 元素转换为 Region
            for (int i = 0; i < config.getUi().size(); i++) {
                UiElement uiElement = config.getUi().get(i);
                Region region = convertUiElementToRegion(uiElement, i);
                if (region != null) {
                    regions.add(region);
                }
            }
        }

        if (config.getOperation() != null) {
            // 将操作转换为 Region
            for (int i = 0; i < config.getOperation().size(); i++) {
                Operation operation = config.getOperation().get(i);
                Region region = convertOperationToRegion(operation, i);
                if (region != null) {
                    regions.add(region);
                }
            }
        }

        // 注意：Mapping 通常不需要转换为 Region，因为它只是定义了映射关系
        // 实际的映射是在运行时处理的

        return regions;
    }

    /**
     * 将 LayoutConfiguration 转换为 LayoutSnapshot
     *
     * @param config 布局配置对象
     * @return LayoutSnapshot 对象
     */
    public static LayoutSnapshot convertToLayoutSnapshot(LayoutConfiguration config) {
        List<Region> regions = convertToRegions(config);
        return new LayoutSnapshot(regions);
    }

    /**
     * 将 UI 元素转换为 Region
     *
     * @param uiElement UI 元素
     * @param index     索引，用于设置 zIndex
     * @return Region 对象
     */
    private static Region convertUiElementToRegion(UiElement uiElement, int index) {
        if (uiElement == null || uiElement.getSize() == null || uiElement.getOffset() == null) {
            return null;
        }

        // 计算区域坐标
        float centerX = uiElement.getOffset().getX().floatValue();
        float centerY = uiElement.getOffset().getY().floatValue();

        // 根据锚点和尺寸计算边界
        float halfWidth = (float) (uiElement.getSize().getWidth() != null ? uiElement.getSize().getWidth() / 2.0 : 0.0);
        float halfHeight = (float) (uiElement.getSize().getHeight() != null ? uiElement.getSize().getHeight() / 2.0 : 0.0);

        // 根据锚点类型调整中心点位置
        float[] anchorAdjustment = calculateAnchorAdjustment(uiElement.getAnchor());
        centerX += anchorAdjustment[0];
        centerY += anchorAdjustment[1];

        // 计算边界
        float left = centerX - halfWidth;
        float top = centerY - halfHeight;
        float right = centerX + halfWidth;
        float bottom = centerY + halfHeight;

        // 确保边界在有效范围内
        left = Math.max(0.0f, Math.min(1.0f, left));
        top = Math.max(0.0f, Math.min(1.0f, top));
        right = Math.max(0.0f, Math.min(1.0f, right));
        bottom = Math.max(0.0f, Math.min(1.0f, bottom));

        // 确定区域类型
        Region.RegionType regionType = mapUiElementType(uiElement);

        // 创建区域
        return new Region(
                uiElement.getId(),                           // id
                regionType,                                  // type
                left,                                        // left
                top,                                         // top
                right,                                       // right
                bottom,                                      // bottom
                index,                                       // zIndex
                0.0f,                                        // deadzone
                "linear",                                    // curve
                null,                                        // range
                null,                                        // outputRange
                null,                                        // operationType
                null,                                        // mappingType
                null,                                        // mappingKey
                null,                                        // mappingAxis
                null,                                        // mappingButton
                null,                                        // customMappingTarget
                uiElement                                    // customData (保存整个 UI 元素对象)
        );
    }

    /**
     * 计算锚点调整值
     *
     * @param anchor 锚点类型
     * @return [xAdjustment, yAdjustment]
     */
    private static float[] calculateAnchorAdjustment(String anchor) {
        if (anchor == null) {
            return new float[]{0.5f, 0.5f}; // 默认中心
        }

        switch (anchor) {
            case "top-left":
                return new float[]{0.0f, 0.0f};
            case "top-center":
                return new float[]{0.5f, 0.0f};
            case "top-right":
                return new float[]{1.0f, 0.0f};
            case "center-left":
                return new float[]{0.0f, 0.5f};
            case "center":
                return new float[]{0.5f, 0.5f};
            case "center-right":
                return new float[]{1.0f, 0.5f};
            case "bottom-left":
                return new float[]{0.0f, 1.0f};
            case "bottom-center":
                return new float[]{0.5f, 1.0f};
            case "bottom-right":
                return new float[]{1.0f, 1.0f};
            default:
                return new float[]{0.5f, 0.5f}; // 默认中心
        }
    }

    /**
     * 映射 UI 元素类型到区域类型
     *
     * @param uiElement UI 元素
     * @return 区域类型
     */
    private static Region.RegionType mapUiElementType(UiElement uiElement) {
        // 这里可以根据 UI 元素的特定属性来判断区域类型
        // 如果没有明确的类型指示，默认为 BUTTON
        return Region.RegionType.BUTTON;
    }

    /**
     * 将操作转换为 Region
     *
     * @param operation 操作对象
     * @param index     索引，用于设置 zIndex
     * @return Region 对象
     */
    private static Region convertOperationToRegion(Operation operation, int index) {
        if (operation == null) {
            return null;
        }

        // 对于操作，我们可以创建一个虚拟区域，或者根据需要创建特定类型的区域
        // 这里我们创建一个覆盖整个屏幕的区域，但实际使用中可能需要不同的策略

        // 创建操作类型的区域
        Region.OperationType operationType = mapOperationType(operation.getType());

        return new Region(
                operation.getId(),                           // id
                Region.RegionType.OPERATION,               // type
                0.0f,                                      // left
                0.0f,                                      // top
                1.0f,                                      // right
                1.0f,                                      // bottom
                index,                                     // zIndex
                (float) (operation.getDefaultVal() != null ? operation.getDefaultVal() : 0.0), // deadzone
                "linear",                                  // curve
                operation.getRange() != null ? new float[]{operation.getRange().getMin().floatValue(), operation.getRange().getMax().floatValue()} : null, // range
                null,                                      // outputRange
                operationType,                             // operationType
                null,                                      // mappingType
                null,                                      // mappingKey
                null,                                      // mappingAxis
                null,                                      // mappingButton
                null,                                      // customMappingTarget
                operation                                  // customData (保存整个操作对象)
        );
    }

    /**
     * 映射操作类型字符串到枚举
     *
     * @param type 类型字符串
     * @return OperationType 枚举
     */
    private static Region.OperationType mapOperationType(String type) {
        if (type == null) {
            return null;
        }

        switch (type.toLowerCase()) {
            case "steering":
                return Region.OperationType.STEERING;
            case "throttle":
                return Region.OperationType.THROTTLE;
            case "brake":
                return Region.OperationType.BRAKE;
            case "button":
                return Region.OperationType.BUTTON;
            case "custom":
                return Region.OperationType.CUSTOM;
            default:
                return Region.OperationType.CUSTOM;
        }
    }
}