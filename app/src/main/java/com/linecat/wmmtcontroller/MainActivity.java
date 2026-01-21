package com.linecat.wmmtcontroller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.linecat.wmmtcontroller.service.InputRuntimeService;

/**
 * 主活动
 * 负责启动/停止输入运行时服务，并提供UI控制
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // UI组件
    private TextView statusText;
    private boolean isServiceRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件 - 暂时注释，等待布局文件添加相关组件
        // statusText = findViewById(R.id.connection_status);
        
        // 设置按钮点击事件 - 暂时注释，等待布局文件添加相关组件
        /*
        findViewById(R.id.btn_start_service).setOnClickListener(v -> startInputService());
        findViewById(R.id.btn_stop_service).setOnClickListener(v -> stopInputService());
        */
        
        // 更新初始状态
        // updateStatus();
        
        // 直接启动服务进行测试
        startInputService();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 应用销毁时停止服务
        stopInputService();
    }
    
    /**
     * 启动输入运行时服务
     */
    private void startInputService() {
        Log.d(TAG, "Starting input runtime service...");
        Intent intent = new Intent(this, InputRuntimeService.class);
        startService(intent);
        isServiceRunning = true;
        updateStatus();
    }
    
    /**
     * 停止输入运行时服务
     */
    private void stopInputService() {
        Log.d(TAG, "Stopping input runtime service...");
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
                statusText.setText("输入运行时服务: 已启动");
            } else {
                statusText.setText("输入运行时服务: 已停止");
            }
        });
    }
}