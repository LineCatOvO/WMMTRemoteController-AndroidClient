package com.linecat.wmmtcontroller.input;

/**
 * 输入原语基类
 * 与Android无关的输入模型
 * 所有输入事件的基类
 */
public abstract class InputPrimitives {
    protected long timestamp;  // 时间戳
    
    public InputPrimitives(long timestamp) {
        this.timestamp = timestamp;
    }
    
    // Getter
    public long getTimestamp() { return timestamp; }
    
    /**
     * 指针事件类型
     */
    public enum PointerEventType {
        DOWN,   // 按下
        MOVE,   // 移动
        UP      // 抬起
    }
    
    /**
     * 按钮事件类型
     */
    public enum ButtonEventType {
        PRESS,  // 按下
        RELEASE // 释放
    }
    
    /**
     * 传感器类型
     */
    public enum SensorType {
        GYROSCOPE,      // 陀螺仪
        ACCELEROMETER,  // 加速度计
        MAGNETOMETER,   // 磁力计
        GRAVITY         // 重力传感器
    }
    
    /**
     * 指针事件
     */
    public static class PointerEvent extends InputPrimitives {
        private PointerEventType type;   // 事件类型
        private float x;                 // X坐标
        private float y;                 // Y坐标
        private int pointerId;           // 指针ID
        
        public PointerEvent(long timestamp, PointerEventType type, float x, float y, int pointerId) {
            super(timestamp);
            this.type = type;
            this.x = x;
            this.y = y;
            this.pointerId = pointerId;
        }
        
        // Getters
        public PointerEventType getType() { return type; }
        public float getX() { return x; }
        public float getY() { return y; }
        public int getPointerId() { return pointerId; }
        
        @Override
        public String toString() {
            return "PointerEvent{" +
                    "timestamp=" + timestamp +
                    ", type=" + type +
                    ", x=" + x +
                    ", y=" + y +
                    ", pointerId=" + pointerId +
                    '}';
        }
    }
    
    /**
     * 轴事件
     */
    public static class AxisEvent extends InputPrimitives {
        private SensorType sensorType; // 传感器类型
        private float x;             // X轴值
        private float y;             // Y轴值
        private float z;             // Z轴值
        
        public AxisEvent(long timestamp, SensorType sensorType, float x, float y, float z) {
            super(timestamp);
            this.sensorType = sensorType;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        // Getters
        public SensorType getSensorType() { return sensorType; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
        
        @Override
        public String toString() {
            return "AxisEvent{" +
                    "timestamp=" + timestamp +
                    ", sensorType=" + sensorType +
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
    
    /**
     * 按钮事件
     */
    public static class ButtonEvent extends InputPrimitives {
        private ButtonEventType type;    // 事件类型
        private String buttonId;         // 按钮ID
        
        public ButtonEvent(long timestamp, ButtonEventType type, String buttonId) {
            super(timestamp);
            this.type = type;
            this.buttonId = buttonId;
        }
        
        // Getters
        public ButtonEventType getType() { return type; }
        public String getButtonId() { return buttonId; }
        
        @Override
        public String toString() {
            return "ButtonEvent{" +
                    "timestamp=" + timestamp +
                    ", type=" + type +
                    ", buttonId='" + buttonId + "'" +
                    '}';
        }
    }
    
    /**
     * 逻辑轴事件
     */
    public static class LogicalAxisEvent extends InputPrimitives {
        private String axisId;       // 轴ID
        private float value;         // 轴值
        
        public LogicalAxisEvent(long timestamp, String axisId, float value) {
            super(timestamp);
            this.axisId = axisId;
            this.value = value;
        }
        
        // Getters
        public String getAxisId() { return axisId; }
        public float getValue() { return value; }
        
        @Override
        public String toString() {
            return "LogicalAxisEvent{" +
                    "timestamp=" + timestamp +
                    ", axisId='" + axisId + "'" +
                    ", value=" + value +
                    '}';
        }
    }
}