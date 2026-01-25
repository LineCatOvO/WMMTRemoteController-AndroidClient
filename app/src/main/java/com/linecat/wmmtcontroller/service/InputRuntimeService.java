package com.linecat.wmmtcontroller.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.input.InputScriptEngine;
import com.linecat.wmmtcontroller.input.JsInputScriptEngine;
import com.linecat.wmmtcontroller.input.ProfileManager;
import com.linecat.wmmtcontroller.input.SafetyController;
import com.linecat.wmmtcontroller.input.ScriptProfile;
import com.linecat.wmmtcontroller.layer.ConversionLayer;
import com.linecat.wmmtcontroller.layer.InputAbstractionLayer;
import com.linecat.wmmtcontroller.layer.MappingLayer;
import com.linecat.wmmtcontroller.layer.NetworkLayer;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer;
import com.linecat.wmmtcontroller.layer.UIInputLayer;
import com.linecat.wmmtcontroller.monitor.SystemMonitor;
import com.linecat.wmmtcontroller.monitor.SystemMonitor.ControlState;
import com.linecat.wmmtcontroller.monitor.SystemMonitor.SafetyState;

/**
 * 输入运行时服务
 * 负责管理五个层的生命周期，协调各层之间的交互
 */
public class InputRuntimeService extends Service {
    private static final String TAG = "InputRuntimeService";
    private static final String CHANNEL_ID = "InputRuntimeService";
    private static final int NOTIFICATION_ID = 1;

    // 运行时组件
    private RuntimeConfig runtimeConfig;
    private InputScriptEngine scriptEngine;
    private ProfileManager profileManager;
    private SafetyController safetyController;
    private android.os.Handler uiHandler;

    // 五个层
    private PlatformAdaptationLayer platformAdaptationLayer;
    private UIInputLayer uiInputLayer;
    private ConversionLayer conversionLayer;
    private MappingLayer mappingLayer;
    private NetworkLayer networkLayer;

    // 运行状态
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "InputRuntimeService onCreate");

        // 创建通知渠道
        createNotificationChannel();

        // 初始化UI Handler
        uiHandler = new android.os.Handler(getMainLooper());

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

            // 显示悬浮球并更新状态
            uiInputLayer.start();
            uiInputLayer.updateFloatWindowStatus("服务运行中");

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

        // 停止所有层
        stopAllLayers();

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
     * 初始化组件
     */
    private void initializeComponents() {
        Log.d(TAG, "Initializing components");

        // 创建运行时配置
        runtimeConfig = new RuntimeConfig(this);

        // 创建脚本引擎
        scriptEngine = new JsInputScriptEngine(this);

        // 创建配置文件管理器
        profileManager = new ProfileManager(this, scriptEngine);

        // 初始化各层
        initializeLayers();

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

        Log.d(TAG, "All components initialized");
    }

    /**
     * 初始化各层
     */
    private void initializeLayers() {
        Log.d(TAG, "Initializing layers");

        // 创建网络层
        networkLayer = new NetworkLayer(this);
        networkLayer.setRuntimeConfig(runtimeConfig);
        networkLayer.init();

        // 创建映射层
        mappingLayer = new MappingLayer(this);
        mappingLayer.setTransportController(networkLayer.getTransportController());
        mappingLayer.init();

        // 创建转换层
        conversionLayer = new ConversionLayer(this);
        conversionLayer.init();

        // 创建UI输入层
        uiInputLayer = new UIInputLayer(this);
        uiInputLayer.setTransportController(networkLayer.getTransportController());
        uiInputLayer.init();

        // 创建平台适配层和输入抽象层
        InputAbstractionLayer inputAbstractionLayer = new InputAbstractionLayer(new InputAbstractionLayer.OutputSink() {
            @Override
            public void onPointerFrame(InputAbstractionLayer.PointerFrame frame) {
                // 处理指针帧（根据需要实现）
            }
            
            @Override
            public void onGyroFrame(InputAbstractionLayer.GyroFrame frame) {
                // 处理陀螺仪帧（根据需要实现）
            }
        });
        
        platformAdaptationLayer = new PlatformAdaptationLayer(this, inputAbstractionLayer);
        
        // 注意：不再需要设置依赖项、初始化或获取交互捕获器
        // 新的平台适配层设计不再使用这些方法

        Log.d(TAG, "All layers initialized");
    }

    /**
     * 启动所有层
     */
    private void startAllLayers() {
        Log.d(TAG, "Starting all layers");

        // 启动网络层
        networkLayer.start();

        // 启动映射层
        mappingLayer.start();

        // 启动转换层
        conversionLayer.start();

        // 启动平台适配层的 overlay
        platformAdaptationLayer.startOverlay();

        Log.d(TAG, "All layers started");
    }

    /**
     * 停止所有层
     */
    private void stopAllLayers() {
        Log.d(TAG, "Stopping all layers");

        // 停止平台适配层的 overlay
        if (platformAdaptationLayer != null) {
            platformAdaptationLayer.stopOverlay();
        }

        // 停止转换层
        if (conversionLayer != null) {
            conversionLayer.stop();
        }

        // 停止映射层
        if (mappingLayer != null) {
            mappingLayer.stop();
        }

        // 停止网络层
        if (networkLayer != null) {
            networkLayer.stop();
        }

        Log.d(TAG, "All layers stopped");
    }

    /**
     * 清理所有层
     */
    private void destroyAllLayers() {
        Log.d(TAG, "Destroying all layers");

        // 平台适配层不需要显式销毁，资源管理通过 stopOverlay() 处理
        platformAdaptationLayer = null;

        // 销毁UI输入层
        if (uiInputLayer != null) {
            uiInputLayer.destroy();
            uiInputLayer = null;
        }

        // 销毁转换层
        if (conversionLayer != null) {
            conversionLayer.destroy();
            conversionLayer = null;
        }

        // 销毁映射层
        if (mappingLayer != null) {
            mappingLayer.destroy();
            mappingLayer = null;
        }

        // 销毁网络层
        if (networkLayer != null) {
            networkLayer.destroy();
            networkLayer = null;
        }

        Log.d(TAG, "All layers destroyed");
    }

    /**
     * 清理组件
     */
    private void cleanupComponents() {
        Log.d(TAG, "Cleaning up components");

        // 销毁所有层
        destroyAllLayers();

        // 清理安全控制器
        if (safetyController != null) {
            safetyController.destroy();
            safetyController = null;
        }

        // 清理脚本引擎
        if (scriptEngine != null) {
            scriptEngine.shutdown();
            scriptEngine = null;
        }

        // 清理配置文件管理器
        if (profileManager != null) {
            profileManager.unloadCurrentProfile();
            profileManager = null;
        }

        Log.d(TAG, "All components cleaned up");
    }

    /**
     * 启动事件驱动机制
     */
    private void startMainLoop() {
        // 设置运行标志为true
        isRunning = true;

        // 启动所有层
        startAllLayers();
    }

    /**
     * 停止事件驱动机制
     */
    private void stopMainLoop() {
        isRunning = false;
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
     * 切换到新版布局引擎
     */
    public void switchToNewEngine() {
        if (mappingLayer != null) {
            mappingLayer.switchToNewEngine();
        }
    }

    /**
     * 切换回旧版布局引擎
     */
    public void switchToLegacyEngine() {
        if (mappingLayer != null) {
            mappingLayer.switchToLegacyEngine();
        }
    }

}
