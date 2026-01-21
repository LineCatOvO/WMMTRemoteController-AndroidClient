/**
 * WMMT 标准游戏手柄输入脚本
 * 使用Stable API实现游戏手柄输入处理
 */

// 配置参数
const CONFIG = {
    // 转向设置
    steering: {
        axis: "LX",
        sensitivity: 1.0,
        deadzone: 0.15
    },
    // 油门设置
    throttle: {
        axis: "LY",
        sensitivity: 1.0,
        deadzone: 0.15
    },
    // 按钮映射
    buttons: {
        brake: "A",
        boost: "B",
        drift: "X"
    }
};

/**
 * 更新函数 - 每帧调用
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function update(rawAccess, stateMutator) {
    // 处理转向
    handleSteering(rawAccess, stateMutator);
    
    // 处理油门和刹车
    handleThrottle(rawAccess, stateMutator);
    
    // 处理辅助功能
    handleAuxiliary(rawAccess, stateMutator);
}

/**
 * 处理转向输入
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function handleSteering(rawAccess, stateMutator) {
    // 获取转向轴值
    let steeringValue = rawAccess.getAxis(CONFIG.steering.axis);
    
    // 应用灵敏度
    steeringValue *= CONFIG.steering.sensitivity;
    
    // 应用死区
    if (Math.abs(steeringValue) < CONFIG.steering.deadzone) {
        steeringValue = 0;
    }
    
    // 控制转向按键
    if (steeringValue > 0.2) {
        // 右转
        stateMutator.holdKey('d');
        stateMutator.releaseKey('a');
    } else if (steeringValue < -0.2) {
        // 左转
        stateMutator.holdKey('a');
        stateMutator.releaseKey('d');
    } else {
        // 回中
        stateMutator.releaseKey('a');
        stateMutator.releaseKey('d');
    }
}

/**
 * 处理油门和刹车输入
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function handleThrottle(rawAccess, stateMutator) {
    // 获取油门轴值（注意：通常游戏手柄的Y轴是反转的）
    let throttleValue = rawAccess.getAxis(CONFIG.throttle.axis);
    
    // 反转Y轴值，使得向前推摇杆是加速
    throttleValue = -throttleValue;
    
    // 应用灵敏度
    throttleValue *= CONFIG.throttle.sensitivity;
    
    // 应用死区
    if (Math.abs(throttleValue) < CONFIG.throttle.deadzone) {
        throttleValue = 0;
    }
    
    // 处理刹车按钮
    const isBraking = rawAccess.isGamepadButtonPressed(CONFIG.buttons.brake);
    
    if (isBraking) {
        // 刹车优先
        stateMutator.holdKey('s');
        stateMutator.releaseKey('w');
    } else if (throttleValue > 0.2) {
        // 加速
        stateMutator.holdKey('w');
        stateMutator.releaseKey('s');
    } else if (throttleValue < -0.2) {
        // 倒车
        stateMutator.holdKey('s');
        stateMutator.releaseKey('w');
    } else {
        // 松开所有油门刹车键
        stateMutator.releaseKey('w');
    }
}

/**
 * 处理辅助功能输入
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function handleAuxiliary(rawAccess, stateMutator) {
    // 处理氮气加速
    if (rawAccess.isGamepadButtonPressed(CONFIG.buttons.boost)) {
        stateMutator.holdKey('space');
    } else {
        stateMutator.releaseKey('space');
    }
    
    // 处理漂移
    if (rawAccess.isGamepadButtonPressed(CONFIG.buttons.drift)) {
        stateMutator.holdKey('shift');
    } else {
        stateMutator.releaseKey('shift');
    }
}

/**
 * 初始化函数 - 脚本加载时调用
 */
function init() {
    // 初始化操作，例如设置初始状态
    // 注意：不要在这里执行耗时操作
}

/**
 * 重置函数 - 脚本重置时调用
 */
function reset() {
    // 重置操作，例如释放所有按键
    // 注意：这个函数会在脚本切换或错误时被调用
}
