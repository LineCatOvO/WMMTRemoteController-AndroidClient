/**
 * Faulty script for testing auto-rollback functionality
 * This script always throws an exception to trigger rollback
 */

/**
 * 初始化函数
 */
function init() {
    console.log('Faulty script initialized');
}

/**
 * 更新函数 - 每帧调用，总是抛出异常
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 * @returns {Object} 脚本输出结果
 */
function update(rawAccess, stateMutator) {
    // Always throw an exception to trigger auto-rollback
    throw new Error('Test exception from faulty script');
    
    // This code will never be reached
    return {
        heldKeys: []
    };
}

/**
 * 重置函数
 */
function reset() {
    console.log('Faulty script reset');
}
