package com.linecat.wmmtcontroller.layer.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

/**
 * 测试专用Activity，用于提供windowToken，支持ACTIVITY_PANEL模式的PlatformAdaptationLayer测试
 */
public class TestHostActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 创建一个空的布局，用于接收触摸事件
        setContentView(android.R.layout.activity_list_item);
    }

    /**
     * 获取Activity的windowToken，用于绑定测试窗口
     * @return Activity的windowToken
     */
    public android.os.IBinder getWindowToken() {
        return getWindow().getDecorView().getWindowToken();
    }
}