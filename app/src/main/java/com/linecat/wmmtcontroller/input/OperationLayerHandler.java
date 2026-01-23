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
 * Operation 层处理器
 * 负责：
 * 1. 定义抽象控制语义（方向盘、油门、刹车、按钮类操作）
 * 2. 执行抽象控制语义处理
 */
public class OperationLayerHandler {
    private static final String TAG = "OperationLayerHandler";
    
    /**
     * 处理 Operation 层
     */
    public void process(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        if (layout == null) {
            return;
        }
        
        // 获取所有 Operation 元素
        List<Region> operationElements = new ArrayList<>();
        for (Region region : layout.getRegions()) {
            if (region.getType() == Region.RegionType.OPERATION) {
                operationElements.add(region);
            }
        }
        
        // 按 zIndex 分组处理
        Map<Integer, List<Region>> zIndexGroups = new HashMap<>();
        for (Region region : operationElements) {
            zIndexGroups.computeIfAbsent(region.getZIndex(), k -> new ArrayList<>()).add(region);
        }
        
        // 处理最高 zIndex 组
        if (!zIndexGroups.isEmpty()) {
            int highestZIndex = Collections.max(zIndexGroups.keySet());
            List<Region> highestZIndexRegions = zIndexGroups.get(highestZIndex);
            
            // 相同 zIndex 结果叠加
            for (Region region : highestZIndexRegions) {
                processOperationElement(region, rawInput, inputState);
            }
            
            Log.d(TAG, "Processed " + highestZIndexRegions.size() + " operation regions with zIndex " + highestZIndex);
        }
    }
    
    /**
     * 处理 Operation 元素
     */
    private void processOperationElement(Region region, RawInput rawInput, InputState inputState) {
        switch (region.getOperationType()) {
            case STEERING:
                processSteeringOperation(region, rawInput, inputState);
                break;
            case THROTTLE:
                processThrottleOperation(region, rawInput, inputState);
                break;
            case BRAKE:
                processBrakeOperation(region, rawInput, inputState);
                break;
            case BUTTON:
                processButtonOperation(region, rawInput, inputState);
                break;
            default:
                Log.w(TAG, "Unknown operation type: " + region.getOperationType());
        }
    }
    
    /**
     * 处理方向盘操作
     */
    private void processSteeringOperation(Region region, RawInput rawInput, InputState inputState) {
        // 处理方向盘操作
        // 应用灵敏度曲线、死区等
        
        // 示例：从 UI 层获取归一化值并应用处理
        float normalizedValue = 0.0f; // 实际应从 UI 层结果获取
        float processedValue = applyCurve(normalizedValue, region.getCurve());
        processedValue = applyDeadzone(processedValue, region.getDeadzone());
        processedValue = applyRange(processedValue, region.getRange());
        
        // 更新输入状态
        // inputState.setSteering(processedValue);
        
        Log.d(TAG, "Steering operation processed: " + region.getId() + ", value: " + processedValue);
    }
    
    /**
     * 处理油门操作
     */
    private void processThrottleOperation(Region region, RawInput rawInput, InputState inputState) {
        // 处理油门操作
        float normalizedValue = 0.0f; // 实际应从 UI 层结果获取
        float processedValue = applyCurve(normalizedValue, region.getCurve());
        processedValue = applyDeadzone(processedValue, region.getDeadzone());
        processedValue = applyRange(processedValue, region.getRange());
        
        // 更新输入状态
        // inputState.setThrottle(processedValue);
        
        Log.d(TAG, "Throttle operation processed: " + region.getId() + ", value: " + processedValue);
    }
    
    /**
     * 处理刹车操作
     */
    private void processBrakeOperation(Region region, RawInput rawInput, InputState inputState) {
        // 处理刹车操作
        float normalizedValue = 0.0f; // 实际应从 UI 层结果获取
        float processedValue = applyCurve(normalizedValue, region.getCurve());
        processedValue = applyDeadzone(processedValue, region.getDeadzone());
        processedValue = applyRange(processedValue, region.getRange());
        
        // 更新输入状态
        // inputState.setBrake(processedValue);
        
        Log.d(TAG, "Brake operation processed: " + region.getId() + ", value: " + processedValue);
    }
    
    /**
     * 处理按钮操作
     */
    private void processButtonOperation(Region region, RawInput rawInput, InputState inputState) {
        // 处理按钮操作
        // 从 RawInput 中获取按钮状态，按钮ID应该是区域ID
        boolean isPressed = rawInput.getGamepad().getButtons().getOrDefault(region.getId(), false);
        
        Log.d(TAG, "Button operation processed: " + region.getId() + ", pressed: " + isPressed);
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
     * 应用死区过滤
     */
    private float applyDeadzone(float value, float deadzone) {
        if (Math.abs(value) < deadzone) {
            return 0f;
        } else {
            // 对超出死区的值进行归一化
            return (value - Math.signum(value) * deadzone) / (1f - deadzone);
        }
    }
    
    /**
     * 应用范围限制
     */
    private float applyRange(float value, float[] range) {
        if (range == null || range.length != 2) {
            return value;
        }
        
        float min = range[0];
        float max = range[1];
        
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 重置 Operation 层处理器
     */
    public void reset() {
        // 清理 Operation 层处理器状态
        Log.d(TAG, "Operation layer handler reset");
    }
}
