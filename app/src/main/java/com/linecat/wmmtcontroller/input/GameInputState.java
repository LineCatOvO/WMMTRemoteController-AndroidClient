package com.linecat.wmmtcontroller.input;

/**
 * 游戏输入状态类
 * 用于表示持续的游戏输入状态
 * 包含所有连续变化的输入值
 */
public class GameInputState {
    // 转向状态（-1.0 到 1.0，负值为左，正值为右）
    private float steering; 
    
    // 油门状态（0.0 到 1.0，0 为未踩，1 为踩到底）
    private float throttle;
    
    // 刹车状态（0.0 到 1.0，0 为未踩，1 为踩到底）
    private float brake;
    
    /**
     * 构造函数
     */
    public GameInputState() {
        this.steering = 0.0f;
        this.throttle = 0.0f;
        this.brake = 0.0f;
    }
    
    /**
     * 获取转向状态
     * @return 转向值，范围 -1.0 到 1.0
     */
    public float getSteering() {
        return steering;
    }
    
    /**
     * 设置转向状态
     * @param steering 转向值，范围 -1.0 到 1.0
     */
    public void setSteering(float steering) {
        this.steering = Math.max(-1.0f, Math.min(1.0f, steering));
    }
    
    /**
     * 获取油门状态
     * @return 油门值，范围 0.0 到 1.0
     */
    public float getThrottle() {
        return throttle;
    }
    
    /**
     * 设置油门状态
     * @param throttle 油门值，范围 0.0 到 1.0
     */
    public void setThrottle(float throttle) {
        this.throttle = Math.max(0.0f, Math.min(1.0f, throttle));
    }
    
    /**
     * 获取刹车状态
     * @return 刹车值，范围 0.0 到 1.0
     */
    public float getBrake() {
        return brake;
    }
    
    /**
     * 设置刹车状态
     * @param brake 刹车值，范围 0.0 到 1.0
     */
    public void setBrake(float brake) {
        this.brake = Math.max(0.0f, Math.min(1.0f, brake));
    }
    
    @Override
    public String toString() {
        return "GameInputState{" +
                "steering=" + steering +
                ", throttle=" + throttle +
                ", brake=" + brake +
                '}';
    }
}