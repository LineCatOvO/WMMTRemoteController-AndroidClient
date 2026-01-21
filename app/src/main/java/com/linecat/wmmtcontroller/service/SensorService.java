package com.linecat.wmmtcontroller.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 传感器服务
 * 负责采集陀螺仪数据
 */
public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "SensorService";
    
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private RawInput currentRawInput;
    private final IBinder binder = new LocalBinder();
    private SensorDataListener listener;
    
    public interface SensorDataListener {
        void onSensorDataUpdate(RawInput rawInput);
    }
    
    public class LocalBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        currentRawInput = new RawInput();
        initializeSensors();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSensorCollection();
    }
    
    /**
     * 初始化传感器
     */
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscopeSensor != null) {
                Log.d(TAG, "陀螺仪传感器已找到");
            } else {
                Log.e(TAG, "未找到陀螺仪传感器");
            }
        }
    }
    
    /**
     * 开始传感器数据采集
     */
    public void startSensorCollection() {
        if (sensorManager != null && gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "传感器数据采集已开始");
        }
    }
    
    /**
     * 停止传感器数据采集
     */
    public void stopSensorCollection() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "传感器数据采集已停止");
        }
    }
    
    /**
     * 设置传感器数据监听器
     * @param listener 监听器
     */
    public void setSensorDataListener(SensorDataListener listener) {
        this.listener = listener;
    }
    
    /**
     * 清理传感器数据监听器
     */
    public void clearSensorDataListener() {
        this.listener = null;
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // 更新陀螺仪数据
            currentRawInput.setGyroPitch(event.values[0]);
            currentRawInput.setGyroRoll(event.values[1]);
            currentRawInput.setGyroYaw(event.values[2]);
            
            // 通知监听器
            if (listener != null) {
                listener.onSensorDataUpdate(currentRawInput);
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 传感器精度变化时的处理
        Log.d(TAG, "传感器精度变化: " + accuracy);
    }
    
    /**
     * 获取当前原始输入数据
     * @return 原始输入数据
     */
    public RawInput getCurrentRawInput() {
        return currentRawInput;
    }
}