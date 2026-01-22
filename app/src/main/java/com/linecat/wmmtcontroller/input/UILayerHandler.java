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
 * UI 层处理器
 * 负责：
 * 1. 接收原始输入（触控、陀螺仪）
 * 2. 执行传感器级输入处理
 * 3. 输出归一化后的抽象值
 * 4. 将结果绑定到 Operation
 */
public class UILayerHandler {
    private static final String TAG = "UILayerHandler";
    
    /**
     * 处理 UI 层输入
     */
    public void process(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        if (layout == null) {
            return;
        }
        
        // 处理触控输入
        processTouchInput(rawInput, layout, inputState);
        
        // 处理陀螺仪输入
        processGyroInput(rawInput, layout, inputState);
        
        // 处理其他输入源
    }
    
    /**
     * 处理触控输入
     */
    private void processTouchInput(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        if (rawInput.isTouchPressed()) {
            // 归一化触摸坐标
            float normalizedX = rawInput.getTouchX() / layout.getScreenWidth();
            float normalizedY = rawInput.getTouchY() / layout.getScreenHeight();
            
            // 确保坐标在0.0-1.0范围内
            normalizedX = Math.max(0f, Math.min(1f, normalizedX));
            normalizedY = Math.max(0f, Math.min(1f, normalizedY));
            
            // 按 zIndex 分组处理
            Map<Integer, List<Region>> zIndexGroups = new HashMap<>();
            for (Region region : layout.getRegions()) {
                if (region.hitTest(normalizedX, normalizedY)) {
                    zIndexGroups.computeIfAbsent(region.getZIndex(), k -> new ArrayList<>()).add(region);
                }
            }
            
            // 处理最高 zIndex 组
            if (!zIndexGroups.isEmpty()) {
                int highestZIndex = Collections.max(zIndexGroups.keySet());
                List<Region> highestZIndexRegions = zIndexGroups.get(highestZIndex);
                
                // 相同 zIndex 结果叠加
                for (Region region : highestZIndexRegions) {
                    processUIElement(region, normalizedX, normalizedY, inputState);
                }
                
                Log.d(TAG, "Processed " + highestZIndexRegions.size() + " regions with zIndex " + highestZIndex);
            } else {
                Log.d(TAG, "Touch input outside any region");
            }
        }
    }
    
    /**
     * 处理陀螺仪输入
     */
    private void processGyroInput(RawInput rawInput, LayoutSnapshot layout, InputState inputState) {
        // 处理陀螺仪输入
        float roll = rawInput.getGyroRoll();
        float pitch = rawInput.getGyroPitch();
        float yaw = rawInput.getGyroYaw();
        
        // 归一化陀螺仪数据
        float normalizedRoll = roll / 180f;
        float normalizedPitch = pitch / 180f;
        float normalizedYaw = yaw / 180f;
        
        // 确保值在 -1.0 到 1.0 范围内
        normalizedRoll = Math.max(-1f, Math.min(1f, normalizedRoll));
        normalizedPitch = Math.max(-1f, Math.min(1f, normalizedPitch));
        normalizedYaw = Math.max(-1f, Math.min(1f, normalizedYaw));
        
        // 查找陀螺仪相关的 UI 元素
        List<Region> gyroRegions = new ArrayList<>();
        for (Region region : layout.getRegions()) {
            if (region.getType() == Region.RegionType.GYROSCOPE) {
                gyroRegions.add(region);
            }
        }
        
        // 按 zIndex 分组处理
        Map<Integer, List<Region>> zIndexGroups = new HashMap<>();
        for (Region region : gyroRegions) {
            zIndexGroups.computeIfAbsent(region.getZIndex(), k -> new ArrayList<>()).add(region);
        }
        
        // 处理最高 zIndex 组
        if (!zIndexGroups.isEmpty()) {
            int highestZIndex = Collections.max(zIndexGroups.keySet());
            List<Region> highestZIndexRegions = zIndexGroups.get(highestZIndex);
            
            // 相同 zIndex 结果叠加
            for (Region region : highestZIndexRegions) {
                processGyroRegion(region, normalizedRoll, normalizedPitch, normalizedYaw, inputState);
            }
            
            Log.d(TAG, "Processed " + highestZIndexRegions.size() + " gyro regions with zIndex " + highestZIndex);
        }
    }
    
    /**
     * 处理 UI 元素
     */
    private void processUIElement(Region region, float normalizedX, float normalizedY, InputState inputState) {
        switch (region.getType()) {
            case BUTTON:
                processButtonRegion(region, normalizedX, normalizedY, inputState);
                break;
            case AXIS:
                processAxisRegion(region, normalizedX, normalizedY, inputState);
                break;
            case GESTURE:
                processGestureRegion(region, normalizedX, normalizedY, inputState);
                break;
            default:
                Log.w(TAG, "Unknown region type: " + region.getType());
        }
    }
    
    /**
     * 处理按钮区域
     */
    private void processButtonRegion(Region region, float normalizedX, float normalizedY, InputState inputState) {
        // 处理按钮区域
        Log.d(TAG, "Button region processed: " + region.getId());
    }
    
    /**
     * 处理轴区域
     */
    private void processAxisRegion(Region region, float normalizedX, float normalizedY, InputState inputState) {
        // 获取区域中心
        float[] center = region.getCenter();
        
        // 计算相对中心的偏移量
        float deltaX = normalizedX - center[0];
        float deltaY = normalizedY - center[1];
        
        // 计算区域的宽高
        float width = region.getRight() - region.getLeft();
        float height = region.getBottom() - region.getTop();
        
        // 将偏移量归一化到 -1.0 到 1.0 范围
        float valueX = deltaX / (width / 2f);
        float valueY = deltaY / (height / 2f);
        
        // 应用死区过滤
        valueX = applyDeadzone(valueX, region.getDeadzone());
        valueY = applyDeadzone(valueY, region.getDeadzone());
        
        // 限制值在 -1.0 到 1.0 范围内
        valueX = Math.max(-1f, Math.min(1f, valueX));
        valueY = Math.max(-1f, Math.min(1f, valueY));
        
        Log.d(TAG, "Axis region processed: " + region.getId() + ", valueX: " + valueX + ", valueY: " + valueY);
    }
    
    /**
     * 处理手势区域
     */
    private void processGestureRegion(Region region, float normalizedX, float normalizedY, InputState inputState) {
        // 处理手势区域
        Log.d(TAG, "Gesture region processed: " + region.getId());
    }
    
    /**
     * 处理陀螺仪区域
     */
    private void processGyroRegion(Region region, float roll, float pitch, float yaw, InputState inputState) {
        // 应用死区过滤
        float processedRoll = applyDeadzone(roll, region.getDeadzone());
        float processedPitch = applyDeadzone(pitch, region.getDeadzone());
        float processedYaw = applyDeadzone(yaw, region.getDeadzone());
        
        // 限制值在 -1.0 到 1.0 范围内
        processedRoll = Math.max(-1f, Math.min(1f, processedRoll));
        processedPitch = Math.max(-1f, Math.min(1f, processedPitch));
        processedYaw = Math.max(-1f, Math.min(1f, processedYaw));
        
        Log.d(TAG, "Gyro region processed: " + region.getId() + ", roll: " + processedRoll + ", pitch: " + processedPitch + ", yaw: " + processedYaw);
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
     * 重置 UI 层处理器
     */
    public void reset() {
        // 清理 UI 层处理器状态
        Log.d(TAG, "UI layer handler reset");
    }
}
