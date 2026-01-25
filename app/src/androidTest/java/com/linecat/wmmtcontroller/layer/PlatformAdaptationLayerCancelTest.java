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
 * CANCEL 端到端收敛测试
 */
@RunWith(AndroidJUnit4.class)
public class PlatformAdaptationLayerCancelTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * 用例 PAL-CANCEL-001：stopOverlay 必须导致 CANCEL 或等效终止
     * 验证：在未 UP 的情况下 stopOverlay 会产生 CANCEL 事件，且抽象层输出 canceled=true 的 PointerFrame
     */
    @Test
    public void testStopOverlayCausesCancelOrEquivalentTermination() {
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
            
            // 在主线程中启动 Overlay
            platformAdaptationLayer.startOverlay();
            
            // 2. 准备 metrics
            PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                    new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
            
            // 3. 模拟 DOWN 事件（相当于系统产生 DOWN）
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                            metrics)
            );
            
            // 验证：DOWN 事件产生了正确的 PointerFrame
            List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();
            assertThat(frames).hasSize(1);
            assertThat(frames.get(0).pointersById.get(0).phase).isEqualTo(InputAbstractionLayer.PointerState.Phase.DOWN);
            
            // 4. 在未 UP 的情况下 stopOverlay
            platformAdaptationLayer.stopOverlay();
            
            // 5. 验证：抽象层输出 `PointerFrame(canceled=true)` 且 active 清空
            frames = primitiveCollector.getPointerFrames();
            assertThat(frames).anyMatch(InputAbstractionLayer.PointerFrame::isCanceled);
            
            // 验证：canceled frame 后 active 清空
            boolean hasActivePointersAfterCancel = false;
            for (int i = 0; i < frames.size(); i++) {
                if (frames.get(i).isCanceled()) {
                    // 检查后续帧
                    for (int j = i + 1; j < frames.size(); j++) {
                        if (!frames.get(j).pointersById.isEmpty() && !frames.get(j).isCanceled()) {
                            hasActivePointersAfterCancel = true;
                            break;
                        }
                    }
                    break;
                }
            }
            assertThat(hasActivePointersAfterCancel).isFalse();
        });
    }
}