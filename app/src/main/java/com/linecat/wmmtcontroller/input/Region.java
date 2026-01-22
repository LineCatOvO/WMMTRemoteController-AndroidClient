package com.linecat.wmmtcontroller.input;

/**
 * 输入区域定义
 * 表示屏幕上的一个可交互区域，用于输入事件的解释
 */
public class Region {
    
    /**
     * 区域类型枚举
     */
    public enum RegionType {
        BUTTON,  // 按钮区域
        AXIS,    // 轴区域（如摇杆）
        GESTURE  // 手势区域
    }
    
    private final String id;              // 区域唯一ID
    private final RegionType type;         // 区域类型
    private final float left;              // 左边界（0.0-1.0，相对于屏幕）
    private final float top;               // 上边界（0.0-1.0，相对于屏幕）
    private final float right;             // 右边界（0.0-1.0，相对于屏幕）
    private final float bottom;            // 下边界（0.0-1.0，相对于屏幕）
    private final int priority;            // 优先级（数值越大，优先级越高）
    private final Object customData;       // 自定义数据（根据区域类型不同而不同）
    
    /**
     * 构造函数
     */
    public Region(String id, RegionType type, float left, float top, float right, float bottom, int priority, Object customData) {
        this.id = id;
        this.type = type;
        this.left = Math.max(0f, Math.min(1f, left));
        this.top = Math.max(0f, Math.min(1f, top));
        this.right = Math.max(0f, Math.min(1f, right));
        this.bottom = Math.max(0f, Math.min(1f, bottom));
        this.priority = priority;
        this.customData = customData;
    }
    
    /**
     * 检查点是否在区域内
     * @param x 归一化X坐标（0.0-1.0）
     * @param y 归一化Y坐标（0.0-1.0）
     * @return 是否在区域内
     */
    public boolean contains(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }
    
    /**
     * 获取区域中心坐标
     * @return 中心坐标数组 [x, y]
     */
    public float[] getCenter() {
        return new float[] {
            (left + right) / 2f,
            (top + bottom) / 2f
        };
    }
    
    // Getters
    public String id() {
        return id;
    }
    
    public RegionType type() {
        return type;
    }
    
    public float left() {
        return left;
    }
    
    public float top() {
        return top;
    }
    
    public float right() {
        return right;
    }
    
    public float bottom() {
        return bottom;
    }
    
    public int priority() {
        return priority;
    }
    
    public Object customData() {
        return customData;
    }
}
