package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 输入控制器
 * 负责采集原始输入：触控、陀螺仪、按键、游戏手柄等
 */
public class InputController implements SensorEventListener {
    /**
     * 输入事件监听器接口
     */
    public interface InputEventListener {
        void onInputEvent();
    }
    private static final String TAG = "InputController";
    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor gyroscopeSensor;
    private final Sensor accelerometerSensor;
    
    // 输入状态
    private RawInput currentRawInput;
    // 输入事件监听器
    private InputEventListener inputEventListener;
    
    public InputController(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.currentRawInput = new RawInput();
    }

    /**
     * 启动输入采集
     */
    public void start() {
        // 注册传感器监听器
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        
        // 初始化触控输入监听（通过系统事件）
        
        // 初始化按键输入监听
        
        // 初始化游戏手柄输入监听
        
        Log.d(TAG, "Input controller started");
    }

    /**
     * 停止输入采集
     */
    public void stop() {
        // 注销传感器监听器
        sensorManager.unregisterListener(this);
        
        // 清理触控输入监听
        
        // 清理按键输入监听
        
        // 清理游戏手柄输入监听
        
        // 重置输入状态
        reset();
        
        Log.d(TAG, "Input controller stopped");
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
     * 设置输入事件监听器
     */
    public void setInputEventListener(InputEventListener listener) {
        this.inputEventListener = listener;
    }

    /**
     * 触发输入事件
     */
    private void triggerInputEvent() {
        if (inputEventListener != null) {
            inputEventListener.onInputEvent();
        }
    }

    /**
     * 处理触控输入
     */
    public void processTouchInput(float x, float y, boolean isPressed) {
        synchronized (this) {
            currentRawInput.setTouchX(x);
            currentRawInput.setTouchY(y);
            currentRawInput.setTouchPressed(isPressed);
            // 触发输入事件
            triggerInputEvent();
        }
    }

    /**
     * 处理按键输入
     */
    public void processKeyInput(int keyCode, boolean isPressed) {
        synchronized (this) {
            // 处理按键输入
            // 触发输入事件
            triggerInputEvent();
        }
    }

    /**
     * 处理游戏手柄输入
     */
    public void processGamepadInput(int axis, float value, int button, boolean isPressed) {
        synchronized (this) {
            // 处理游戏手柄输入
            // 触发输入事件
            triggerInputEvent();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
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
            // 触发输入事件
            triggerInputEvent();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 传感器精度变化，暂不处理
    }
}