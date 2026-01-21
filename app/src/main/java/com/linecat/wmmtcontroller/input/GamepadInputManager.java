package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.linecat.wmmtcontroller.model.RawInput;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏手柄输入管理器
 * 负责采集游戏手柄输入并转换为标准格式
 */
public class GamepadInputManager {
    private static final String TAG = "GamepadInputManager";
    
    // 标准轴名称映射
    private static final Map<Integer, String> AXIS_NAME_MAP = new HashMap<Integer, String>() {
        {
            put(MotionEvent.AXIS_X, "LX");          // 左摇杆 X
            put(MotionEvent.AXIS_Y, "LY");          // 左摇杆 Y
            put(MotionEvent.AXIS_Z, "RX");          // 右摇杆 X
            put(MotionEvent.AXIS_RZ, "RY");         // 右摇杆 Y
            put(MotionEvent.AXIS_LTRIGGER, "LT");   // 左扳机
            put(MotionEvent.AXIS_RTRIGGER, "RT");   // 右扳机
            put(MotionEvent.AXIS_HAT_X, "DPadX");   // 方向键 X
            put(MotionEvent.AXIS_HAT_Y, "DPadY");   // 方向键 Y
        }
    };
    
    // 标准按键名称映射
    private static final Map<Integer, String> BUTTON_NAME_MAP = new HashMap<Integer, String>() {
        {
            put(KeyEvent.KEYCODE_BUTTON_A, "A");     // A 键
            put(KeyEvent.KEYCODE_BUTTON_B, "B");     // B 键
            put(KeyEvent.KEYCODE_BUTTON_X, "X");     // X 键
            put(KeyEvent.KEYCODE_BUTTON_Y, "Y");     // Y 键
            put(KeyEvent.KEYCODE_BUTTON_L1, "L1");   // 左肩键
            put(KeyEvent.KEYCODE_BUTTON_R1, "R1");   // 右肩键
            put(KeyEvent.KEYCODE_BUTTON_L2, "L2");   // 左扳机键
            put(KeyEvent.KEYCODE_BUTTON_R2, "R2");   // 右扳机键
            put(KeyEvent.KEYCODE_BUTTON_START, "Start");   // 开始键
            put(KeyEvent.KEYCODE_BUTTON_SELECT, "Select"); // 选择键
            put(KeyEvent.KEYCODE_BUTTON_THUMBL, "L3");     // 左摇杆按下
            put(KeyEvent.KEYCODE_BUTTON_THUMBR, "R3");     // 右摇杆按下
            put(KeyEvent.KEYCODE_HOME, "Home");     // 主页键
        }
    };
    
    private Context context;
    private InputManager inputManager;
    private Handler mainHandler;
    
    // 已连接的游戏手柄
    private Map<Integer, InputDevice> connectedGamepads = new ConcurrentHashMap<>();
    
    // 回调接口
    private OnGamepadInputListener listener;
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public GamepadInputManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        
        // 初始化已连接的游戏手柄
        initializeConnectedGamepads();
    }
    
    /**
     * 初始化已连接的游戏手柄
     */
    private void initializeConnectedGamepads() {
        int[] deviceIds = inputManager.getInputDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice device = inputManager.getInputDevice(deviceId);
            if (isGamepad(device)) {
                connectedGamepads.put(deviceId, device);
                Log.d(TAG, "Gamepad connected: " + device.getName());
            }
        }
    }
    
    /**
     * 检查设备是否为游戏手柄
     * @param device 输入设备
     * @return 是否为游戏手柄
     */
    private boolean isGamepad(InputDevice device) {
        if (device == null) {
            return false;
        }
        
        int sources = device.getSources();
        return (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
               (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
               (sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD;
    }
    
    /**
     * 处理按键事件
     * @param event 按键事件
     * @param rawInput 原始输入对象
     */
    public void handleKeyEvent(KeyEvent event, RawInput rawInput) {
        if (!isGamepad(event.getDevice())) {
            return;
        }
        
        int keyCode = event.getKeyCode();
        boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
        
        // 转换为标准按键名称
        String buttonName = BUTTON_NAME_MAP.get(keyCode);
        if (buttonName != null) {
            // 更新游戏手柄按键状态
            rawInput.getGamepad().setButton(buttonName, pressed);
            
            if (listener != null) {
                listener.onGamepadInput(rawInput);
            }
        }
    }
    
    /**
     * 处理运动事件（轴）
     * @param event 运动事件
     * @param rawInput 原始输入对象
     */
    public void handleMotionEvent(MotionEvent event, RawInput rawInput) {
        if (!isGamepad(event.getDevice())) {
            return;
        }
        
        // 处理已知的轴
        for (Map.Entry<Integer, String> entry : AXIS_NAME_MAP.entrySet()) {
            int axis = entry.getKey();
            String axisName = entry.getValue();
            
            // 获取轴值
            float value = event.getAxisValue(axis);
            
            // 更新游戏手柄轴状态
            rawInput.getGamepad().setAxis(axisName, value);
        }
        
        if (listener != null) {
            listener.onGamepadInput(rawInput);
        }
    }
    
    /**
     * 注册游戏手柄输入监听器
     * @param listener 监听器
     */
    public void setOnGamepadInputListener(OnGamepadInputListener listener) {
        this.listener = listener;
    }
    
    /**
     * 获取已连接的游戏手柄数量
     * @return 游戏手柄数量
     */
    public int getConnectedGamepadCount() {
        return connectedGamepads.size();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        connectedGamepads.clear();
        listener = null;
    }
    
    /**
     * 游戏手柄输入监听器接口
     */
    public interface OnGamepadInputListener {
        /**
         * 当游戏手柄输入更新时调用
         * @param rawInput 更新后的原始输入对象
         */
        void onGamepadInput(RawInput rawInput);
    }
}