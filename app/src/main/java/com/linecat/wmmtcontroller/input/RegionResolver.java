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
    
    // 默认布局JSON字符串
    private static final String DEFAULT_LAYOUT_JSON = "{\"layoutId\": \"basic_racing_layout\", \"version\": 1, \"description\": \"Basic racing layout with throttle, brake, gear shift and gyro steering\", \"elements\": [{\"id\": \"steering_wheel\", \"type\": \"gyro\", \"displayOnly\": true, \"position\": {\"x\": 0.5, \"y\": 0.15}, \"size\": {\"width\": 0.4, \"height\": 0.25}, \"mapping\": {\"axis\": \"LX\", \"source\": \"gyroscope\", \"sensitivity\": 1.0}}, {\"id\": \"gear_up\", \"type\": \"button\", \"position\": {\"x\": 0.05, \"y\": 0.55}, \"size\": {\"width\": 0.12, \"height\": 0.15}, \"mapping\": {\"button\": \"RB\"}}, {\"id\": \"gear_down\", \"type\": \"button\", \"position\": {\"x\": 0.05, \"y\": 0.72}, \"size\": {\"width\": 0.12, \"height\": 0.15}, \"mapping\": {\"button\": \"LB\"}}, {\"id\": \"brake\", \"type\": \"analog\", \"position\": {\"x\": 0.20, \"y\": 0.60}, \"size\": {\"width\": 0.18, \"height\": 0.30}, \"mapping\": {\"trigger\": \"LT\"}}, {\"id\": \"throttle\", \"type\": \"analog\", \"position\": {\"x\": 0.75, \"y\": 0.60}, \"size\": {\"width\": 0.20, \"height\": 0.30}, \"mapping\": {\"trigger\": \"RT\"}}]} ";
    
    /**
     * 构造函数
     * @param context 上下文，用于初始化LayoutManager
     */
    public RegionResolver(Context context) {
        // 初始化布局管理器
        this.layoutManager = new LayoutManager(context);
        
        // 加载默认布局
        loadDefaultLayout();
    }
    
    /**
     * 构造函数（用于测试或无上下文场景）
     */
    public RegionResolver() {
        // 初始化空布局管理器
        this.layoutManager = null;
        
        // 加载默认布局
        loadDefaultLayout();
    }
    
    /**
     * 加载默认布局
     */
    private void loadDefaultLayout() {
        try {
            List<Region> defaultRegions = parseRegionsFromJson(DEFAULT_LAYOUT_JSON);
            if (defaultRegions != null && !defaultRegions.isEmpty()) {
                updateRegions(defaultRegions);
                Log.d(TAG, "Loaded default layout with " + defaultRegions.size() + " regions");
            } else {
                // 初始化空布局快照
                this.currentLayoutSnapshot = new LayoutSnapshot(new ArrayList<>());
                Log.w(TAG, "Default layout parsing returned no regions");
            }
        } catch (Exception e) {
            // 初始化空布局快照
            this.currentLayoutSnapshot = new LayoutSnapshot(new ArrayList<>());
            Log.e(TAG, "Error loading default layout", e);
        }
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
            
            List<Region> parsedRegions = new ArrayList<>();
            
            // 检查是否为用户提供的新格式（elements数组）
            JSONArray elementsArray = jsonObj.optJSONArray("elements");
            if (elementsArray != null) {
                // 遍历elements数组（新格式）
                for (int i = 0; i < elementsArray.length(); i++) {
                    JSONObject elementObj = elementsArray.getJSONObject(i);
                    
                    // 解析元素属性
                    String id = elementObj.getString("id");
                    String typeStr = elementObj.getString("type");
                    
                    // 映射新类型到RegionType
                    Region.RegionType type = mapElementTypeToRegionType(typeStr);
                    if (type == null) {
                        Log.w(TAG, "Unknown element type: " + typeStr);
                        continue;
                    }
                    
                    // 解析位置和大小
                    JSONObject positionObj = elementObj.getJSONObject("position");
                    JSONObject sizeObj = elementObj.getJSONObject("size");
                    
                    float x = (float) positionObj.getDouble("x");
                    float y = (float) positionObj.getDouble("y");
                    float width = (float) sizeObj.getDouble("width");
                    float height = (float) sizeObj.getDouble("height");
                    
                    // 计算left, top, right, bottom（归一化坐标，0.0-1.0）
                    float left = x - width / 2;
                    float top = y - height / 2;
                    float right = x + width / 2;
                    float bottom = y + height / 2;
                    
                    // 解析优先级（默认0）
                    int priority = 0;
                    
                    // 解析自定义数据
                    JSONObject mappingObj = elementObj.optJSONObject("mapping");
                    Object customData = mappingObj != null ? mappingObj : null;
                    
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
                            mappingObj != null ? mappingObj.optString("axis", null) : null, // mappingAxis
                            mappingObj != null ? mappingObj.optString("button", null) : null, // mappingButton
                            null, // customMappingTarget
                            customData // customData
                    );
                    parsedRegions.add(region);
                }
            } else {
                // 原格式（regions数组）
                JSONArray regionsArray = jsonObj.optJSONArray("regions");
                if (regionsArray == null) {
                    Log.e(TAG, "No regions or elements array found in JSON");
                    return null;
                }
                
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
     * 将新格式的元素类型映射到RegionType
     * @param elementType 元素类型字符串
     * @return 对应的RegionType，未知类型返回null
     */
    private Region.RegionType mapElementTypeToRegionType(String elementType) {
        switch (elementType.toLowerCase()) {
            case "gyro":
                return Region.RegionType.GYROSCOPE;
            case "button":
                return Region.RegionType.BUTTON;
            case "analog":
                return Region.RegionType.AXIS;
            default:
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