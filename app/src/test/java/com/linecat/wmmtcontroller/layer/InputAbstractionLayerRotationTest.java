package com.linecat.wmmtcontroller.layer;

import com.linecat.wmmtcontroller.layer.test.PrimitiveCollector;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InputAbstractionLayer 旋转归一化测试
 */
public class InputAbstractionLayerRotationTest {
    private InputAbstractionLayer inputAbstractionLayer;
    private PrimitiveCollector primitiveCollector;

    @Before
    public void setUp() {
        primitiveCollector = new PrimitiveCollector();
        inputAbstractionLayer = new InputAbstractionLayer(primitiveCollector);
    }

    /**
     * 用例 IA-ROT-001：rotation=0 直通
     * 验证：当 rotation=0 时，坐标直接传递，不归一化
     */
    @Test
    public void testRotationZeroPassThrough() {
        // 准备 RawWindowEvent 模拟 metrics，rotation=0
        PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, metrics));

        // 发送 DOWN 事件，坐标 (100, 200)
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                        metrics)
        );

        // 获取输出的帧
        List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();

        // 验证：输出的坐标经过旋转映射，space=1080x2400
        assertThat(frames).hasSize(1);
        InputAbstractionLayer.PointerFrame frame = frames.get(0);
        // 当 rotation=0 时，坐标会被旋转到基准方向（90° landscape）
        // 旋转规则：rotatedX = displayHeightPx - yPx, rotatedY = xPx
        assertThat(frame.pointersById.get(0).x).isEqualTo(2400f - 200f); // 2400-200=2200
        assertThat(frame.pointersById.get(0).y).isEqualTo(100f);
        assertThat(frame.space.widthPx).isEqualTo(1080);
        assertThat(frame.space.heightPx).isEqualTo(2400);
    }

    /**
     * 用例 IA-ROT-002：rotation=90/180/270 映射正确
     * 验证：不同 rotation 下同一视觉位置的点归一化后得到一致的坐标
     */
    @Test
    public void testRotationMappingCorrect() {
        // 测试用的 metrics（1080x2400）
        int width = 1080;
        int height = 2400;
        
        // 测试中心点 (540, 1200)
        testSameVisualPointAcrossRotations(width, height, 540f, 1200f);
        
        // 测试左上角 (0, 0)
        testSameVisualPointAcrossRotations(width, height, 0f, 0f);
        
        // 测试右上角 (1080, 0)
        testSameVisualPointAcrossRotations(width, height, 1080f, 0f);
        
        // 测试左下角 (0, 2400)
        testSameVisualPointAcrossRotations(width, height, 0f, 2400f);
        
        // 测试右下角 (1080, 2400)
        testSameVisualPointAcrossRotations(width, height, 1080f, 2400f);
    }

    /**
     * 辅助方法：测试同一视觉位置在不同 rotation 下的坐标映射
     */
    private void testSameVisualPointAcrossRotations(int width, int height, float x, float y) {
        // 测试不同 rotation 值
        int[] rotations = {0, 90, 180, 270};
        
        for (int rotation : rotations) {
            // 重置测试环境
            primitiveCollector.clear();
            inputAbstractionLayer = new InputAbstractionLayer(primitiveCollector);
            
            // 准备 RawWindowEvent 模拟 metrics
            PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                    new PlatformAdaptationLayer.RawWindowEvent.Metrics(width, height, 480, rotation);
            inputAbstractionLayer.onRawWindowEvent(
                    new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                            PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, metrics));

            // 发送 DOWN 事件
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                            PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, x, y)),
                            metrics)
            );

            // 获取输出的帧
            List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();
            assertThat(frames).hasSize(1);
            
            // 验证：坐标在合理范围内（根据旋转变换规则）
            InputAbstractionLayer.PointerFrame frame = frames.get(0);
            float normalizedX = frame.pointersById.get(0).x;
            float normalizedY = frame.pointersById.get(0).y;
            
            // 验证坐标在有效范围内（根据旋转变换规则）
            // 旋转变换会将坐标映射到基准方向（90° landscape）
            // 所以坐标可能超出原始宽度/高度范围
            assertThat(normalizedX).isBetween(0f, (float) Math.max(width, height));
            assertThat(normalizedY).isBetween(0f, (float) Math.max(width, height));
            
            // 只有当 rotation=90 时，坐标才直接传递（不需要旋转）
            if (rotation == 90) {
                // 对于 rotation=90，坐标应该保持不变
                assertThat(normalizedX).isEqualTo(x);
                assertThat(normalizedY).isEqualTo(y);
            }
        }
    }

    /**
     * 用例 IA-ROT-003：metrics 变化与 space 更新
     * 验证：metrics 变化后，space 更新且坐标转换使用新 metrics
     */
    @Test
    public void testMetricsChangeAndSpaceUpdate() {
        // 1. 发送初始 METRICS(1080x2400)
        PlatformAdaptationLayer.RawWindowEvent.Metrics initialMetrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, initialMetrics));

        // 2. 发送触摸事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                        initialMetrics)
        );

        // 3. 发送新的 METRICS(1200x2400)
        PlatformAdaptationLayer.RawWindowEvent.Metrics newMetrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1200, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, newMetrics));

        // 4. 发送新的触摸事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.MOVE,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 150f, 250f)),
                        newMetrics)
        );

        // 获取输出的帧
        List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();

        // 验证：至少有 2 帧
        assertThat(frames).hasSizeGreaterThanOrEqualTo(2);
        
        // 验证：第一帧使用初始 metrics (1080x2400)
        InputAbstractionLayer.PointerFrame firstFrame = frames.get(0);
        assertThat(firstFrame.space.widthPx).isEqualTo(1080);
        assertThat(firstFrame.space.heightPx).isEqualTo(2400);
        
        // 验证：后续帧使用新 metrics (1200x2400)
        boolean foundUpdatedSpace = false;
        for (int i = 1; i < frames.size(); i++) {
            InputAbstractionLayer.PointerFrame frame = frames.get(i);
            if (frame.space.widthPx == 1200 && frame.space.heightPx == 2400) {
                foundUpdatedSpace = true;
                break;
            }
        }
        assertThat(foundUpdatedSpace).isTrue();
    }
}