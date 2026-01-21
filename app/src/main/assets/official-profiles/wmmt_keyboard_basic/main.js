/**
 * WMMT 基础键盘输入脚本
 * 使用Stable API实现按键映射和输入处理
 */

/**
 * 更新函数 - 每帧调用
 * @param {Object} rawAccess - 只读访问接口
 * @param {Object} stateMutator - 状态修改接口
 */
function update(rawAccess, stateMutator) {
    // 获取原始输入数据
    const rawInput = rawAccess.getRawInput();
    
    // 控制转向
    if (rawInput.isButtonA()) {
        // 按下A键，左转
        stateMutator.holdKey('a');
        stateMutator.releaseKey('d');
    } else if (rawInput.isButtonB()) {
        // 按下B键，右转
        stateMutator.holdKey('d');
        stateMutator.releaseKey('a');
    } else {
        // 松开转向键
        stateMutator.releaseKey('a');
        stateMutator.releaseKey('d');
    }
    
    // 控制油门和刹车
    if (rawInput.isButtonC()) {
        // 按下C键，加速
        stateMutator.holdKey('w');
        stateMutator.releaseKey('s');
    } else if (rawInput.isButtonD()) {
        // 按下D键，刹车
        stateMutator.holdKey('s');
        stateMutator.releaseKey('w');
    } else {
        // 松开油门键，但保持刹车松开
        stateMutator.releaseKey('w');
    }
    
    // 控制辅助功能
    if (rawInput.isButtonDpadUp()) {
        // 按下DpadUp，氮气加速
        stateMutator.holdKey('space');
    } else {
        stateMutator.releaseKey('space');
    }
    
    if (rawInput.isButtonDpadDown()) {
        // 按下DpadDown，漂移
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
