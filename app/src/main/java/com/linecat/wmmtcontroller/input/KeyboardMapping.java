package com.linecat.wmmtcontroller.input;

/**
 * 键盘映射策略类
 * 封装所有输入到键盘按键的映射关系
 * 负责将游戏语义映射到具体的键盘按键
 */
public class KeyboardMapping {
    // 方向控制映射
    private String leftKey = "a"; // 左转向
    private String rightKey = "d"; // 右转向
    
    // 油门刹车映射
    private String throttleKey = "w"; // 加速
    private String brakeKey = "s"; // 刹车
    
    // 辅助按钮映射
    private String buttonCKey = "ctrl"; // 按钮C
    private String buttonDKey = "alt"; // 按钮D
    
    /**
     * 获取左转向键
     * @return 左转向对应的键盘按键
     */
    public String getLeftKey() {
        return leftKey;
    }
    
    /**
     * 设置左转向键
     * @param leftKey 左转向对应的键盘按键
     */
    public void setLeftKey(String leftKey) {
        this.leftKey = leftKey;
    }
    
    /**
     * 获取右转向键
     * @return 右转向对应的键盘按键
     */
    public String getRightKey() {
        return rightKey;
    }
    
    /**
     * 设置右转向键
     * @param rightKey 右转向对应的键盘按键
     */
    public void setRightKey(String rightKey) {
        this.rightKey = rightKey;
    }
    
    /**
     * 获取加速键
     * @return 加速对应的键盘按键
     */
    public String getThrottleKey() {
        return throttleKey;
    }
    
    /**
     * 设置加速键
     * @param throttleKey 加速对应的键盘按键
     */
    public void setThrottleKey(String throttleKey) {
        this.throttleKey = throttleKey;
    }
    
    /**
     * 获取刹车键
     * @return 刹车对应的键盘按键
     */
    public String getBrakeKey() {
        return brakeKey;
    }
    
    /**
     * 设置刹车键
     * @param brakeKey 刹车对应的键盘按键
     */
    public void setBrakeKey(String brakeKey) {
        this.brakeKey = brakeKey;
    }
    
    /**
     * 获取按钮C对应的键盘按键
     * @return 按钮C对应的键盘按键
     */
    public String getButtonCKey() {
        return buttonCKey;
    }
    
    /**
     * 设置按钮C对应的键盘按键
     * @param buttonCKey 按钮C对应的键盘按键
     */
    public void setButtonCKey(String buttonCKey) {
        this.buttonCKey = buttonCKey;
    }
    
    /**
     * 获取按钮D对应的键盘按键
     * @return 按钮D对应的键盘按键
     */
    public String getButtonDKey() {
        return buttonDKey;
    }
    
    /**
     * 设置按钮D对应的键盘按键
     * @param buttonDKey 按钮D对应的键盘按键
     */
    public void setButtonDKey(String buttonDKey) {
        this.buttonDKey = buttonDKey;
    }
}