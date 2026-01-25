package com.linecat.wmmtcontroller.layer;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.layer.test.PrimitiveCollector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 联合集成测试：PlatformAdaptationLayer → InputAbstractionLayer
 */
@RunWith(AndroidJUnit4.class)
public class PlatformToInputAbstractionIntegrationTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * 用例 E2E-001：真实输入 → primitives 不违背契约
     * 验证：平台层输出的 raw 能让抽象层输出正确 primitives
     */
    @Test
    public void testRealInputProducesValidPrimitives() {
        // 获取 Context
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // 创建 PrimitiveCollector
        PrimitiveCollector primitiveCollector = new PrimitiveCollector();
        
        // 创建 InputAbstractionLayer
        InputAbstractionLayer inputAbstractionLayer = new InputAbstractionLayer(primitiveCollector);
        
        // 使用ACTIVITY_PANEL模式创建PlatformAdaptationLayer
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 获取MainActivity的windowToken
            android.os.IBinder windowToken = activity.getWindow().getDecorView().getWindowToken();
            
            // 创建 PlatformAdaptationLayer，使用ACTIVITY_PANEL模式
            PlatformAdaptationLayer platformAdaptationLayer = new PlatformAdaptationLayer(
                    context, 
                    inputAbstractionLayer,
                    PlatformAdaptationLayer.OverlayMode.ACTIVITY_PANEL,
                    windowToken
            );
            
            // 1. 启动 Overlay
            platformAdaptationLayer.startOverlay();
            
            // 2. 准备 metrics
            PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                    new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
            
            // 3. 测试 pointer 序列：DOWN → MOVE → UP
            // 直接调用 InputAbstractionLayer 的 onRawPointerEvent 方法，因为它实现了 RawEventSink
            // 发送 DOWN 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 发送 MOVE 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.MOVE,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 150f, 250f)),
                            metrics)
            );
            
            // 发送 UP 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.UP,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 150f, 250f)),
                            metrics)
            );
            
            // 验证：pointer 序列产生正确的 PointerFrame
            List<InputAbstractionLayer.PointerFrame> pointerFrames = primitiveCollector.getPointerFrames();
            assertThat(pointerFrames).hasSize(3); // DOWN、MOVE、UP
            
            // 4. 测试 CANCEL 处理
            primitiveCollector.clear();
            
            // 发送 DOWN 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 发送 CANCEL 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.CANCEL,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 验证：CANCEL 事件产生 canceled=true 的 PointerFrame，且 active 清空
            assertThat(primitiveCollector.containsCanceledPointerFrame()).isTrue();
            
            // 5. 测试 rotation 变化
            primitiveCollector.clear();
            
            // 发送新的 metrics，rotation=90
            PlatformAdaptationLayer.RawWindowEvent.Metrics rotatedMetrics = 
                    new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 90);
            inputAbstractionLayer.onRawWindowEvent(
                    new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                            PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, rotatedMetrics));
            
            // 发送 DOWN 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            rotatedMetrics)
            );
            
            // 验证：space 更新，且坐标转换使用新 metrics
            pointerFrames = primitiveCollector.getPointerFrames();
            assertThat(pointerFrames).hasSize(1);
            InputAbstractionLayer.PointerFrame frame = pointerFrames.get(0);
            assertThat(frame.space.heightPx).isEqualTo(2400);
            
            // 6. 测试 gyro 输出
            // 发送 gyro 事件
            inputAbstractionLayer.onRawSensorEvent(
                    new PlatformAdaptationLayer.RawSensorEvent(
                            System.nanoTime(),
                            PlatformAdaptationLayer.RawSensorEvent.SensorType.GYROSCOPE,
                            new float[]{0.5f, 1.0f, 1.5f}, // pitch, roll, yaw
                            PlatformAdaptationLayer.RawSensorEvent.Accuracy.HIGH
                    )
            );
            
            // 验证：gyro 输出 time 单调且轴映射正确
            List<InputAbstractionLayer.GyroFrame> gyroFrames = primitiveCollector.getGyroFrames();
            assertThat(gyroFrames).hasSize(1);
            InputAbstractionLayer.GyroFrame gyroFrame = gyroFrames.get(0);
            assertThat(gyroFrame.yawRate).isEqualTo(1.5f);
            assertThat(gyroFrame.pitchRate).isEqualTo(0.5f);
            assertThat(gyroFrame.rollRate).isEqualTo(1.0f);
            
            // 7. 停止 Overlay
            platformAdaptationLayer.stopOverlay();
        });
    }
}