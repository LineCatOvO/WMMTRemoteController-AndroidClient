package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 区域解析器
 * 负责管理输入区域定义和解析输入事件对应的区域
 * 是Java输入层的核心组件之一，为InputInterpreter提供区域信息
 */
public class RegionResolver {
    private static final String TAG = "RegionResolver";
    
    // 区域列表，按优先级排序
    private List<Region> regions = Collections.emptyList();
    
    // 当前布局快照
    private LayoutSnapshot currentLayoutSnapshot;
    
    // 布局管理器
    private LayoutManager layoutManager;
    
    /**
     * 构造函数
     * @param context 上下文，用于初始化LayoutManager
     */
    public RegionResolver(Context context) {
        // 初始化布局管理器
        this.layoutManager = new LayoutManager(context);
        
        // 初始化空布局快照
        this.currentLayoutSnapshot = new LayoutSnapshot(new ArrayList<>());
    }
    
    /**
     * 构造函数（用于测试或无上下文场景）
     */
    public RegionResolver() {
        // 初始化空布局管理器
        this.layoutManager = null;
        
        // 初始化空布局快照
        this.currentLayoutSnapshot = new LayoutSnapshot(new ArrayList<>());
    }
    
    /**
     * 更新区域定义
     * @param newRegions 新的区域列表
     */
    public void updateRegions(List<Region> newRegions) {
        if (newRegions == null) {
            newRegions = new ArrayList<>();
        }
        
        // 按zIndex排序，zIndex高的排在前面
        List<Region> sortedRegions = new ArrayList<>(newRegions);
        sortedRegions.sort(Comparator.comparingInt(Region::getZIndex).reversed());
        
        // 更新区域列表
        this.regions = Collections.unmodifiableList(sortedRegions);
        
        // 更新布局快照
        this.currentLayoutSnapshot = new LayoutSnapshot(this.regions);
        
        Log.d(TAG, "Regions updated, count: " + this.regions.size());
    }
    
    /**
     * 获取当前布局快照
     * @return 当前布局快照
     */
    public LayoutSnapshot getCurrentLayoutSnapshot() {
        return currentLayoutSnapshot;
    }
    
    /**
     * 查找指定ID的区域
     * @param regionId 区域ID
     * @return 区域对象，找不到返回null
     */
    public Region findRegionById(String regionId) {
        if (regionId == null) {
            return null;
        }
        
        for (Region region : regions) {
            if (region.getId().equals(regionId)) {
                return region;
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有区域
     * @return 所有区域的不可修改列表
     */
    public List<Region> getAllRegions() {
        return regions;
    }
    
    /**
     * 检查是否存在区域
     * @return 是否存在区域
     */
    public boolean hasRegions() {
        return !regions.isEmpty();
    }
    
    /**
     * 从布局项目加载区域定义
     * @param projectId 布局项目ID
     * @return 是否加载成功
     */
    public boolean loadRegionsFromLayoutProject(String projectId) {
        if (layoutManager == null) {
            Log.e(TAG, "LayoutManager is not initialized");
            return false;
        }
        
        // 读取布局项目的main.json文件
        String mainJson = layoutManager.readLayoutProject(projectId);
        if (mainJson == null) {
            Log.e(TAG, "Failed to read layout project: " + projectId);
            return false;
        }
        
        // 解析main.json并更新区域
        List<Region> loadedRegions = parseRegionsFromJson(mainJson);
        if (loadedRegions == null || loadedRegions.isEmpty()) {
            Log.e(TAG, "No regions found in layout project: " + projectId);
            return false;
        }
        
        // 更新区域列表
        updateRegions(loadedRegions);
        Log.d(TAG, "Loaded " + loadedRegions.size() + " regions from project: " + projectId);
        return true;
    }
    
    /**
     * 从JSON字符串解析区域定义
     * @param jsonStr JSON字符串
     * @return 解析出的区域列表，解析失败返回null
     */
    private List<Region> parseRegionsFromJson(String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            
            // 获取regions数组
            JSONArray regionsArray = jsonObj.optJSONArray("regions");
            if (regionsArray == null) {
                Log.e(TAG, "regions array not found in JSON");
                return null;
            }
            
            List<Region> parsedRegions = new ArrayList<>();
            
            // 遍历regions数组
            for (int i = 0; i < regionsArray.length(); i++) {
                JSONObject regionObj = regionsArray.getJSONObject(i);
                
                // 解析区域属性
                String id = regionObj.getString("id");
                String typeStr = regionObj.getString("type");
                Region.RegionType type = Region.RegionType.valueOf(typeStr.toUpperCase());
                
                // 解析区域坐标（归一化坐标，0.0-1.0）
                float left = (float) regionObj.optDouble("left", 0.0);
                float top = (float) regionObj.optDouble("top", 0.0);
                float right = (float) regionObj.optDouble("right", 1.0);
                float bottom = (float) regionObj.optDouble("bottom", 1.0);
                
                // 解析优先级
                int priority = regionObj.optInt("priority", 0);
                
                // 解析自定义数据
                JSONObject customDataObj = regionObj.optJSONObject("customData");
                Object customData = customDataObj != null ? customDataObj : null;
                
                // 创建Region对象，为缺少的参数提供默认值
                Region region = new Region(
                        id, type, left, top, right, bottom, priority, 
                        0.0f, // deadzone
                        "linear", // curve
                        new float[]{0.0f, 1.0f}, // range
                        new float[]{0.0f, 1.0f}, // outputRange
                        null, // operationType
                        null, // mappingType
                        null, // mappingKey
                        null, // mappingAxis
                        null, // mappingButton
                        null, // customMappingTarget
                        customData // customData
                );
                parsedRegions.add(region);
            }
            
            return parsedRegions;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing regions from JSON: " + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid region type: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取布局管理器
     * @return 布局管理器实例
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    /**
     * 重置区域解析器
     */
    public void reset() {
        updateRegions(new ArrayList<>());
    }
}