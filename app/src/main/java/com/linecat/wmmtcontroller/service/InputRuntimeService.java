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
import com.linecat.wmmtcontroller.floatwindow.FloatWindowManager;
import com.linecat.wmmtcontroller.input.EventNormalizer;
import com.linecat.wmmtcontroller.input.InputInterpreter;
import com.linecat.wmmtcontroller.input.InputScriptEngine;
import com.linecat.wmmtcontroller.input.JsInputScriptEngine;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.input.NormalizedEvent;
import com.linecat.wmmtcontroller.input.ProfileManager;
import com.linecat.wmmtcontroller.input.RegionResolver;
import com.linecat.wmmtcontroller.input.ScriptProfile;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import java.util.ArrayList;

/**
 * 输入运行时服务
 * 负责采集RawInput、驱动frame tick、调用ScriptEngine、发送WebSocket
 */
public class InputRuntimeService extends Service {
    private static final String TAG = "InputRuntimeService";
    private static final String CHANNEL_ID = "InputRuntimeService";
    private static final int NOTIFICATION_ID = 1;

    // 运行时组件
    private RuntimeConfig runtimeConfig;
    private ProfileManager profileManager;
    private InputScriptEngine scriptEngine;
    private InputCollector inputCollector;
    private WebSocketClient webSocketClient;
    // 输入服务层组件
    private InputInterpreter inputInterpreter;
    private EventNormalizer eventNormalizer;
    private RegionResolver regionResolver;
    // 浮窗管理器
    private FloatWindowManager floatWindowManager;
    // UI更新Handler
    private android.os.Handler uiHandler;

    // 运行状态
    private boolean isRunning = false;
    private Thread mainLoopThread;
    // Frame ID 计数器，用于生成单调递增的帧ID
    private long frameIdCounter = 1;

    // 广播接收器，用于处理连接控制事件
    private android.content.BroadcastReceiver connectControlReceiver;
    // 广播接收器，用于处理WebSocket连接状态事件
    private android.content.BroadcastReceiver wsStatusReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "InputRuntimeService onCreate");

        // 创建通知渠道
        createNotificationChannel();

        // 初始化UI Handler
        uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        // 初始化浮窗管理器
        floatWindowManager = FloatWindowManager.getInstance(this);

        // 初始化广播接收器
        initConnectControlReceiver();
        initWebSocketStatusReceiver();

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

            // 显示浮窗
            floatWindowManager.showFloatWindow();
            floatWindowManager.updateStatusText("服务运行中");

            // 发送运行时启动广播
            Intent broadcastIntent = new Intent(RuntimeEvents.ACTION_RUNTIME_STARTED);
            sendBroadcast(broadcastIntent);
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

        // 隐藏浮窗
        floatWindowManager.hideFloatWindow();

        // 注销广播接收器
        if (connectControlReceiver != null) {
            try {
                unregisterReceiver(connectControlReceiver);
                connectControlReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering connect control receiver: " + e.getMessage());
            }
        }
        
        if (wsStatusReceiver != null) {
            try {
                unregisterReceiver(wsStatusReceiver);
                wsStatusReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering WebSocket status receiver: " + e.getMessage());
            }
        }

        // 清理资源
        cleanupComponents();

        super.onDestroy();
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "输入运行时服务",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("负责处理输入运行时的核心服务");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
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
     * 初始化连接控制广播接收器
     */
    private void initConnectControlReceiver() {
        connectControlReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case "com.linecat.wmmtcontroller.ACTION_START_CONNECT":
                            Log.d(TAG, "Received start connect broadcast");
                            // 开始连接WebSocket
                            webSocketClient.connect();
                            break;
                        case "com.linecat.wmmtcontroller.ACTION_STOP_CONNECT":
                            Log.d(TAG, "Received stop connect broadcast");
                            // 断开WebSocket连接
                            webSocketClient.disconnect();
                            break;
                    }
                }
            }
        };

        // 注册广播接收器
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction("com.linecat.wmmtcontroller.ACTION_START_CONNECT");
        filter.addAction("com.linecat.wmmtcontroller.ACTION_STOP_CONNECT");
        registerReceiver(connectControlReceiver, filter);
    }
    
    /**
     * 初始化WebSocket状态广播接收器
     */
    private void initWebSocketStatusReceiver() {
        wsStatusReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case RuntimeEvents.ACTION_WS_DISCONNECTED:
                            Log.d(TAG, "Received WebSocket disconnected broadcast");
                            // 检查是否是因为连接失败导致的断开
                            // 如果当前尝试连接但未成功，显示连接错误
                            if (webSocketClient != null && !webSocketClient.isConnected()) {
                                showConnectionError();
                            }
                            break;
                    }
                }
            }
        };
        
        // 注册广播接收器
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(RuntimeEvents.ACTION_WS_DISCONNECTED);
        registerReceiver(wsStatusReceiver, filter);
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 创建运行时配置
        runtimeConfig = new RuntimeConfig(this);

        // 创建脚本引擎
        scriptEngine = new JsInputScriptEngine(this);

        // 创建配置文件管理器
        profileManager = new ProfileManager(this, scriptEngine);

        // 创建输入采集器
        inputCollector = new InputCollector();

        // 创建输入服务层组件
        inputInterpreter = new InputInterpreter();
        eventNormalizer = new EventNormalizer();
        regionResolver = new RegionResolver(this);

        // 创建WebSocket客户端，使用配置的URL
        webSocketClient = new WebSocketClient(this, runtimeConfig.getWebSocketUrl());

        // 初始化各组件
        scriptEngine.init();
        profileManager.loadAvailableProfiles();

        // 加载配置的Profile
        String profileId = runtimeConfig.getProfileId();
        if (profileId != null) {
            boolean loadSuccess = profileManager.loadAndSetProfile(profileId);
            if (loadSuccess) {
                // 发送配置文件加载成功广播
                Intent intent = new Intent(RuntimeEvents.ACTION_PROFILE_LOADED);
                intent.putExtra(RuntimeEvents.EXTRA_PROFILE_ID, profileId);
                sendBroadcast(intent);
            }
        }

        // 发送脚本引擎准备就绪广播
        Intent engineReadyIntent = new Intent(RuntimeEvents.ACTION_SCRIPT_ENGINE_READY);
        sendBroadcast(engineReadyIntent);

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
        
        // 清理输入服务层组件
        if (inputInterpreter != null) {
            inputInterpreter.reset();
        }
        
        if (eventNormalizer != null) {
            eventNormalizer.reset();
        }
        
        if (regionResolver != null) {
            regionResolver.reset();
        }
    }

    /**
     * 启动主循环
     */
    private void startMainLoop() {
        mainLoopThread = new Thread(() -> {
            Log.d(TAG, "Main loop started");

            while (isRunning) {
                try {
                    // 1. 采集原始输入
                    RawInput rawInput = inputCollector.collect();

                    // 2. 使用输入解释器将原始输入转换为标准化事件
                    // 从区域解析器获取当前布局快照
                    LayoutSnapshot currentLayout = regionResolver.getCurrentLayoutSnapshot();
                    NormalizedEvent event = inputInterpreter.interpret(rawInput, currentLayout);
                    
                    // 3. 使用事件标准化器处理标准化事件
                    if (event != null) {
                        eventNormalizer.processEvent(event);
                        
                        // 4. 将处理后的事件发送到JS层
                        NormalizedEvent normalizedEvent;
                        while ((normalizedEvent = eventNormalizer.getNextEvent()) != null) {
                            if (scriptEngine instanceof JsInputScriptEngine) {
                                ((JsInputScriptEngine) scriptEngine).sendNormalizedEventToJs(normalizedEvent);
                            }
                        }
                    }

                    // 5. 更新脚本引擎（保留原有逻辑，确保兼容旧版本脚本）
                    InputState inputState = new InputState();
                    boolean updateSuccess = scriptEngine.update(rawInput, inputState);

                    // 6. 处理自动回滚
                    if (!updateSuccess) {
                        // 发送运行时错误广播
                        Intent errorIntent = new Intent(RuntimeEvents.ACTION_RUNTIME_ERROR);
                        errorIntent.putExtra(RuntimeEvents.EXTRA_ERROR_TYPE, RuntimeEvents.ERROR_TYPE_RUNTIME_ERROR);
                        sendBroadcast(errorIntent);

                        boolean rollbackSuccess = profileManager.autoRollback();
                        if (rollbackSuccess) {
                            // 发送配置文件回滚成功广播
                            Intent rollbackIntent = new Intent(RuntimeEvents.ACTION_PROFILE_ROLLBACK);
                            sendBroadcast(rollbackIntent);
                        }
                    }

                    // 7. 设置单调递增的 frameId
                    inputState.setFrameId(frameIdCounter);
                    // 递增 frameId，确保下次生成更大的值
                    frameIdCounter++;

                    // 8. 发送输入状态到WebSocket
                    webSocketClient.sendInputState(inputState);

                    // 9. 更新浮窗状态
                    updateFloatWindowStatus(frameIdCounter - 1, updateSuccess);

                    // 10. 等待下一帧
                    Thread.sleep(16); // 约60 FPS

                } catch (Exception e) {
                    Log.e(TAG, "Error in main loop", e);
                    // 发生错误时尝试回滚
                    profileManager.autoRollback();
                    // 更新浮窗错误状态
                    updateFloatWindowStatus(frameIdCounter - 1, false);
                }
            }

            Log.d(TAG, "Main loop stopped");
        });

        // 设置运行标志为true，确保主循环持续运行
        isRunning = true;
        mainLoopThread.start();
    }

    /**
     * 更新浮窗状态
     */
    private void updateFloatWindowStatus(long frameId, boolean isRunningNormally) {
        // 使用Handler更新UI
        uiHandler.post(() -> {
            if (floatWindowManager != null && floatWindowManager.isFloatWindowShowing()) {
                String statusText;
                // 区分脚本执行错误和正常的未连接状态
                if (isRunningNormally) {
                    // 脚本执行正常
                    if (webSocketClient.isConnected()) {
                        statusText = "运行中";
                    } else {
                        statusText = "未连接";
                    }
                } else {
                    // 脚本执行错误
                    statusText = "脚本错误";
                }
                floatWindowManager.updateStatusText(statusText);
            }
        });
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
    
    /**
     * 显示连接错误提示
     */
    private void showConnectionError() {
        // 使用Handler在UI线程显示错误提示
        uiHandler.post(() -> {
            if (floatWindowManager != null) {
                floatWindowManager.showConnectionError();
            }
        });
    }
}
