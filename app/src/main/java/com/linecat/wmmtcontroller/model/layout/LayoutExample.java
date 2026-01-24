package com.linecat.wmmtcontroller.model.layout;

import java.util.Arrays;
import java.util.Collections;

/**
 * 布局配置示例类
 */
public class LayoutExample {

    /**
     * 创建一个示例布局配置
     *
     * @return 示例布局配置
     */
    public static LayoutConfiguration createExampleLayout() {
        LayoutConfiguration config = new LayoutConfiguration();
        config.setVersion("1.0.0");

        // 创建 UI 元素示例
        UiElement throttleElement = new UiElement();
        throttleElement.setId("throttle");
        throttleElement.setAnchor("bottom-right");
        
        Offset throttleOffset = new Offset();
        throttleOffset.setX(0.1);
        throttleOffset.setY(0.2);
        throttleOffset.setUnit("percent");
        throttleElement.setOffset(throttleOffset);

        Size throttleSize = new Size();
        throttleSize.setMode("percent");
        throttleSize.setWidth(0.2);
        throttleSize.setHeight(0.4);
        throttleElement.setSize(throttleSize);

        Hitbox throttleHitbox = new Hitbox();
        throttleHitbox.setShape("rect");
        throttleHitbox.setPadding(5.0);
        throttleElement.setHitbox(throttleHitbox);

        UiElement steeringElement = new UiElement();
        steeringElement.setId("steering");
        steeringElement.setAnchor("top-center");
        
        Offset steeringOffset = new Offset();
        steeringOffset.setX(0.0);
        steeringOffset.setY(0.1);
        steeringOffset.setUnit("percent");
        steeringElement.setOffset(steeringOffset);

        Size steeringSize = new Size();
        steeringSize.setMode("percent");
        steeringSize.setWidth(0.8);
        steeringSize.setHeight(0.3);
        steeringElement.setSize(steeringSize);

        config.setUi(Arrays.asList(throttleElement, steeringElement));

        // 创建操作示例
        Operation throttleOp = new Operation();
        throttleOp.setId("throttle_op");
        throttleOp.setType("axis");
        
        Range throttleRange = new Range();
        throttleRange.setMin(0.0);
        throttleRange.setMax(1.0);
        throttleOp.setRange(throttleRange);
        throttleOp.setDefaultVal(0.0);

        Operation steeringOp = new Operation();
        steeringOp.setId("steering_op");
        steeringOp.setType("axis");
        
        Range steeringRange = new Range();
        steeringRange.setMin(-1.0);
        steeringRange.setMax(1.0);
        steeringOp.setRange(steeringRange);
        steeringOp.setDefaultVal(0.0);

        config.setOperation(Arrays.asList(throttleOp, steeringOp));

        // 创建映射示例
        Mapping throttleMapping = new Mapping();
        throttleMapping.setOperation("throttle_op");
        throttleMapping.setOutput("RT");
        throttleMapping.setTrigger("axis");

        Mapping steeringMapping = new Mapping();
        steeringMapping.setOperation("steering_op");
        steeringMapping.setOutput("LX");
        steeringMapping.setTrigger("axis");

        config.setMapping(Arrays.asList(throttleMapping, steeringMapping));

        return config;
    }

    public static void main(String[] args) {
        LayoutConfiguration example = createExampleLayout();
        String json = LayoutSerializer.serialize(example);
        System.out.println("Generated JSON:");
        System.out.println(json);

        // 测试反序列化
        LayoutConfiguration deserialized = LayoutSerializer.deserialize(json);
        System.out.println("\nDeserialization successful: " + (deserialized != null));
        System.out.println("Version: " + deserialized.getVersion());
        System.out.println("UI Elements count: " + deserialized.getUi().size());
        System.out.println("Operations count: " + deserialized.getOperation().size());
        System.out.println("Mappings count: " + deserialized.getMapping().size());
    }
}