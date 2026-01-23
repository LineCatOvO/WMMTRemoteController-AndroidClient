package com.linecat.wmmtcontroller.floatwindow;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.linecat.wmmtcontroller.R;
import com.linecat.wmmtcontroller.input.LayoutEngine;
import com.linecat.wmmtcontroller.input.LayoutRenderer;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.model.ConnectionInfo;
import com.linecat.wmmtcontroller.service.RuntimeConfig;
import com.linecat.wmmtcontroller.service.RuntimeEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * 浮窗管理器
 * 负责浮窗的创建、显示、隐藏和更新
 */
public class FloatWindowManager {
    private static final String TAG = "FloatWindowManager";

    // 浮窗视图
    private View floatView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;

    // 上下文
    private Context context;

    // 浮窗是否显示
    private boolean isShowing = false;

    // 单例实例
    private static FloatWindowManager instance;

    /**
     * 私有构造方法
     */
    private FloatWindowManager(Context context) {
        this.context = context.getApplicationContext();
        initFloatWindow();
    }

    /**
     * 获取单例实例
     */
    public static synchronized FloatWindowManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloatWindowManager(context);
        }
        return instance;
    }

    // 弹出菜单显示状态
    private boolean isPopupMenuShowing = false;

    // 布局管理面板显示状态
    private boolean isLayoutManagementPanelShowing = false;

    // 组件引用
    private View circleEntryView;
    private View popupMenuView;
    private View layoutManagementPanelView;
    private CheckBox layoutEnabledCheckbox;
    private ListView layoutsListView;
    private ArrayAdapter<String> layoutsAdapter;
    private List<String> layoutsList = new ArrayList<>();
    
    // 布局渲染器
    private LayoutRenderer layoutRenderer;
    private ViewGroup layoutRenderContainer;

    /**
     * 初始化浮窗
     */
    private void initFloatWindow() {
        // 获取WindowManager服务
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // 创建浮窗参数
        windowParams = new WindowManager.LayoutParams();

        // 设置浮窗类型
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 设置浮窗参数
        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        // 设置浮窗位置和大小
        windowParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowParams.x = 100;
        windowParams.y = 100;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // 加载浮窗布局
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        floatView = inflater.inflate(R.layout.float_window, null);

        // 获取组件引用
        circleEntryView = floatView.findViewById(R.id.ll_circle_entry);
        popupMenuView = floatView.findViewById(R.id.ll_popup_menu);
        layoutManagementPanelView = floatView.findViewById(R.id.ll_layout_management_panel);
        layoutEnabledCheckbox = floatView.findViewById(R.id.cb_layout_enabled);
        layoutsListView = floatView.findViewById(R.id.lv_layouts);

        // 初始化布局列表
        initLayoutsList();

        // 设置菜单项点击事件
        setupMenuItemListeners();

        // 设置布局管理事件监听
        setupLayoutManagementListeners();

        // 初始化布局渲染器
        initLayoutRenderer();

        // 设置浮窗触摸事件，实现拖拽功能和菜单显示控制
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY;
            private int paramX, paramY;
            private boolean isDragging = false;
            private boolean isClickingCircle = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = windowParams.x;
                        paramY = windowParams.y;
                        isDragging = false;
                        isClickingCircle = false;

                        // 检查点击位置是否在圆形入口内
                        int[] circleLocation = new int[2];
                        circleEntryView.getLocationOnScreen(circleLocation);
                        int circleLeft = circleLocation[0];
                        int circleTop = circleLocation[1];
                        int circleRight = circleLeft + circleEntryView.getWidth();
                        int circleBottom = circleTop + circleEntryView.getHeight();

                        int rawX = (int) event.getRawX();
                        int rawY = (int) event.getRawY();

                        if (rawX >= circleLeft && rawX <= circleRight && rawY >= circleTop && rawY <= circleBottom) {
                            isClickingCircle = true;
                        } else if (isPopupMenuShowing) {
                            // 点击的是其他区域，隐藏菜单
                            hidePopupMenu();
                        } else if (isLayoutManagementPanelShowing) {
                            // 点击的是其他区域，隐藏布局管理面板
                            hideLayoutManagementPanel();
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        // 如果移动距离超过阈值，视为拖拽
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isDragging = true;
                            windowParams.x = paramX + dx;
                            windowParams.y = paramY + dy;
                            // 更新浮窗位置
                            windowManager.updateViewLayout(floatView, windowParams);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        // 如果不是拖拽且点击的是圆形入口，触发点击事件
                        if (!isDragging && isClickingCircle) {
                            togglePopupMenu();
                        }
                        break;
                }
                // 总是返回true，消费所有事件，避免事件冒泡导致的冲突
                return true;
            }
        });
    }

    /**
     * 初始化布局渲染器
     */
    private void initLayoutRenderer() {
        // 创建布局渲染容器
        layoutRenderContainer = new FrameLayout(context);
        layoutRenderContainer.setLayoutParams(new FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        ));

        // 创建布局渲染器
        layoutRenderer = new LayoutRenderer(context);
        layoutRenderContainer.addView(layoutRenderer, new FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        ));

        // 将布局渲染容器添加到窗口管理器
        WindowManager.LayoutParams renderParams = new WindowManager.LayoutParams();
        renderParams.copyFrom(windowParams);
        renderParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        renderParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        renderParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        renderParams.gravity = Gravity.TOP | Gravity.LEFT;

        try {
            windowManager.addView(layoutRenderContainer, renderParams);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add layout render container: " + e.getMessage());
        }

        // 设置布局渲染器默认状态
        layoutRenderer.setLayoutEnabled(false);
    }

    /**
     * 设置菜单项点击事件
     */
    private void setupMenuItemListeners() {
        // 开始连接按钮点击事件
        floatView.findViewById(R.id.btn_start_connect).setOnClickListener(v -> {
            Log.d(TAG, "Start connect button clicked");

            // 检查连接信息是否完整
            if (!checkConnectionInfo()) {
                Toast.makeText(context, "请先填写完整的连接信息", Toast.LENGTH_SHORT).show();
                hidePopupMenu();
                return;
            }

            // 发送开始连接广播
            Intent intent = new Intent("com.linecat.wmmtcontroller.ACTION_START_CONNECT");
            context.sendBroadcast(intent);
            hidePopupMenu();
        });

        // 断开连接按钮点击事件
        floatView.findViewById(R.id.btn_stop_connect).setOnClickListener(v -> {
            Log.d(TAG, "Stop connect button clicked");
            // 发送断开连接广播
            Intent intent = new Intent("com.linecat.wmmtcontroller.ACTION_STOP_CONNECT");
            context.sendBroadcast(intent);
            hidePopupMenu();
        });

        // 显示设置面板按钮点击事件
        floatView.findViewById(R.id.btn_show_settings).setOnClickListener(v -> {
            Log.d(TAG, "Show settings button clicked");
            hidePopupMenu();
            showSettingsPanel();
        });

        // 布局管理按钮点击事件
        floatView.findViewById(R.id.btn_layout_management).setOnClickListener(v -> {
            Log.d(TAG, "Layout management button clicked");
            hidePopupMenu();
            showLayoutManagementPanel();
        });

        // 保存设置按钮点击事件
        floatView.findViewById(R.id.btn_save_settings).setOnClickListener(v -> {
            Log.d(TAG, "Save settings button clicked");
            saveSettings();
            hideSettingsPanel();
        });

        // 取消设置按钮点击事件
        floatView.findViewById(R.id.btn_cancel_settings).setOnClickListener(v -> {
            Log.d(TAG, "Cancel settings button clicked");
            hideSettingsPanel();
        });
    }

    /**
     * 切换弹出菜单的显示/隐藏状态
     */
    private void togglePopupMenu() {
        if (isPopupMenuShowing) {
            hidePopupMenu();
        } else {
            showPopupMenu();
        }
    }

    /**
     * 显示弹出菜单
     */
    private void showPopupMenu() {
        popupMenuView.setVisibility(View.VISIBLE);
        isPopupMenuShowing = true;
        // 更新窗口参数，允许获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Popup menu showed");
    }

    /**
     * 隐藏弹出菜单
     */
    private void hidePopupMenu() {
        popupMenuView.setVisibility(View.GONE);
        isPopupMenuShowing = false;
        // 恢复窗口参数，不获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Popup menu hidden");
    }

    /**
     * 显示设置面板
     */
    private void showSettingsPanel() {
        // 加载当前设置
        loadCurrentSettings();
        // 显示设置面板
        View settingsPanel = floatView.findViewById(R.id.ll_settings_panel);
        settingsPanel.setVisibility(View.VISIBLE);
        // 更新窗口参数，允许获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Settings panel showed");
    }

    /**
     * 隐藏设置面板
     */
    private void hideSettingsPanel() {
        View settingsPanel = floatView.findViewById(R.id.ll_settings_panel);
        settingsPanel.setVisibility(View.GONE);
        // 恢复窗口参数，不获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Settings panel hidden");
    }

    /**
     * 加载当前设置到UI组件
     */
    private void loadCurrentSettings() {
        // 从RuntimeConfig获取当前连接信息
        RuntimeConfig runtimeConfig = new RuntimeConfig(context);
        ConnectionInfo connectionInfo = runtimeConfig.getDefaultConnectionInfo();

        EditText etAddress = floatView.findViewById(R.id.et_address);
        EditText etPort = floatView.findViewById(R.id.et_port);
        CheckBox cbUseTls = floatView.findViewById(R.id.cb_use_tls);

        if (connectionInfo != null) {
            etAddress.setText(connectionInfo.getAddress());
            etPort.setText(String.valueOf(connectionInfo.getPort()));
            cbUseTls.setChecked(connectionInfo.isUseTls());
        } else {
            // 默认值
            etAddress.setText("localhost");
            etPort.setText("8080");
            cbUseTls.setChecked(false);
        }
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        EditText etAddress = floatView.findViewById(R.id.et_address);
        EditText etPort = floatView.findViewById(R.id.et_port);
        CheckBox cbUseTls = floatView.findViewById(R.id.cb_use_tls);

        String address = etAddress.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();
        boolean useTls = cbUseTls.isChecked();

        // 验证输入
        if (address.isEmpty()) {
            Log.w(TAG, "Address is empty, using default");
            address = "localhost";
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                Log.w(TAG, "Invalid port, using default");
                port = 8080;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid port format, using default");
            port = 8080;
        }

        // 保存到数据库
        RuntimeConfig runtimeConfig = new RuntimeConfig(context);
        ConnectionInfo connectionInfo = runtimeConfig.getDefaultConnectionInfo();

        if (connectionInfo != null) {
            // 更新现有连接信息
            connectionInfo.setAddress(address);
            connectionInfo.setPort(port);
            connectionInfo.setUseTls(useTls);
        } else {
            // 创建新连接信息
            connectionInfo = new ConnectionInfo(address, port);
            connectionInfo.setUseTls(useTls);
            connectionInfo.setDefault(true);
        }

        long id = runtimeConfig.saveConnectionInfo(connectionInfo);
        Log.d(TAG, "Settings saved with ID: " + id);
        
        // 发送连接信息更新广播
        Intent updateIntent = new Intent(RuntimeEvents.ACTION_CONNECTION_INFO_UPDATED);
        context.sendBroadcast(updateIntent);
    }

    /**
     * 显示浮窗
     */
    public void showFloatWindow() {
        if (!isShowing) {
            try {
                windowManager.addView(floatView, windowParams);
                isShowing = true;
                Log.d(TAG, "Float window showed");
            } catch (Exception e) {
                Log.e(TAG, "Failed to show float window: " + e.getMessage());
            }
        }
    }

    /**
     * 隐藏浮窗
     */
    public void hideFloatWindow() {
        if (isShowing) {
            try {
                windowManager.removeView(floatView);
                isShowing = false;
                Log.d(TAG, "Float window hidden");
            } catch (Exception e) {
                Log.e(TAG, "Failed to hide float window: " + e.getMessage());
            }
        }
    }

    /**
     * 更新浮窗状态文本
     */
    public void updateStatusText(String status) {
        // 新设计中状态文本不再直接显示在浮窗上
        // 可以根据需要添加状态指示逻辑
        Log.d(TAG, "Float window status updated: " + status);
    }

    /**
     * 初始化布局列表
     */
    private void initLayoutsList() {
        // 添加示例布局
        layoutsList.add("默认键盘布局");
        layoutsList.add("游戏手柄布局");
        layoutsList.add("陀螺仪触控组合");
        layoutsList.add("故障脚本测试");

        // 创建布局适配器
        layoutsAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_single_choice, layoutsList);
        layoutsListView.setAdapter(layoutsAdapter);
        layoutsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // 默认选择第一个布局
        if (!layoutsList.isEmpty()) {
            layoutsListView.setItemChecked(0, true);
        }
    }

    /**
     * 设置布局管理事件监听
     */
    private void setupLayoutManagementListeners() {
        // 布局启用开关
        layoutEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Layout enabled: " + isChecked);
            // 更新布局渲染器状态
            if (layoutRenderer != null) {
                layoutRenderer.setLayoutEnabled(isChecked);
            }
            // 发送布局启用状态广播
            Intent intent = new Intent("com.linecat.wmmtcontroller.ACTION_LAYOUT_ENABLED_CHANGED");
            intent.putExtra("enabled", isChecked);
            context.sendBroadcast(intent);
        });

        // 创建布局按钮
        floatView.findViewById(R.id.btn_create_layout).setOnClickListener(v -> {
            Log.d(TAG, "Create layout button clicked");
            Toast.makeText(context, "创建布局功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 编辑布局按钮
        floatView.findViewById(R.id.btn_edit_layout).setOnClickListener(v -> {
            Log.d(TAG, "Edit layout button clicked");
            int position = layoutsListView.getCheckedItemPosition();
            if (position != ListView.INVALID_POSITION) {
                String layoutName = layoutsList.get(position);
                Toast.makeText(context, "编辑布局: " + layoutName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "请先选择一个布局", Toast.LENGTH_SHORT).show();
            }
        });

        // 删除布局按钮
        floatView.findViewById(R.id.btn_delete_layout).setOnClickListener(v -> {
            Log.d(TAG, "Delete layout button clicked");
            int position = layoutsListView.getCheckedItemPosition();
            if (position != ListView.INVALID_POSITION) {
                String layoutName = layoutsList.get(position);
                layoutsList.remove(position);
                layoutsAdapter.notifyDataSetChanged();
                // 默认选择第一个布局
                if (!layoutsList.isEmpty()) {
                    layoutsListView.setItemChecked(0, true);
                }
                Toast.makeText(context, "已删除布局: " + layoutName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "请先选择一个布局", Toast.LENGTH_SHORT).show();
            }
        });

        // 返回按钮
        floatView.findViewById(R.id.btn_back_from_layout).setOnClickListener(v -> {
            Log.d(TAG, "Back from layout management button clicked");
            hideLayoutManagementPanel();
        });
    }

    /**
     * 显示布局管理面板
     */
    private void showLayoutManagementPanel() {
        layoutManagementPanelView.setVisibility(View.VISIBLE);
        isLayoutManagementPanelShowing = true;
        // 更新窗口参数，允许获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Layout management panel showed");
    }

    /**
     * 隐藏布局管理面板
     */
    private void hideLayoutManagementPanel() {
        layoutManagementPanelView.setVisibility(View.GONE);
        isLayoutManagementPanelShowing = false;
        // 恢复窗口参数，不获取焦点
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        windowManager.updateViewLayout(floatView, windowParams);
        Log.d(TAG, "Layout management panel hidden");
    }

    /**
     * 检查连接信息是否完整
     * @return true if connection info is complete, false otherwise
     */
    private boolean checkConnectionInfo() {
        // 从RuntimeConfig获取当前连接信息
        RuntimeConfig runtimeConfig = new RuntimeConfig(context);
        ConnectionInfo connectionInfo = runtimeConfig.getDefaultConnectionInfo();

        if (connectionInfo == null) {
            Log.w(TAG, "No connection info found");
            return false;
        }

        String address = connectionInfo.getAddress();
        int port = connectionInfo.getPort();

        // 检查地址是否为空或无效
        if (address == null || address.trim().isEmpty()) {
            Log.w(TAG, "Address is empty");
            return false;
        }

        // 检查端口是否在有效范围内
        if (port < 1 || port > 65535) {
            Log.w(TAG, "Port is invalid: " + port);
            return false;
        }

        return true;
    }

    /**
     * 显示连接错误提示
     */
    public void showConnectionError() {
        Toast.makeText(context, "连接失败，请检查服务器地址和端口", Toast.LENGTH_SHORT).show();
    }

    /**
     * 浮窗是否正在显示
     */
    public boolean isFloatWindowShowing() {
        return isShowing;
    }
    
    /**
     * 设置当前布局
     */
    public void setCurrentLayout(LayoutSnapshot layout) {
        if (layoutRenderer != null) {
            layoutRenderer.setLayout(layout);
        }
    }

    /**
     * 销毁浮窗
     */
    public void destroyFloatWindow() {
        hideFloatWindow();
        
        // 移除布局渲染容器
        if (layoutRenderContainer != null) {
            try {
                windowManager.removeView(layoutRenderContainer);
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove layout render container: " + e.getMessage());
            }
            layoutRenderContainer = null;
            layoutRenderer = null;
        }
        
        instance = null;
    }
}
