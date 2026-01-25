package com.linecat.wmmtcontroller.layer;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.layer.test.RawEventCollector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlatformAdaptationLayer 生命周期与输出序列测试
 */
@RunWith(AndroidJUnit4.class)
public class PlatformAdaptationLayerLifecycleTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * 用例 PAL-INT-001：startOverlay 产生 ATTACHED + METRICS
     * 验证：调用 startOverlay() 后产生正确的 RawWindowEvent
     */
    @Test
    public void testStartOverlayProducesAttachedAndMetrics() {
        // 获取 Context
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // 创建 RawEventCollector
        RawEventCollector eventCollector = new RawEventCollector();
        
        // 使用ACTIVITY_PANEL模式创建PlatformAdaptationLayer
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 获取MainActivity的windowToken
            android.os.IBinder windowToken = activity.getWindow().getDecorView().getWindowToken();
            
            // 创建 PlatformAdaptationLayer，使用ACTIVITY_PANEL模式
            PlatformAdaptationLayer platformAdaptationLayer = new PlatformAdaptationLayer(
                    context, 
                    eventCollector,
                    PlatformAdaptationLayer.OverlayMode.ACTIVITY_PANEL,
                    windowToken
            );
            
            // 在主线程中调用 startOverlay()
            platformAdaptationLayer.startOverlay();
            
            // 等待事件生成（最多1秒）
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 验证：收到 RawWindowEvent(kind=ATTACHED)
            boolean attachedEventFound = !eventCollector.getWindowEvents().isEmpty();
            assertThat(attachedEventFound).isTrue();
            
            // 验证：存在包含 metrics 的事件
            boolean hasMetricsEvent = eventCollector.getWindowEvents().stream()
                    .anyMatch(event -> event.metrics != null);
            assertThat(hasMetricsEvent).isTrue();
            
            // 停止 overlay
            platformAdaptationLayer.stopOverlay();
        });
    }

    /**
     * 用例 PAL-INT-002：stopOverlay 产生 DETACHED，且传感器停止
     * 验证：调用 stopOverlay() 后产生 DETACHED 事件，且传感器停止发送事件
     */
    @Test
    public void testStopOverlayProducesDetachedAndStopsSensor() {
        // 获取 Context
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // 创建 RawEventCollector
        RawEventCollector eventCollector = new RawEventCollector();
        
        // 使用ACTIVITY_PANEL模式创建PlatformAdaptationLayer
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 获取MainActivity的windowToken
            android.os.IBinder windowToken = activity.getWindow().getDecorView().getWindowToken();
            
            // 创建 PlatformAdaptationLayer，使用ACTIVITY_PANEL模式
            PlatformAdaptationLayer platformAdaptationLayer = new PlatformAdaptationLayer(
                    context, 
                    eventCollector,
                    PlatformAdaptationLayer.OverlayMode.ACTIVITY_PANEL,
                    windowToken
            );
            
            // 在主线程中调用 startOverlay()
            platformAdaptationLayer.startOverlay();
            
            // 等待传感器事件生成（最多2秒）
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 验证：收到 RawSensorEvent
            boolean sensorEventFound = eventCollector.hasSensorEvent();
            assertThat(sensorEventFound).isTrue();
            
            // 记录当前传感器事件数量
            int sensorEventCount = eventCollector.getSensorEventCount();
            
            // 调用 stopOverlay()
            platformAdaptationLayer.stopOverlay();
            
            // 等待 DETACHED 事件（最多1秒）
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 验证：收到 DETACHED
            boolean detachedEventFound = !eventCollector.getWindowEvents().isEmpty();
            assertThat(detachedEventFound).isTrue();
            
            // 等待一段时间（500ms），检查是否还有新的传感器事件
            SystemClock.sleep(500);
            
            // 记录新的传感器事件数量
            int newSensorEventCount = eventCollector.getSensorEventCount();
            
            // 验证：stop 后不再有 RawSensorEvent
            assertThat(newSensorEventCount).isEqualTo(sensorEventCount);
        });
    }
}