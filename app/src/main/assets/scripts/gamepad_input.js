/**
 * 游戏手柄输入示例脚本
 * 使用游戏手柄的摇杆控制转向和油门，按钮控制刹车和辅助功能
 */

// 配置参数
const CONFIG = {
    // 转向设置
    steeringAxis: "LX",          // 转向摇杆轴
    steeringSensitivity: 1.0,     // 转向灵敏度
    steeringDeadzone: 0.15,       // 转向死区
    
    // 油门刹车设置
    throttleAxis: "LY",          // 油门摇杆轴
    throttleSensitivity: 1.0,     // 油门灵敏度
    throttleDeadzone: 0.15,       // 油门死区
    brakeButton: "A",             // 刹车按钮
    
    // 辅助功能
    boostButton: "B",             // 氮气加速按钮
    driftButton: "X"              // 漂移按钮
};

/**
 * 更新函数 - 每帧调用
 * @param {Object} context - 脚本执行上下文
 */
function update(context) {
    // 获取摇杆轴值
    const steering = context.getAxis(CONFIG.steeringAxis) * CONFIG.steeringSensitivity;
    const throttle = context.getAxis(CONFIG.throttleAxis) * CONFIG.throttleSensitivity;
    
    // 控制转向
    if (Math.abs(steering) > CONFIG.steeringDeadzone) {
        if (steering > 0) {
            context.holdKey("d");  // 右转
            context.releaseKey("a");
        } else {
            context.holdKey("a");  // 左转
            context.releaseKey("d");
        }
    } else {
        context.releaseKey("a");
        context.releaseKey("d");
    }
    
    // 控制油门（注意：通常游戏手柄的Y轴是反转的）
    if (Math.abs(throttle) > CONFIG.throttleDeadzone) {
        if (throttle < 0) {
            // 向前推摇杆 = 加速
            context.holdKey("w");  // 加速
            context.releaseKey("s");
        } else {
            // 向后拉摇杆 = 倒车
            context.holdKey("s");  // 刹车/倒车
            context.releaseKey("w");
        }
    } else {
        context.releaseKey("w");
    }
    
    // 刹车按钮
    if (context.isGamepadButtonPressed(CONFIG.brakeButton)) {
        context.holdKey("s");  // 刹车
    }
    
    // 氮气加速
    if (context.isGamepadButtonPressed(CONFIG.boostButton)) {
        context.holdKey("space");  // 氮气加速
    } else {
        context.releaseKey("space");
    }
    
    // 漂移
    if (context.isGamepadButtonPressed(CONFIG.driftButton)) {
        context.holdKey("shift");  // 漂移
    } else {
        context.releaseKey("shift");
    }
    
    // 应用按键状态到输入状态
    context.applyKeyStates();
}

/**
 * 初始化函数
 */
function init() {
    console.log("Gamepad Input Script Initialized");
}

/**
 * 重置函数
 */
function reset() {
    console.log("Gamepad Input Script Reset");
}