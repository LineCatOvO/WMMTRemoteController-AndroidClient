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
        BUTTON,      // 按钮区域
        AXIS,        // 轴区域（如摇杆）
        GESTURE,     // 手势区域
        GYROSCOPE,   // 陀螺仪区域
        OPERATION,   // 操作区域
        MAPPING      // 映射区域
    }
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        STEERING,    // 方向盘
        THROTTLE,    // 油门
        BRAKE,       // 刹车
        BUTTON,      // 按钮
        CUSTOM       // 自定义
    }
    
    /**
     * 映射类型枚举
     */
    public enum MappingType {
        KEYBOARD,    // 键盘映射
        GAMEPAD,     // 游戏手柄映射
        CUSTOM       // 自定义映射
    }
    
    private final String id;              // 区域唯一ID
    private final RegionType type;        // 区域类型
    private final float left;             // 左边界（0.0-1.0，相对于屏幕）
    private final float top;              // 上边界（0.0-1.0，相对于屏幕）
    private final float right;            // 右边界（0.0-1.0，相对于屏幕）
    private final float bottom;           // 下边界（0.0-1.0，相对于屏幕）
    private final int zIndex;             // z-index（数值越大，优先级越高）
    private final float deadzone;         // 死区
    private final String curve;           // 曲线类型
    private final float[] range;          // 范围
    private final float[] outputRange;    // 输出范围
    
    // Operation 层属性
    private final OperationType operationType; // 操作类型
    
    // Mapping 层属性
    private final MappingType mappingType;     // 映射类型
    private final String mappingKey;           // 映射键
    private final String mappingAxis;          // 映射轴
    private final String mappingButton;        // 映射按钮
    private final String customMappingTarget;  // 自定义映射目标
    
    private final Object customData;       // 自定义数据（根据区域类型不同而不同）
    
    /**
     * 构造函数
     */
    public Region(String id, RegionType type, float left, float top, float right, float bottom, int zIndex, float deadzone, String curve, float[] range, float[] outputRange, OperationType operationType, MappingType mappingType, String mappingKey, String mappingAxis, String mappingButton, String customMappingTarget, Object customData) {
        this.id = id;
        this.type = type;
        this.left = Math.max(0f, Math.min(1f, left));
        this.top = Math.max(0f, Math.min(1f, top));
        this.right = Math.max(0f, Math.min(1f, right));
        this.bottom = Math.max(0f, Math.min(1f, bottom));
        this.zIndex = zIndex;
        this.deadzone = deadzone;
        this.curve = curve;
        this.range = range;
        this.outputRange = outputRange;
        this.operationType = operationType;
        this.mappingType = mappingType;
        this.mappingKey = mappingKey;
        this.mappingAxis = mappingAxis;
        this.mappingButton = mappingButton;
        this.customMappingTarget = customMappingTarget;
        this.customData = customData;
    }
    
    /**
     * 检查点是否在区域内（命中测试）
     * @param x 归一化X坐标（0.0-1.0）
     * @param y 归一化Y坐标（0.0-1.0）
     * @return 是否在区域内
     */
    public boolean hitTest(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }
    
    /**
     * 检查点是否在区域内（兼容旧方法名）
     * @param x 归一化X坐标（0.0-1.0）
     * @param y 归一化Y坐标（0.0-1.0）
     * @return 是否在区域内
     */
    public boolean contains(float x, float y) {
        return hitTest(x, y);
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
    public String getId() {
        return id;
    }
    
    public RegionType getType() {
        return type;
    }
    
    public float getLeft() {
        return left;
    }
    
    public float getTop() {
        return top;
    }
    
    public float getRight() {
        return right;
    }
    
    public float getBottom() {
        return bottom;
    }
    
    public int getZIndex() {
        return zIndex;
    }
    
    public float getDeadzone() {
        return deadzone;
    }
    
    public String getCurve() {
        return curve;
    }
    
    public float[] getRange() {
        return range;
    }
    
    public float[] getOutputRange() {
        return outputRange;
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public MappingType getMappingType() {
        return mappingType;
    }
    
    public String getMappingKey() {
        return mappingKey;
    }
    
    public String getMappingAxis() {
        return mappingAxis;
    }
    
    public String getMappingButton() {
        return mappingButton;
    }
    
    public String getCustomMappingTarget() {
        return customMappingTarget;
    }
    
    public Object getCustomData() {
        return customData;
    }
    
    // 兼容旧方法名
    public String id() {
        return getId();
    }
    
    public RegionType type() {
        return getType();
    }
    
    public float left() {
        return getLeft();
    }
    
    public float top() {
        return getTop();
    }
    
    public float right() {
        return getRight();
    }
    
    public float bottom() {
        return getBottom();
    }
    
    @Override
    public String toString() {
        return "Region{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", zIndex=" + zIndex +
                ", deadzone=" + deadzone +
                ", curve='" + curve + '\'' +
                '}';
    }
}
