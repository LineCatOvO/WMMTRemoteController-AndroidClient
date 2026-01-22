package com.linecat.wmmtcontroller.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.linecat.wmmtcontroller.model.RawInput;

/**
 * 输入采集器
 * 负责采集原始输入数据，包括传感器、触摸、按键等
 * 实现RawInputCollector接口，直接与Android系统交互
 */
public class InputCollector implements RawInputCollector {
    private static final String TAG = "InputCollector";
    
    private Context context;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    
    // 输入数据
    private RawInput currentInput;
    
    // 传感器监听器
    private SensorEventListener sensorEventListener;
    
    /**
     * 初始化输入采集器
     * @param context 上下文
     */
    public void init(Context context) {
        this.context = context;
        this.currentInput = new RawInput();
        
        // 初始化传感器
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        // 创建传感器监听器
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                synchronized (currentInput) {
                    if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        // 更新陀螺仪数据
                        currentInput.setGyroPitch(event.values[0]);
                        currentInput.setGyroRoll(event.values[1]);
                        currentInput.setGyroYaw(event.values[2]);
                    } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        // 更新加速度计数据 - 暂时注释，等待RawInput类添加相关字段
                        // currentInput.setAccelX(event.values[0]);
                        // currentInput.setAccelY(event.values[1]);
                        // currentInput.setAccelZ(event.values[2]);
                    }
                }
            }
            
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // 忽略精度变化
            }
        };
        
        // 注册传感器监听器
        if (gyroscope != null) {
            sensorManager.registerListener(
                sensorEventListener,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            );
        }
        
        if (accelerometer != null) {
            sensorManager.registerListener(
                sensorEventListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            );
        }
        
        Log.d(TAG, "InputCollector initialized");
    }
    
    /**
     * 采集原始输入数据
     * @return 原始输入数据
     */
    public RawInput collect() {
        synchronized (currentInput) {
            // 创建副本，避免并发问题
            RawInput copy = new RawInput();
            copy.setGyroPitch(currentInput.getGyroPitch());
            copy.setGyroRoll(currentInput.getGyroRoll());
            copy.setGyroYaw(currentInput.getGyroYaw());
            // 以下方法在RawInput类中不存在，暂时注释
            // copy.setAccelX(currentInput.getAccelX());
            // copy.setAccelY(currentInput.getAccelY());
            // copy.setAccelZ(currentInput.getAccelZ());
            copy.setButtonA(currentInput.isButtonA());
            copy.setButtonB(currentInput.isButtonB());
            // copy.setButtonX(currentInput.isButtonX());
            // copy.setButtonY(currentInput.isButtonY());
            // copy.setButtonStart(currentInput.isButtonStart());
            // copy.setButtonSelect(currentInput.isButtonSelect());
            // copy.setButtonL(currentInput.isButtonL());
            // copy.setButtonR(currentInput.isButtonR());
            // copy.setDpadUp(currentInput.isDpadUp());
            // copy.setDpadDown(currentInput.isDpadDown());
            // copy.setDpadLeft(currentInput.isDpadLeft());
            // copy.setDpadRight(currentInput.isDpadRight());
            copy.setTouchPressed(currentInput.isTouchPressed());
            copy.setTouchX(currentInput.getTouchX());
            copy.setTouchY(currentInput.getTouchY());
            
            return copy;
        }
    }
    
    /**
     * 更新触摸输入
     * @param pressed 是否按下
     * @param x X坐标
     * @param y Y坐标
     */
    public void updateTouch(boolean pressed, float x, float y) {
        synchronized (currentInput) {
            currentInput.setTouchPressed(pressed);
            currentInput.setTouchX(x);
            currentInput.setTouchY(y);
        }
    }
    
    /**
     * 更新按钮输入
     * @param buttonName 按钮名称
     * @param pressed 是否按下
     */
    public void updateButton(String buttonName, boolean pressed) {
        synchronized (currentInput) {
            switch (buttonName) {
                case "A":
                    currentInput.setButtonA(pressed);
                    break;
                case "B":
                    currentInput.setButtonB(pressed);
                    break;
                case "C":
                    currentInput.setButtonC(pressed);
                    break;
                case "D":
                    currentInput.setButtonD(pressed);
                    break;
                // 以下按钮在RawInput类中不存在，暂时注释
                /*
                case "X":
                    currentInput.setButtonX(pressed);
                    break;
                case "Y":
                    currentInput.setButtonY(pressed);
                    break;
                case "Start":
                    currentInput.setButtonStart(pressed);
                    break;
                case "Select":
                    currentInput.setButtonSelect(pressed);
                    break;
                case "L":
                    currentInput.setButtonL(pressed);
                    break;
                case "R":
                    currentInput.setButtonR(pressed);
                    break;
                case "DpadUp":
                    currentInput.setDpadUp(pressed);
                    break;
                case "DpadDown":
                    currentInput.setDpadDown(pressed);
                    break;
                case "DpadLeft":
                    currentInput.setDpadLeft(pressed);
                    break;
                case "DpadRight":
                    currentInput.setDpadRight(pressed);
                    break;
                */
            }
        }
    }
    
    // ===== RawInputCollector 接口实现 =====
    
    @Override
    public void onTouch(android.view.MotionEvent event) {
        // 处理触摸事件
        // 从MotionEvent中提取触摸状态和坐标
        boolean pressed = event.getAction() != android.view.MotionEvent.ACTION_UP && 
                         event.getAction() != android.view.MotionEvent.ACTION_CANCEL;
        
        float x = event.getX();
        float y = event.getY();
        
        // 更新触摸输入
        updateTouch(pressed, x, y);
    }
    
    @Override
    public void onKey(android.view.KeyEvent event) {
        // 处理按键事件
        // 从KeyEvent中提取按键状态和名称
        boolean pressed = event.getAction() == android.view.KeyEvent.ACTION_DOWN;
        String buttonName = getButtonName(event.getKeyCode());
        
        if (buttonName != null) {
            // 更新按钮输入
            updateButton(buttonName, pressed);
        }
    }
    
    @Override
    public void onGamepad(android.view.InputEvent event) {
        // 处理游戏手柄事件
        // 目前暂未实现，只记录日志
        Log.d(TAG, "onGamepad: " + event);
    }
    
    /**
     * 根据按键代码获取按钮名称
     * @param keyCode 按键代码
     * @return 按钮名称，找不到返回null
     */
    private String getButtonName(int keyCode) {
        // 简单的按键映射，实际应用中应该根据配置进行映射
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_A:
                return "A";
            case android.view.KeyEvent.KEYCODE_B:
                return "B";
            case android.view.KeyEvent.KEYCODE_C:
                return "C";
            case android.view.KeyEvent.KEYCODE_D:
                return "D";
            default:
                return null;
        }
    }
    
    /**
     * 关闭输入采集器
     */
    public void shutdown() {
        // 注销传感器监听器
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        
        Log.d(TAG, "InputCollector shutdown");
    }
}