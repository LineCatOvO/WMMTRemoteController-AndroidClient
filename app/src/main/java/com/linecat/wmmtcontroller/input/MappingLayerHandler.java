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
    
    // 用于控制日志打印频率的变量
    private static long lastMappingLogTime = 0;
    private static final long MAPPING_LOG_INTERVAL = 2000; // 2秒间隔
    private static int mappingProcessedCount = 0;
    private static int regionProcessedCount = 0;
    
    /**
     * 处理 Mapping 层
     */
    public void process(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        if (layout == null) {
            return;
        }
        
        // 获取所有包含映射信息的元素（包括传统MAPPING类型和简化格式中的UI类型）
        List<Region> mappingElements = new ArrayList<>();
        for (Region region : layout.getRegions()) {
            // 传统MAPPING类型的区域
            if (region.getType() == Region.RegionType.MAPPING) {
                mappingElements.add(region);
            }
            // 或者包含映射信息的UI类型区域（简化格式）
            else if (region.getType() == Region.RegionType.BUTTON || 
                     region.getType() == Region.RegionType.AXIS || 
                     region.getType() == Region.RegionType.GYROSCOPE) {
                // 检查是否有映射信息
                if (region.getMappingButton() != null || region.getMappingKey() != null || region.getMappingAxis() != null) {
                    mappingElements.add(region);
                }
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
                regionProcessedCount++; // 统计处理的区域数
            }
            
            mappingProcessedCount++; // 统计处理次数
        }
        
        // 按时间间隔打印日志汇总
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMappingLogTime >= MAPPING_LOG_INTERVAL) {
            
            // 重置计数器
            mappingProcessedCount = 0;
            regionProcessedCount = 0;
            lastMappingLogTime = currentTime;
        }
    }
    
    /**
     * 处理 Mapping 元素
     */
    private void processMappingElement(Region region, RawInput rawInput, InputState inputState) {
        // 检查映射类型 - 首先检查区域是否是传统的MAPPING类型，否则基于映射属性判断
        if (region.getType() == Region.RegionType.MAPPING) {
            // 传统MAPPING类型的处理
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
        } else {
            // 简化格式：基于映射属性判断
            if (region.getMappingKey() != null) {
                processKeyboardMapping(region, rawInput, inputState);
            } else if (region.getMappingButton() != null) {
                processGamepadMapping(region, rawInput, inputState);
            } else if (region.getMappingAxis() != null) {
                // 轴映射也被视为游戏手柄映射的一种
                processGamepadMapping(region, rawInput, inputState);
            } else if (region.getCustomMappingTarget() != null) {
                processCustomMapping(region, rawInput, inputState);
            } else {
                Log.w(TAG, "Region has no mapping information: " + region.getId());
            }
        }
    }
    
    /**
     * 处理键盘映射
     */
    private void processKeyboardMapping(Region region, RawInput rawInput, InputState inputState) {
        // 处理键盘映射
        String keyCode = region.getMappingKey();
        // 从 RawInput 获取当前按钮状态，使用区域ID作为键
        boolean isPressed = rawInput.getGamepad().getButtons().getOrDefault(region.getId(), false);
        
        // 更新输入状态
        if (keyCode != null) {
            if (isPressed) {
                inputState.getKeyboard().add(keyCode);
            } else {
                inputState.getKeyboard().remove(keyCode);
            }
        }
        
        // 添加日志频率控制
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMappingLogTime >= MAPPING_LOG_INTERVAL) {
            Log.d(TAG, "Keyboard mapping processed: " + region.getId() + ", key: " + keyCode + ", pressed: " + isPressed);
        }
    }
    
    /**
     * 处理游戏手柄映射
     */
    private void processGamepadMapping(Region region, RawInput rawInput, InputState inputState) {
        // 处理游戏手柄映射
        String axis = region.getMappingAxis();
        String button = region.getMappingButton();
        
        // 从 RawInput 获取当前按钮状态，使用区域ID作为键
        boolean buttonPressed = rawInput.getGamepad().getButtons().getOrDefault(region.getId(), false);
        
        // 应用输出范围、曲线等
        float axisValue = 0.0f; // 暂时设为0，如果需要轴映射则应从相应数据源获取
        axisValue = applyOutputRange(axisValue, region.getOutputRange());
        axisValue = applyCurve(axisValue, region.getCurve());
        
        // 对于游戏手柄按钮，将其添加到游戏手柄状态中
        if (button != null && buttonPressed) {
            inputState.addGamepadButton(button);
        } else if (button != null && !buttonPressed) {
            inputState.removeGamepadButton(button); // 确保松开按钮时从游戏手柄状态中移除
        }
        
        // 添加日志频率控制
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMappingLogTime >= MAPPING_LOG_INTERVAL) {

        }
    }
    
    /**
     * 处理自定义映射
     */
    private void processCustomMapping(Region region, RawInput rawInput, InputState inputState) {
        // 处理自定义映射
        String customTarget = region.getCustomMappingTarget();
        // 从 RawInput 获取当前按钮状态作为示例值，使用区域ID作为键
        Object customValue = rawInput.getGamepad().getButtons().getOrDefault(region.getId(), false);
        
        // 更新输入状态 - 这里可以根据具体需求进行处理
        // 示例：将布尔值存储到自定义映射中
        // inputState.setCustomMapping(customTarget, customValue);
        
        // 添加日志频率控制
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMappingLogTime >= MAPPING_LOG_INTERVAL) {
            Log.d(TAG, "Custom mapping processed: " + region.getId() + ", target: " + customTarget + ", value: " + customValue);
        }
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
