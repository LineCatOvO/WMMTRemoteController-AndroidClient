package com.linecat.wmmtcontroller.input;

import com.linecat.wmmtcontroller.annotation.Stable;

/**
 * 状态修改接口
 * 只允许修改输入状态，如holdKey/releaseKey等
 */
@Stable(version = "1.0.0")
public interface StateMutator {
    /**
     * 按下并保持键盘按键
     * @param key 按键名称
     */
    @Stable
    void holdKey(String key);
    
    /**
     * 释放键盘按键
     * @param key 按键名称
     */
    @Stable
    void releaseKey(String key);
    
    /**
     * 释放所有键盘按键
     */
    @Stable
    void releaseAllKeys();
    
    /**
     * 检查按键是否被按下
     * @param key 按键名称
     * @return 是否被按下
     */
    @Stable
    boolean isKeyHeld(String key);
    
    /**
     * 推送事件
     * @param eventType 事件类型
     * @param eventData 事件数据
     */
    @Stable
    void pushEvent(String eventType, Object eventData);
}