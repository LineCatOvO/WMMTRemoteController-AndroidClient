package com.linecat.wmmtcontroller.input;

import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping 层处理器
 * 负责：
 * 1. 将抽象语义映射到设备输出
 * 2. 执行设备适配
 */
public class MappingLayerHandler {
    private static final String TAG = "MappingLayerHandler";
    
    /**
     * 处理 Mapping 层
     */
    public void process(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        if (layout == null) {
            return;
        }
        
        // 获取所有 Mapping 元素
        List<Region> mappingElements = new ArrayList<>();
        for (Region region : layout.getRegions()) {
            if (region.getType() == Region.RegionType.MAPPING) {
                mappingElements.add(region);
            }
        }
        
        // 按 zIndex 分组处理
        Map<Integer, List<Region>> zIndexGroups = new HashMap<>();
        for (Region region : mappingElements) {
            zIndexGroups.computeIfAbsent(region.getZIndex(), k -> new ArrayList<>()).add(region);
        }
        
        // 处理最高 zIndex 组
        if (!zIndexGroups.isEmpty()) {
            int highestZIndex = Collections.max(zIndexGroups.keySet());
            List<Region> highestZIndexRegions = zIndexGroups.get(highestZIndex);
            
            // 相同 zIndex 结果叠加
            for (Region region : highestZIndexRegions) {
                processMappingElement(region, rawInput, inputState);
            }
            
            Log.d(TAG, "Processed " + highestZIndexRegions.size() + " mapping regions with zIndex " + highestZIndex);
        }
    }
    
    /**
     * 处理 Mapping 元素
     */
    private void processMappingElement(Region region, RawInput rawInput, InputState inputState) {
        switch (region.getMappingType()) {
            case KEYBOARD:
                processKeyboardMapping(region, rawInput, inputState);
                break;
            case GAMEPAD:
                processGamepadMapping(region, rawInput, inputState);
                break;
            case CUSTOM:
                processCustomMapping(region, rawInput, inputState);
                break;
            default:
                Log.w(TAG, "Unknown mapping type: " + region.getMappingType());
        }
    }
    
    /**
     * 处理键盘映射
     */
    private void processKeyboardMapping(Region region, RawInput rawInput, InputState inputState) {
        // 处理键盘映射
        String keyCode = region.getMappingKey();
        boolean isPressed = false; // 实际应从 Operation 层结果获取
        
        // 更新输入状态
        // inputState.setKeyPressed(keyCode, isPressed);
        
        Log.d(TAG, "Keyboard mapping processed: " + region.getId() + ", key: " + keyCode + ", pressed: " + isPressed);
    }
    
    /**
     * 处理游戏手柄映射
     */
    private void processGamepadMapping(Region region, RawInput rawInput, InputState inputState) {
        // 处理游戏手柄映射
        String axis = region.getMappingAxis();
        String button = region.getMappingButton();
        
        // 从 Operation 层获取值
        float axisValue = 0.0f; // 实际应从 Operation 层结果获取
        boolean buttonPressed = false; // 实际应从 Operation 层结果获取
        
        // 应用输出范围、曲线等
        axisValue = applyOutputRange(axisValue, region.getOutputRange());
        axisValue = applyCurve(axisValue, region.getCurve());
        
        // 更新输入状态
        // if (axis != null) {
        //     inputState.setGamepadAxis(axis, axisValue);
        // }
        // if (button != null) {
        //     inputState.setGamepadButton(button, buttonPressed);
        // }
        
        Log.d(TAG, "Gamepad mapping processed: " + region.getId() + ", axis: " + axis + ", value: " + axisValue + ", button: " + button + ", pressed: " + buttonPressed);
    }
    
    /**
     * 处理自定义映射
     */
    private void processCustomMapping(Region region, RawInput rawInput, InputState inputState) {
        // 处理自定义映射
        String customTarget = region.getCustomMappingTarget();
        Object customValue = null; // 实际应从 Operation 层结果获取
        
        // 更新输入状态
        // inputState.setCustomMapping(customTarget, customValue);
        
        Log.d(TAG, "Custom mapping processed: " + region.getId() + ", target: " + customTarget + ", value: " + customValue);
    }
    
    /**
     * 应用输出范围
     */
    private float applyOutputRange(float value, float[] outputRange) {
        if (outputRange == null || outputRange.length != 2) {
            return value;
        }
        
        float min = outputRange[0];
        float max = outputRange[1];
        
        // 将输入值（-1.0 到 1.0）映射到输出范围
        return min + (value + 1.0f) * (max - min) / 2.0f;
    }
    
    /**
     * 应用灵敏度曲线
     */
    private float applyCurve(float value, String curveType) {
        // 应用不同类型的灵敏度曲线
        switch (curveType) {
            case "linear":
                return value;
            case "exponential":
                return (float) Math.pow(value, 2.0) * Math.signum(value);
            case "logarithmic":
                return (float) Math.log10(Math.abs(value) * 9 + 1) * Math.signum(value) * 0.5f;
            case "sine":
                return (float) Math.sin(value * Math.PI / 2);
            default:
                return value;
        }
    }
    
    /**
     * 重置 Mapping 层处理器
     */
    public void reset() {
        // 清理 Mapping 层处理器状态
        Log.d(TAG, "Mapping layer handler reset");
    }
}
