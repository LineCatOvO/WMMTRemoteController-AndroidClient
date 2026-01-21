package com.linecat.wmmtcontroller.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.input.InputScriptEngine;
import com.linecat.wmmtcontroller.input.JsInputScriptEngine;
import com.linecat.wmmtcontroller.input.ProfileManager;
import com.linecat.wmmtcontroller.input.ScriptProfile;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 输入运行时服务
 * 负责采集RawInput、驱动frame tick、调用ScriptEngine、发送WebSocket
 */
public class InputRuntimeService extends Service {
    private static final String TAG = "InputRuntimeService";
    private static final String CHANNEL_ID = "InputRuntimeService";
    private static final int NOTIFICATION_ID = 1;
    
    // 运行时组件
    private ProfileManager profileManager;
    private InputScriptEngine scriptEngine;
    private InputCollector inputCollector;
    private WebSocketClient webSocketClient;
    
    // 运行状态
    private boolean isRunning = false;
    private Thread mainLoopThread;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "InputRuntimeService onCreate");
        
        // 创建通知渠道
        createNotificationChannel();
        
        // 初始化组件
        initializeComponents();
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "InputRuntimeService onStartCommand");
        
        // 启动主循环
        if (!isRunning) {
            startMainLoop();
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // 不支持绑定
        return null;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "InputRuntimeService onDestroy");
        
        // 停止主循环
        stopMainLoop();
        
        // 清理资源
        cleanupComponents();
        
        super.onDestroy();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "输入运行时服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("负责处理输入运行时的核心服务");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("输入运行时服务")
                .setContentText("正在运行...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .build();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 创建脚本引擎
        scriptEngine = new JsInputScriptEngine(this);
        
        // 创建配置文件管理器
        profileManager = new ProfileManager(this, scriptEngine);
        
        // 创建输入采集器
        inputCollector = new InputCollector();
        
        // 创建WebSocket客户端
        webSocketClient = new WebSocketClient();
        
        // 初始化各组件
        scriptEngine.init();
        profileManager.loadAvailableProfiles();
        inputCollector.init(this);
        webSocketClient.init();
    }
    
    /**
     * 清理组件
     */
    private void cleanupComponents() {
        // 清理各组件
        if (webSocketClient != null) {
            webSocketClient.shutdown();
        }
        
        if (inputCollector != null) {
            inputCollector.shutdown();
        }
        
        if (profileManager != null) {
            profileManager.unloadCurrentProfile();
        }
        
        if (scriptEngine != null) {
            scriptEngine.shutdown();
        }
    }
    
    /**
     * 启动主循环
     */
    private void startMainLoop() {
        isRunning = true;
        mainLoopThread = new Thread(() -> {
            Log.d(TAG, "Main loop started");
            
            while (isRunning) {
                try {
                    // 1. 采集原始输入
                    RawInput rawInput = inputCollector.collect();
                    
                    // 2. 更新脚本引擎
                    InputState inputState = new InputState();
                    boolean updateSuccess = scriptEngine.update(rawInput, inputState);
                    
                    // 3. 处理自动回滚
                    if (!updateSuccess) {
                        profileManager.autoRollback();
                    }
                    
                    // 4. 发送输入状态到WebSocket
                    webSocketClient.sendInputState(inputState);
                    
                    // 5. 等待下一帧
                    Thread.sleep(16); // 约60 FPS
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in main loop", e);
                    // 发生错误时尝试回滚
                    profileManager.autoRollback();
                }
            }
            
            Log.d(TAG, "Main loop stopped");
        });
        
        mainLoopThread.start();
    }
    
    /**
     * 停止主循环
     */
    private void stopMainLoop() {
        isRunning = false;
        if (mainLoopThread != null) {
            try {
                mainLoopThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping main loop", e);
            }
        }
    }
    
    /**
     * 获取配置文件管理器
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }
    
    /**
     * 获取脚本引擎
     */
    public InputScriptEngine getScriptEngine() {
        return scriptEngine;
    }
    
    /**
     * 切换配置文件
     */
    public boolean switchProfile(ScriptProfile profile) {
        return profileManager.switchProfile(profile);
    }
    
    /**
     * 回滚配置文件
     */
    public boolean rollbackProfile() {
        return profileManager.rollbackProfile();
    }
    
    /**
     * 卸载当前配置文件
     */
    public void unloadCurrentProfile() {
        profileManager.unloadCurrentProfile();
    }
}