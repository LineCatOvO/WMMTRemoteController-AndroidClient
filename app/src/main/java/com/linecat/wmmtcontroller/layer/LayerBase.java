package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.util.Log;

/**
 * 层基类
 * 定义层的基本生命周期方法
 */
public abstract class LayerBase {
    protected static final String TAG = "LayerBase";
    protected Context context;
    protected boolean isInitialized = false;
    protected boolean isRunning = false;

    public LayerBase(Context context) {
        this.context = context;
    }

    /**
     * 初始化层
     */
    public abstract void init();

    /**
     * 启动层
     */
    public abstract void start();

    /**
     * 停止层
     */
    public abstract void stop();

    /**
     * 销毁层
     */
    public abstract void destroy();

    /**
     * 获取层是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 获取层是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
}