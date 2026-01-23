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
import com.linecat.wmmtcontroller.floatwindow.OverlayController;
import com.linecat.wmmtcontroller.input.EventNormalizer;
import com.linecat.wmmtcontroller.input.InputInterpreter;
import com.linecat.wmmtcontroller.input.InputScriptEngine;
import com.linecat.wmmtcontroller.input.InteractionCapture;
import com.linecat.wmmtcontroller.input.IntentComposer;
import com.linecat.wmmtcontroller.input.DeviceProjector;

import com.linecat.wmmtcontroller.input.JsInputScriptEngine;
import com.linecat.wmmtcontroller.input.LayoutEngine;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.input.NormalizedEvent;
import com.linecat.wmmtcontroller.input.OutputController;
import com.linecat.wmmtcontroller.input.ProfileManager;
import com.linecat.wmmtcontroller.input.RegionResolver;
import com.linecat.wmmtcontroller.input.SafetyController;
import com.linecat.wmmtcontroller.input.ScriptProfile;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.monitor.SystemMonitor;
import com.linecat.wmmtcontroller.monitor.SystemMonitor.ControlState;
import com.linecat.wmmtcontroller.monitor.SystemMonitor.SafetyState;
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
    private InteractionCapture inputController;
    private LayoutEngine layoutEngine;
    private OutputController outputController;
    private SafetyController safetyController;
    private TransportController transportController;
    private OverlayController overlayController;
    // 输入服务层组件
    private InputInterpreter inputInterpreter;
    private EventNormalizer eventNormalizer;
    private RegionResolver regionResolver;
    // UI更新Handler
    private android.os.Handler uiHandler;

    // 运行状态
    private boolean isRunning = false;
    // Frame ID 计数器，用于生成单调递增的帧ID
    private long frameIdCounter = 1;
    // 输入处理标志，防止并发处理
    private boolean isProcessingInput = false;


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

        // 初始化广播接收器
        initConnectControlReceiver();
        initWebSocketStatusReceiver();

        // 初始化组件
        initializeComponents();

        // 初始化系统监控器
        SystemMonitor.getInstance().setControlState(ControlState.IDLE);
        SystemMonitor.getInstance().setSafetyState(SafetyState.SAFE);

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "InputRuntimeService onStartCommand");

        // 启动主循环
        if (!isRunning) {
            startMainLoop();

            // 显示悬浮球
            overlayController.showOverlay();
            overlayController.updateStatus("服务运行中");

            // 更新系统监控状态
            SystemMonitor.getInstance().setControlState(ControlState.RUNNING);

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

        // 隐藏悬浮球
        overlayController.hideOverlay();

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

        // 更新系统监控状态
        SystemMonitor.getInstance().setControlState(ControlState.IDLE);
        SystemMonitor.getInstance().setSafetyState(SafetyState.SAFE);

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
                            transportController.connect();
                            break;
                        case "com.linecat.wmmtcontroller.ACTION_STOP_CONNECT":
                            Log.d(TAG, "Received stop connect broadcast");
                            // 断开WebSocket连接
                            transportController.disconnect();
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
                            // 通信断开时触发安全清零
                            safetyController.triggerSafetyClear();
                            
                            // 检查是否是因为连接失败导致的断开
                            // 如果当前尝试连接但未成功，显示连接错误
                            if (transportController != null && !transportController.isConnected()) {
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

        // 创建输出控制器
        outputController = new OutputController();
        
        // 创建布局引擎
        layoutEngine = new LayoutEngine(outputController);
        layoutEngine.setContext(this);
        layoutEngine.init();

        // 创建脚本引擎
        scriptEngine = new JsInputScriptEngine(this);

        // 创建配置文件管理器
        profileManager = new ProfileManager(this, scriptEngine);
        // 设置布局引擎
        profileManager.setLayoutEngine(layoutEngine);

        // 创建意图合成器
        IntentComposer intentComposer = new IntentComposer();
        
        // 创建设备投影器
        DeviceProjector deviceProjector = new DeviceProjector(transportController, layoutEngine);
        
        // 创建交互捕获器
        inputController = new InteractionCapture(this, intentComposer, deviceProjector);
        // 将当前服务注册为输入事件监听器
        // 注意：现在InteractionCapture不再实现InputEventListener接口，所以我们不再需要设置监听器

        // 创建悬浮球控制器
        overlayController = new OverlayController(this);
        
        // 将布局引擎的当前布局传递给渲染器
        LayoutSnapshot currentLayout = layoutEngine.getCurrentLayout();
        if (currentLayout != null) {
            overlayController.setCurrentLayout(currentLayout);
        }
        
        // 将输入控制器设置到布局渲染器
        overlayController.setInputController(inputController);
        
        // 创建安全控制器
        safetyController = new SafetyController(outputController);
        
        // 创建输入服务层组件
        inputInterpreter = new InputInterpreter();
        eventNormalizer = new EventNormalizer();
        regionResolver = new RegionResolver(this);

        // 创建传输控制器
        transportController = new TransportController(this, runtimeConfig);
        transportController.init();

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

        // 初始化输入控制器
        inputController.start();

        Log.d(TAG, "All components initialized");
    }

    /**
     * 清理组件
     */
    private void cleanupComponents() {
        // 停止输入控制器
        if (inputController != null) {
            inputController.stop();
        }
        
        // 清理安全控制器
        if (safetyController != null) {
            safetyController.destroy();
        }
        
        // 清理布局引擎
        if (layoutEngine != null) {
            layoutEngine.reset();
        }
        
        // 清理输出控制器
        if (outputController != null) {
            outputController.clearAllOutputs();
        }
        
        // 清理悬浮球控制器
        if (overlayController != null) {
            overlayController.destroy();
        }
        
        // 清理传输控制器
        if (transportController != null) {
            transportController.cleanup();
        }
        
        // 清理脚本引擎
        if (scriptEngine != null) {
            scriptEngine.shutdown();
        }
        
        // 清理配置文件管理器
        if (profileManager != null) {
            profileManager.unloadCurrentProfile();
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
        
        Log.d(TAG, "All components cleaned up");
    }

    /**
     * 启动事件驱动机制
     */
    private void startMainLoop() {
        Log.d(TAG, "Event-driven input processing started");
        // 设置运行标志为true
        isRunning = true;
    }



    /**
     * 更新浮窗状态
     */
    private void updateFloatWindowStatus(long frameId, boolean isRunningNormally) {
        // 使用Handler更新UI
        uiHandler.post(() -> {
            String statusText;
            // 区分脚本执行错误和正常的未连接状态
            if (isRunningNormally) {
                // 脚本执行正常
                if (transportController.isConnected()) {
                    statusText = "运行中";
                } else {
                    statusText = "未连接";
                }
            } else {
                // 脚本执行错误
                statusText = "脚本错误";
            }
            overlayController.updateStatus(statusText);
        });
    }

    /**
     * 停止事件驱动机制
     */
    private void stopMainLoop() {
        isRunning = false;
        Log.d(TAG, "Event-driven input processing stopped");
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
            overlayController.showConnectionError();
        });
    }


}
