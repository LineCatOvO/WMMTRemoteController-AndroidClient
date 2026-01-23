package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.service.TransportController;

/**
 * 交互捕获器
 * 负责捕获和标准化原始输入：触控、陀螺仪、按键、游戏手柄等
 * 这是三层输入架构中的交互捕获层
 */
public class InteractionCapture {
    private static final String TAG = "InteractionCapture";
    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor gyroscopeSensor;
    private final Sensor accelerometerSensor;
    
    // 当前原始输入状态
    private RawInput currentRawInput;
    
    // 意图合成器
    private IntentComposer intentComposer;
    // 设备投影器
    private DeviceProjector deviceProjector;
    
    // 帧ID计数器
    private long frameIdCounter = 0;
    
    // 初始化标志
    private boolean isInitialized = false;
    
    public InteractionCapture(Context context, IntentComposer intentComposer, DeviceProjector deviceProjector) {
        this.context = context;
        this.intentComposer = intentComposer;
        this.deviceProjector = deviceProjector;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.currentRawInput = new RawInput();
        
        // 标记为已初始化
        this.isInitialized = true;
    }

    /**
     * 启动输入捕获
     */
    public void start() {
        // 注册传感器监听器
        sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        
        Log.d(TAG, "Interaction capture started");
    }

    /**
     * 停止输入捕获
     */
    public void stop() {
        // 注销传感器监听器
        sensorManager.unregisterListener(sensorEventListener);
        
        // 重置输入状态
        reset();
        
        Log.d(TAG, "Interaction capture stopped");
    }

    /**
     * 采集当前原始输入
     */
    public RawInput collect() {
        synchronized (this) {
            return new RawInput(currentRawInput); // 返回副本，避免并发问题
        }
    }
    
    /**
     * 获取当前原始输入状态（用于内部同步）
     * 注意：此方法返回直接引用，仅供内部同步使用
     */
    public RawInput getCurrentRawInput() {
        return currentRawInput;
    }

    /**
     * 处理触控输入
     */
    public void processTouchInput(float x, float y, boolean isPressed) {
        synchronized (this) {
            currentRawInput.setTouchX(x);
            currentRawInput.setTouchY(y);
            currentRawInput.setTouchPressed(isPressed);
            // 触发投影
            triggerProjection();
        }
    }

    /**
     * 处理按键输入
     */
    public void processKeyInput(int keyCode, boolean isPressed) {
        synchronized (this) {
            // 处理按键输入
            // 触发投影
            triggerProjection();
        }
    }

    /**
     * 处理游戏手柄输入
     */
    public void processGamepadInput(int axis, float value, int button, boolean isPressed) {
        synchronized (this) {
            // 处理游戏手柄输入
            // 触发投影
            triggerProjection();
        }
    }

    /**
     * 触发投影
     */
    public void triggerProjection() {
        if (isInitialized && intentComposer != null && deviceProjector != null) {
            // 1. 使用意图合成器处理输入
            RawInput processedInput = intentComposer.composeIntent(collect());
            // 2. 使用设备投影器将处理后的输入投影到设备
            deviceProjector.projectToDevice(processedInput, frameIdCounter++);
        }
    }

    /**
     * 重置输入状态
     */
    public void reset() {
        synchronized (this) {
            currentRawInput = new RawInput();
        }
    }
    
    // 传感器事件监听器
    private final android.hardware.SensorEventListener sensorEventListener = new android.hardware.SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (InteractionCapture.this) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    // 处理陀螺仪数据
                    currentRawInput.setGyroPitch(event.values[0]);
                    currentRawInput.setGyroRoll(event.values[1]);
                    currentRawInput.setGyroYaw(event.values[2]);
                } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    // 处理加速度计数据
                    currentRawInput.setAccelX(event.values[0]);
                    currentRawInput.setAccelY(event.values[1]);
                    currentRawInput.setAccelZ(event.values[2]);
                }
                // 触发投影
                triggerProjection();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // 传感器精度变化，暂不处理
        }
    };
}