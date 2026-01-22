package com.linecat.wmmtcontroller.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.linecat.wmmtcontroller.database.DatabaseHelper;
import com.linecat.wmmtcontroller.model.ConnectionInfo;

/**
 * 运行时配置管理类
 * 用于管理WebSocket URL、Profile选择等配置
 * 支持测试注入，便于E2E测试
 */
public class RuntimeConfig {

    private static final String TAG = "RuntimeConfig";
    private static final String PREFS_NAME = "runtime_config";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_USE_SCRIPT_RUNTIME = "use_script_runtime";
    
    // 默认配置
    private static final String DEFAULT_PROFILE_ID = "official-profiles/wmmt_keyboard_basic";
    private static final boolean DEFAULT_USE_SCRIPT_RUNTIME = true;
    
    private final SharedPreferences sharedPreferences;
    private final DatabaseHelper databaseHelper;
    
    public RuntimeConfig(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.databaseHelper = new DatabaseHelper(context);
    }
    
    /**
     * 获取WebSocket URL
     * @return WebSocket URL
     */
    public String getWebSocketUrl() {
        // 从数据库获取默认连接信息
        ConnectionInfo connectionInfo = databaseHelper.getDefaultConnectionInfo();
        if (connectionInfo != null) {
            // 构建WebSocket URL
            return DatabaseHelper.getWebSocketUrl(connectionInfo);
        } else {
            // 数据库中没有连接信息，返回默认URL
            return "ws://localhost:8080/ws/input";
        }
    }
    
    /**
     * 设置WebSocket URL
     * @param url WebSocket URL
     */
    public void setWebSocketUrl(String url) {
        // 解析URL，获取地址和端口
        String address = "localhost";
        int port = 8080;
        boolean useTls = url.startsWith("wss://");
        
        // 解析地址和端口
        try {
            String urlWithoutProtocol = url.replaceFirst("^wss?:\\/\\/", "");
            String[] parts = urlWithoutProtocol.split(":");
            if (parts.length > 0) {
                address = parts[0];
                if (parts.length > 1) {
                    String portStr = parts[1].split("/")[0];
                    port = Integer.parseInt(portStr);
                }
            }
            
            // 保存到数据库
            ConnectionInfo connectionInfo = new ConnectionInfo(address, port);
            connectionInfo.setUseTls(useTls);
            connectionInfo.setDefault(true);
            databaseHelper.saveConnectionInfo(connectionInfo);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WebSocket URL: " + url, e);
        }
    }
    
    /**
     * 获取默认连接信息
     * @return 默认连接信息
     */
    public ConnectionInfo getDefaultConnectionInfo() {
        return databaseHelper.getDefaultConnectionInfo();
    }
    
    /**
     * 保存连接信息
     * @param connectionInfo 连接信息
     * @return 保存成功的ID
     */
    public long saveConnectionInfo(ConnectionInfo connectionInfo) {
        return databaseHelper.saveConnectionInfo(connectionInfo);
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
