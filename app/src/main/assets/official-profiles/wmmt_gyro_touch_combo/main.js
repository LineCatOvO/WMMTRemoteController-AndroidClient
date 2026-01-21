/**
 * WMMT 陀螺仪+触摸组合输入脚本
 * 使用Stable API实现陀螺仪转向和触摸控制油门刹车
 */

// 配置参数
const CONFIG = {
    // 陀螺仪设置
    gyro: {
        sensitivity: 0.5,
        deadzone: 0.2,
        axis: "roll"
    },
    // 触摸区域设置
    touch: {
        accelerateArea: {
            x: 0,
            y: 0,
            width: 0.5,
            height: 1.0
        },
        brakeArea: {
            x: 0.5,
            y: 0,
            width: 0.5,
            height: 1.0
        }
    },
    // 按键映射
    keys: {
        left: "a",
        right: "d",
        accelerate: "w",
        brake: "s",
        boost: "space",
        drift: "shift"
    }
};

/**
 * 更新函数 - 每帧调用
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function update(rawAccess, stateMutator) {
    // 处理陀螺仪转向
    handleGyroSteering(rawAccess, stateMutator);
    
    // 处理触摸油门刹车
    handleTouchThrottle(rawAccess, stateMutator);
    
    // 处理辅助功能
    handleAuxiliary(rawAccess, stateMutator);
}

/**
 * 处理陀螺仪转向
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function handleGyroSteering(rawAccess, stateMutator) {
    // 获取原始输入数据
    const rawInput = rawAccess.getRawInput();
    
    // 获取陀螺仪数据
    const gyroRoll = rawInput.getGyroRoll();
    
    // 应用灵敏度
    let steering = gyroRoll * CONFIG.gyro.sensitivity;
    
    // 应用死区
    if (Math.abs(steering) < CONFIG.gyro.deadzone) {
        steering = 0;
    }
    
    // 控制转向按键
    if (steering > 0.2) {
        // 右转
        stateMutator.holdKey(CONFIG.keys.right);
        stateMutator.releaseKey(CONFIG.keys.left);
    } else if (steering < -0.2) {
        // 左转
        stateMutator.holdKey(CONFIG.keys.left);
        stateMutator.releaseKey(CONFIG.keys.right);
    } else {
        // 回中
        stateMutator.releaseKey(CONFIG.keys.left);
        stateMutator.releaseKey(CONFIG.keys.right);
    }
}

/**
 * 处理触摸油门刹车
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function handleTouchThrottle(rawAccess, stateMutator) {
    // 获取原始输入数据
    const rawInput = rawAccess.getRawInput();
    
    // 检查触摸状态
    if (rawInput.isTouchPressed()) {
        // 获取触摸位置
        const touchX = rawInput.getTouchX();
        const touchY = rawInput.getTouchY();
        
        // 检查是否在加速区域
        if (isInArea(touchX, touchY, CONFIG.touch.accelerateArea)) {
            // 加速
            stateMutator.holdKey(CONFIG.keys.accelerate);
            stateMutator.releaseKey(CONFIG.keys.brake);
        } 
        // 检查是否在刹车区域
        else if (isInArea(touchX, touchY, CONFIG.touch.brakeArea)) {
            // 刹车
            stateMutator.holdKey(CONFIG.keys.brake);
            stateMutator.releaseKey(CONFIG.keys.accelerate);
        }
    } else {
        // 松开油门
        stateMutator.releaseKey(CONFIG.keys.accelerate);
        // 保持刹车松开
        stateMutator.releaseKey(CONFIG.keys.brake);
    }
}

/**
 * 处理辅助功能
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function handleAuxiliary(rawAccess, stateMutator) {
    // 获取原始输入数据
    const rawInput = rawAccess.getRawInput();
    
    // 处理氮气加速
    if (rawInput.isButtonA()) {
        stateMutator.holdKey(CONFIG.keys.boost);
    } else {
        stateMutator.releaseKey(CONFIG.keys.boost);
    }
    
    // 处理漂移
    if (rawInput.isButtonB()) {
        stateMutator.holdKey(CONFIG.keys.drift);
    } else {
        stateMutator.releaseKey(CONFIG.keys.drift);
    }
}

/**
 * 检查点是否在指定区域内
 * @param {number} x - 点的X坐标（0-1）
 * @param {number} y - 点的Y坐标（0-1）
 * @param {Object} area - 区域定义
 * @returns {boolean} 是否在区域内
 */
function isInArea(x, y, area) {
    return x >= area.x && x <= area.x + area.width &&
           y >= area.y && y <= area.y + area.height;
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
