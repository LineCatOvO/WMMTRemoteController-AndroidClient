package com.linecat.wmmtcontroller.input;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 输入脚本引擎接口
 * 定义脚本执行的核心方法
 * 遵循Input Script Runtime Specification
 */
public interface InputScriptEngine {
    
    /**
     * 初始化脚本引擎
     * 启动脚本执行环境
     */
    void init();
    
    /**
     * 加载并执行脚本
     * @param scriptCode 脚本代码
     * @return 加载是否成功
     */
    boolean loadScript(String scriptCode);
    
    /**
     * 更新脚本状态 - 脚本执行的唯一入口点
     * @param rawInput 原始输入数据（只读）
     * @param inputState 输入状态（用于输出）
     * @return 执行是否成功
     */
    boolean update(RawInput rawInput, InputState inputState);
    
    /**
     * 处理输入事件
     * @param event 游戏输入事件
     */
    void onEvent(GameInputEvent event);
    
    /**
     * 重置脚本引擎
     * 清理脚本状态，释放资源
     */
    void reset();
    
    /**
     * 关闭脚本引擎
     * 终止脚本执行环境
     */
    void shutdown();
    
    /**
     * 获取脚本引擎状态
     * @return 脚本引擎状态
     */
    EngineState getState();
    
    /**
     * 获取最后一次执行的错误信息
     * @return 错误信息，如果没有错误则返回null
     */
    String getLastError();
    
    /**
     * 获取脚本执行时间（毫秒）
     * @return 上次执行耗时
     */
    long getLastExecutionTime();
    
    /**
     * 脚本引擎状态枚举
     */
    enum EngineState {
        UNINITIALIZED,  // 未初始化
        INITIALIZED,    // 已初始化
        LOADING,        // 正在加载脚本
        LOADED,         // 脚本已加载
        EXECUTING,      // 正在执行脚本
        ERROR,          // 错误状态
        SHUTDOWN        // 已关闭
    }
}