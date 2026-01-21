/**
 * 陀螺仪转向示例脚本
 * 使用陀螺仪的roll值控制转向，按钮控制油门和刹车
 */

// 配置参数
const CONFIG = {
    steeringSensitivity: 0.5,  // 转向灵敏度
    steeringDeadzone: 0.2,      // 转向死区
    throttleButton: "A",        // 油门按钮
    brakeButton: "B"            // 刹车按钮
};

/**
 * 更新函数 - 每帧调用
 * @param {Object} context - 脚本执行上下文
 */
function update(context) {
    // 获取陀螺仪数据
    const gyro = context.getGyro();
    
    // 获取原始输入
    const raw = context.getRawInput();
    
    // 计算转向值
    const roll = gyro.getRoll() * CONFIG.steeringSensitivity;
    
    // 应用死区
    let steering = 0;
    if (Math.abs(roll) > CONFIG.steeringDeadzone) {
        steering = roll;
    }
    
    // 控制转向
    if (steering > 0.2) {
        context.holdKey("d");  // 右转
        context.releaseKey("a");
    } else if (steering < -0.2) {
        context.holdKey("a");  // 左转
        context.releaseKey("d");
    } else {
        context.releaseKey("a");
        context.releaseKey("d");
    }
    
    // 控制油门
    if (raw.isButtonA()) {
        context.holdKey("w");  // 加速
    } else {
        context.releaseKey("w");
    }
    
    // 控制刹车
    if (raw.isButtonB()) {
        context.holdKey("s");  // 刹车
    } else {
        context.releaseKey("s");
    }
    
    // 应用按键状态到输入状态
    context.applyKeyStates();
}

/**
 * 初始化函数
 */
function init() {
    console.log("Gyro Steering Script Initialized");
}

/**
 * 重置函数
 */
function reset() {
    console.log("Gyro Steering Script Reset");
}