// 测试极端情况的脚本
// 支持模拟延迟、异常、超时等情况

let testMode = 'normal'; // normal, delay, error, timeout
let delayTime = 100; // 延迟时间（毫秒）

// 初始化函数
function init() {
    console.log('Test script initialized');
}

// 更新函数
function update(rawInput, state, event) {
    // 根据测试模式执行不同逻辑
    switch (testMode) {
        case 'delay':
            // 模拟延迟返回
            return delayUpdate(rawInput);
        case 'error':
            // 模拟抛出异常
            throw new Error('Test error in update function');
        case 'timeout':
            // 模拟超时（不返回）
            while (true) {
                // 无限循环，模拟超时
            }
        case 'normal':
        default:
            // 正常执行
            return normalUpdate(rawInput);
    }
}

// 正常更新逻辑
function normalUpdate(rawInput) {
    const heldKeys = [];
    
    // 根据陀螺仪数据决定按键
    if (rawInput.gyroRoll > 0.1) {
        heldKeys.push('A');
    } else if (rawInput.gyroRoll < -0.1) {
        heldKeys.push('D');
    }
    
    if (rawInput.buttonA) {
        heldKeys.push('W');
    }
    
    if (rawInput.buttonB) {
        heldKeys.push('S');
    }
    
    return {
        frameId: rawInput.frameId,
        heldKeys: heldKeys,
        events: [],
        debug: {
            testMode: testMode,
            gyroRoll: rawInput.gyroRoll
        }
    };
}

// 延迟更新逻辑
function delayUpdate(rawInput) {
    // 使用setTimeout模拟延迟
    setTimeout(() => {
        const result = normalUpdate(rawInput);
        // 注意：这里不能直接返回，需要通过回调
        // 实际测试中，这种情况会被系统的超时机制处理
    }, delayTime);
    
    // 立即返回空结果，模拟延迟情况
    return {
        frameId: rawInput.frameId,
        heldKeys: [],
        events: [],
        debug: {
            testMode: testMode,
            delayTime: delayTime
        }
    };
}

// 事件处理函数
function onEvent(event) {
    console.log('Event received:', event);
}

// 重置函数
function reset() {
    console.log('Test script reset');
    testMode = 'normal';
    delayTime = 100;
}