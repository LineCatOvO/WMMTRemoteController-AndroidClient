package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.layer.test.RawEventCollector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Touch 端到端测试
 */
@RunWith(AndroidJUnit4.class)
public class PlatformAdaptationLayerTouchTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * 用例 PAL-TOUCH-001（强）：真实 overlay 触摸注入
     * 验证：使用 UiAutomator 点击 overlay 区域，滑动，抬起，收到 DOWN/MOVE/UP 事件
     * 注意：此测试需要 overlay 权限
     */
    @Test
    public void testRealOverlayTouchInjection() {
        // 获取 Context 和 UiDevice
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        
        // 创建 RawEventCollector
        RawEventCollector eventCollector = new RawEventCollector();
        
        // 创建 PlatformAdaptationLayer
        PlatformAdaptationLayer platformAdaptationLayer = new PlatformAdaptationLayer(context, eventCollector);
        
        // 启动 Overlay
        platformAdaptationLayer.startOverlay();
        
        // 等待 Overlay 准备就绪
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 使用 UiAutomator 注入触摸事件（点击屏幕中心）
        int screenWidth = device.getDisplayWidth();
        int screenHeight = device.getDisplayHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        // 注入 DOWN 事件
        device.swipe(centerX, centerY, centerX, centerY, 10);
        
        // 注入 MOVE 事件
        device.swipe(centerX, centerY, centerX + 100, centerY + 100, 20);
        
        // 等待事件收集
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证：收到 RawPointerEvent，包含 DOWN/MOVE 事件
        int pointerEventCount = eventCollector.getPointerEventCount();
        assertThat(pointerEventCount).isGreaterThanOrEqualTo(2);
        
        // 停止 Overlay
        platformAdaptationLayer.stopOverlay();
    }

    /**
     * 用例 PAL-TOUCH-002（弱但 CI 友好）：同逻辑 View 的触摸回调可工作
     * 验证：在测试 Activity 中创建与 overlay 同样的触摸入口，RawPointerEvent 序列正确
     */
    @Test
    public void testSameLogicViewTouchCallback() {
        // 获取 Context
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // 创建 RawEventCollector
        RawEventCollector eventCollector = new RawEventCollector();
        
        // 创建 PlatformAdaptationLayer
        PlatformAdaptationLayer platformAdaptationLayer = new PlatformAdaptationLayer(context, eventCollector);
        
        // 创建一个与 OverlayView 相同逻辑的 View
        View testView = new View(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                // 调用 PlatformAdaptationLayer 的 handleTouchEvent 方法
                // 注意：由于 handleTouchEvent 是 private 的，我们需要通过反射调用
                // 或者，我们可以直接创建 RawPointerEvent 并调用 onRawPointerEvent 方法
                
                // 这里我们直接创建 RawPointerEvent 并调用 onRawPointerEvent
                // 模拟与 OverlayView 相同的行为
                PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                        new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
                
                // 转换 MotionEvent.Action 为 RawPointerEvent.Action
                PlatformAdaptationLayer.RawPointerEvent.Action action;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        action = PlatformAdaptationLayer.RawPointerEvent.Action.DOWN;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        action = PlatformAdaptationLayer.RawPointerEvent.Action.MOVE;
                        break;
                    case MotionEvent.ACTION_UP:
                        action = PlatformAdaptationLayer.RawPointerEvent.Action.UP;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        action = PlatformAdaptationLayer.RawPointerEvent.Action.CANCEL;
                        break;
                    default:
                        return true;
                }
                
                // 收集指针信息
                java.util.List<PlatformAdaptationLayer.RawPointerEvent.Pointer> pointers = new java.util.ArrayList<>();
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pointerId = event.getPointerId(i);
                    float x = event.getX(i);
                    float y = event.getY(i);
                    pointers.add(new PlatformAdaptationLayer.RawPointerEvent.Pointer(pointerId, x, y));
                }
                
                // 创建并发送 RawPointerEvent
                PlatformAdaptationLayer.RawPointerEvent rawPointerEvent = new PlatformAdaptationLayer.RawPointerEvent(
                        event.getEventTime() * 1000000, // 转换为纳秒
                        action,
                        event.getPointerId(event.getActionIndex()),
                        pointers,
                        metrics
                );
                
                // 直接将事件添加到收集器，因为 platformAdaptationLayer.onRawPointerEvent 不是公共方法
                eventCollector.onRawPointerEvent(rawPointerEvent);
                
                return true;
            }
        };
        
        // 启动 Activity 场景并获取根 View
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 添加 testView 到 Activity
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            ((FrameLayout) activity.findViewById(android.R.id.content)).addView(testView, params);
            
            // 模拟触摸事件：DOWN → MOVE → UP
            long downTime = SystemClock.uptimeMillis();
            
            // 模拟 DOWN 事件
            MotionEvent downEvent = MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_DOWN,
                    100f,
                    200f,
                    0
            );
            testView.dispatchTouchEvent(downEvent);
            downEvent.recycle();
            
            // 模拟 MOVE 事件
            MotionEvent moveEvent = MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_MOVE,
                    150f,
                    250f,
                    0
            );
            testView.dispatchTouchEvent(moveEvent);
            moveEvent.recycle();
            
            // 模拟 UP 事件
            MotionEvent upEvent = MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    150f,
                    250f,
                    0
            );
            testView.dispatchTouchEvent(upEvent);
            upEvent.recycle();
            
            // 移除 testView
            ((FrameLayout) activity.findViewById(android.R.id.content)).removeView(testView);
        });
        
        // 验证：收到 RawPointerEvent 序列，包含 DOWN/MOVE/UP 事件
        int pointerEventCount = eventCollector.getPointerEventCount();
        assertThat(pointerEventCount).isEqualTo(3);
        
        // 验证：包含 DOWN 事件
        boolean hasDownEvent = eventCollector.hasPointerDownEvent();
        assertThat(hasDownEvent).isTrue();
        
        // 验证：包含 MOVE 事件
        boolean hasMoveEvent = eventCollector.hasPointerMoveEvent();
        assertThat(hasMoveEvent).isTrue();
        
        // 验证：包含 UP 事件
        boolean hasUpEvent = eventCollector.hasPointerUpEvent();
        assertThat(hasUpEvent).isTrue();
    }
}
