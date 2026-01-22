package com.linecat.wmmtcontroller.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 布局快照
 * 表示当前屏幕上的所有可交互区域的快照
 * 用于输入事件的区域命中检测
 */
public class LayoutSnapshot {
    
    private final List<Region> regions;
    private final long timestamp;
    
    /**
     * 构造函数
     * @param regions 区域列表
     */
    public LayoutSnapshot(List<Region> regions) {
        // 创建区域列表的不可变副本，并按优先级排序（优先级高的在前）
        this.regions = new ArrayList<>(regions);
        Collections.sort(this.regions, Comparator.comparingInt(Region::priority).reversed());
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 区域命中检测
     * @param normalizedX 归一化X坐标（0.0-1.0）
     * @param normalizedY 归一化Y坐标（0.0-1.0）
     * @return 命中的区域，如果没有命中则返回null
     */
    public Region hitTest(float normalizedX, float normalizedY) {
        // 遍历所有区域，返回第一个命中的区域（按优先级排序，优先级高的先检查）
        for (Region region : regions) {
            if (region.contains(normalizedX, normalizedY)) {
                return region;
            }
        }
        return null;
    }
    
    /**
     * 获取所有区域
     * @return 区域列表（不可变）
     */
    public List<Region> getRegions() {
        return Collections.unmodifiableList(regions);
    }
    
    /**
     * 根据ID获取区域
     * @param regionId 区域ID
     * @return 区域，如果没有找到则返回null
     */
    public Region getRegionById(String regionId) {
        for (Region region : regions) {
            if (region.id().equals(regionId)) {
                return region;
            }
        }
        return null;
    }
    
    /**
     * 获取布局快照的时间戳
     * @return 时间戳（毫秒）
     */
    public long timestamp() {
        return timestamp;
    }
    
    /**
     * 获取区域数量
     * @return 区域数量
     */
    public int getRegionCount() {
        return regions.size();
    }
    
    /**
     * 检查是否包含指定区域
     * @param regionId 区域ID
     * @return 是否包含
     */
    public boolean containsRegion(String regionId) {
        return getRegionById(regionId) != null;
    }
}
