/**
 * 极端情况测试脚本
 * 用于测试JsInputScriptEngine在各种极端情况下的行为
 */

// 测试模式：normal, error, timeout
let testMode = 'normal';

/**
 * 初始化函数
 */
function init() {
    console.log('Test script initialized');
}

/**
 * 更新函数 - 每帧调用
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 * @returns {Object} 脚本输出结果
 */
function update(rawAccess, stateMutator) {
    // 根据测试模式执行不同的逻辑
    switch (testMode) {
        case 'normal':
            return handleNormalCase(rawAccess, stateMutator);
        case 'error':
            return handleErrorCase(rawAccess, stateMutator);
        case 'timeout':
            return handleTimeoutCase(rawAccess, stateMutator);
        default:
            return handleNormalCase(rawAccess, stateMutator);
    }
}

/**
 * 处理正常情况
 */
function handleNormalCase(rawAccess, stateMutator) {
    // 获取陀螺仪数据
    const gyroRoll = rawAccess.getRawInput().getGyroRoll();
    const isButtonA = rawAccess.getRawInput().isButtonA();
    
    // 基于陀螺仪数据控制按键
    if (gyroRoll > 0.1) {
        stateMutator.holdKey('A');
    } else {
        stateMutator.releaseKey('A');
    }
    
    // 基于按钮A控制按键
    if (isButtonA) {
        stateMutator.holdKey('W');
    } else {
        stateMutator.releaseKey('W');
    }
    
    return { heldKeys: [...new Set(Array.from(stateMutator._heldKeys || []))] };
}

/**
 * 处理错误情况
 */
function handleErrorCase(rawAccess, stateMutator) {
    // 抛出异常，测试异常处理机制
    throw new Error('Test exception from script');
}

/**
 * 处理超时情况
 */
function handleTimeoutCase(rawAccess, stateMutator) {
    // 模拟超时，通过无限循环
    while (true) {
        // 无限循环，应该触发超时
    }
    return { heldKeys: [] };
}

/**
 * 重置函数
 */
function reset() {
    console.log('Test script reset');
}
