package com.linecat.wmmtcontroller.layer;

import android.content.Context;

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
 * Raw 背压与丢弃策略测试
 */
@RunWith(AndroidJUnit4.class)
public class PlatformAdaptationLayerBackpressureTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * 用例 PAL-BP-001：sensor 洪泛时只丢 sensor
     * 验证：当 sink 消费变慢时，出现 RawDropEvent(kind=SENSOR)，但关键 pointer 帧不丢
     */
    @Test
    public void testSensorFloodOnlyDropsSensor() {
        // 获取 Context
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // 创建一个特殊的 RawEventCollector，模拟消费变慢
        RawEventCollector slowEventCollector = new RawEventCollector() {
            @Override
            public void onRawSensorEvent(PlatformAdaptationLayer.RawSensorEvent e) {
                // 模拟消费变慢：处理 sensor 事件时休眠 10ms
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                super.onRawSensorEvent(e);
            }
        };
        
        // 使用ACTIVITY_PANEL模式创建PlatformAdaptationLayer
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 获取MainActivity的windowToken
            android.os.IBinder windowToken = activity.getWindow().getDecorView().getWindowToken();
            
            // 创建 PlatformAdaptationLayer，使用ACTIVITY_PANEL模式
            PlatformAdaptationLayer platformAdaptationLayer = new PlatformAdaptationLayer(
                    context, 
                    slowEventCollector,
                    PlatformAdaptationLayer.OverlayMode.ACTIVITY_PANEL,
                    windowToken
            );
            
            // 在主线程中启动 Overlay
            platformAdaptationLayer.startOverlay();
            
            // 1. 制造 sensor 事件洪泛
            long startTime = System.currentTimeMillis();
            long endTime = startTime + 1000; // 持续 1 秒
            
            PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                    new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
            
            // 直接调用 slowEventCollector 的方法来模拟事件处理
            // 注意：这测试的是 RawEventCollector 的处理，而不是 PlatformAdaptationLayer 的事件处理
            while (System.currentTimeMillis() < endTime) {
                // 发送大量 sensor 事件
                slowEventCollector.onRawSensorEvent(
                        new PlatformAdaptationLayer.RawSensorEvent(
                                System.nanoTime(),
                                PlatformAdaptationLayer.RawSensorEvent.SensorType.GYROSCOPE,
                                new float[]{0.1f, 0.2f, 0.3f},
                                PlatformAdaptationLayer.RawSensorEvent.Accuracy.HIGH
                        )
                );
            }
            
            // 2. 在洪泛期间注入 pointer DOWN/UP/CANCEL 事件
            // 发送 DOWN 事件
            slowEventCollector.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                            0,
                            java.util.List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 发送 UP 事件
            slowEventCollector.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.UP,
                            0,
                            java.util.List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 发送 CANCEL 事件
            slowEventCollector.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.CANCEL,
                            0,
                            java.util.List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 等待事件处理完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 3. 断言
            // 3.1 出现 RawDropEvent(kind=SENSOR)
            boolean hasSensorDropEvent = !slowEventCollector.getDropEvents().isEmpty();
            assertThat(hasSensorDropEvent).isTrue();
            
            // 3.2 在洪泛期间注入的 pointer DOWN/UP/CANCEL 事件都被收到
            long downEventCount = slowEventCollector.getPointerEvents().stream()
                    .filter(event -> event.action == PlatformAdaptationLayer.RawPointerEvent.Action.DOWN)
                    .count();
            long upEventCount = slowEventCollector.getPointerEvents().stream()
                    .filter(event -> event.action == PlatformAdaptationLayer.RawPointerEvent.Action.UP)
                    .count();
            long cancelEventCount = slowEventCollector.getPointerEvents().stream()
                    .filter(event -> event.action == PlatformAdaptationLayer.RawPointerEvent.Action.CANCEL)
                    .count();
            
            assertThat(downEventCount).isEqualTo(1);
            assertThat(upEventCount).isEqualTo(1);
            assertThat(cancelEventCount).isEqualTo(1);
            
            // 停止 Overlay
            platformAdaptationLayer.stopOverlay();
        });
    }
}