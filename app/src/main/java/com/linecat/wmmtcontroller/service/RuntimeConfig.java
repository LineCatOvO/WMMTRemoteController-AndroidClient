package com.linecat.wmmtcontroller.service;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 运行时配置管理类
 * 用于管理WebSocket URL、Profile选择等配置
 * 支持测试注入，便于E2E测试
 */
public class RuntimeConfig {

    private static final String TAG = "RuntimeConfig";
    private static final String PREFS_NAME = "runtime_config";
    private static final String KEY_WEBSOCKET_URL = "websocket_url";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_USE_SCRIPT_RUNTIME = "use_script_runtime";
    
    // 默认配置
    private static final String DEFAULT_WEBSOCKET_URL = "ws://localhost:8080/ws/input";
    private static final String DEFAULT_PROFILE_ID = "official-profiles/wmmt_keyboard_basic";
    private static final boolean DEFAULT_USE_SCRIPT_RUNTIME = true;
    
    private final SharedPreferences sharedPreferences;
    
    public RuntimeConfig(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 获取WebSocket URL
     * @return WebSocket URL
     */
    public String getWebSocketUrl() {
        return sharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL);
    }
    
    /**
     * 设置WebSocket URL
     * @param url WebSocket URL
     */
    public void setWebSocketUrl(String url) {
        sharedPreferences.edit().putString(KEY_WEBSOCKET_URL, url).apply();
    }
    
    /**
     * 获取当前Profile ID
     * @return Profile ID
     */
    public String getProfileId() {
        return sharedPreferences.getString(KEY_PROFILE_ID, DEFAULT_PROFILE_ID);
    }
    
    /**
     * 设置当前Profile ID
     * @param profileId Profile ID
     */
    public void setProfileId(String profileId) {
        sharedPreferences.edit().putString(KEY_PROFILE_ID, profileId).apply();
    }
    
    /**
     * 是否使用脚本运行时
     * @return 是否使用脚本运行时
     */
    public boolean useScriptRuntime() {
        return sharedPreferences.getBoolean(KEY_USE_SCRIPT_RUNTIME, DEFAULT_USE_SCRIPT_RUNTIME);
    }
    
    /**
     * 设置是否使用脚本运行时
     * @param useScriptRuntime 是否使用脚本运行时
     */
    public void setUseScriptRuntime(boolean useScriptRuntime) {
        sharedPreferences.edit().putBoolean(KEY_USE_SCRIPT_RUNTIME, useScriptRuntime).apply();
    }
    
    /**
     * 清除所有配置，恢复默认值
     */
    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
