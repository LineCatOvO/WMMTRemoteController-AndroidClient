package com.linecat.wmmtcontroller;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.linecat.wmmtcontroller.service.InputRuntimeService;

/**
 * 主活动
 * 仅作为Service启动器，负责启动/停止输入运行时服务
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // UI组件
    private TextView statusText;
    private Button startButton;
    private Button stopButton;
    private boolean isServiceRunning = false;
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        statusText = findViewById(R.id.status_text);
        startButton = findViewById(R.id.btn_start_service);
        stopButton = findViewById(R.id.btn_stop_service);
        
        // 设置按钮点击事件
        startButton.setOnClickListener(v -> startInputService());
        stopButton.setOnClickListener(v -> stopInputService());
        
        // 检查并请求浮窗权限
        checkOverlayPermission();
        
        // 更新初始状态
        updateStatus();
    }
    
    /**
     * 检查并请求浮窗权限
     */
    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission not granted, requesting...");
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            } else {
                Log.d(TAG, "Overlay permission already granted");
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Log.d(TAG, "Overlay permission granted");
                } else {
                    Log.d(TAG, "Overlay permission denied");
                }
            }
        }
    }
    
    /**
     * 启动输入运行时服务
     */
    private void startInputService() {

        Intent intent = new Intent(this, InputRuntimeService.class);
        startService(intent);
        isServiceRunning = true;
        updateStatus();
    }
    
    /**
     * 停止输入运行时服务
     */
    private void stopInputService() {

        Intent intent = new Intent(this, InputRuntimeService.class);
        stopService(intent);
        isServiceRunning = false;
        updateStatus();
    }
    
    /**
     * 更新服务状态UI
     */
    private void updateStatus() {
        runOnUiThread(() -> {
            if (isServiceRunning) {
                statusText.setText("服务状态: 已启动");
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            } else {
                statusText.setText("服务状态: 已停止");
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
    }
}