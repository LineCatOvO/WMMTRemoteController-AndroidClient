package com.linecat.wmmtcontroller.service;

import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * 原始输入采集器接口
 * 负责采集系统输入事件，包括触摸、按键、游戏手柄等
 * 是Java输入层的最底层组件，直接与Android系统交互
 */
public interface RawInputCollector {
    
    /**
     * 处理触摸事件
     * @param event 触摸事件
     */
    void onTouch(MotionEvent event);
    
    /**
     * 处理按键事件
     * @param event 按键事件
     */
    void onKey(KeyEvent event);
    
    /**
     * 处理游戏手柄事件
     * @param event 游戏手柄事件
     */
    void onGamepad(InputEvent event);
    
    /**
     * 初始化输入采集器
     * @param context 上下文
     */
    void init(android.content.Context context);
    
    /**
     * 关闭输入采集器
     */
    void shutdown();
}