package com.linecat.wmmtcontroller.layer;

import com.linecat.wmmtcontroller.layer.test.PrimitiveCollector;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InputAbstractionLayer Pointer 状态机测试
 */
public class InputAbstractionLayerPointerTest {
    private InputAbstractionLayer inputAbstractionLayer;
    private PrimitiveCollector primitiveCollector;

    @Before
    public void setUp() {
        primitiveCollector = new PrimitiveCollector();
        inputAbstractionLayer = new InputAbstractionLayer(primitiveCollector);
    }

    /**
     * 用例 IA-PTR-001：单指点击序列
     * 验证：DOWN → UP 序列产生正确的 PointerFrame
     */
    @Test
    public void testSinglePointerTapSequence() {
        // 准备 RawWindowEvent 模拟 metrics
        PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, metrics));

        // 测试1：只发送 DOWN 事件
        long downTime = System.nanoTime();
        List<PlatformAdaptationLayer.RawPointerEvent.Pointer> downPointers = List.of(
                new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)
        );
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(downTime,
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        0,
                        downPointers,
                        metrics)
        );

        // 检查 DOWN 事件的处理结果
        List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();
        assertThat(frames).hasSize(1);
        InputAbstractionLayer.PointerFrame downFrame = frames.get(0);
        assertThat(downFrame.pointersById).hasSize(1);
        // 立即检查 DOWN 帧的 phase，因为 PointerState 是可变的
        assertThat(downFrame.pointersById.get(0).phase).isEqualTo(InputAbstractionLayer.PointerState.Phase.DOWN);
        assertThat(downFrame.changedIds).containsExactly(0);
        assertThat(downFrame.canceled).isFalse();
        
        // 重置收集器，开始新的测试
        primitiveCollector.clear();
        
        // 测试2：只发送 UP 事件（模拟之前有 DOWN 的情况）
        long upTime = System.nanoTime();
        List<PlatformAdaptationLayer.RawPointerEvent.Pointer> upPointers = List.of(
                new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)
        );
        // 先发送一个 DOWN 事件建立状态
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(downTime,
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        0,
                        downPointers,
                        metrics)
        );
        // 然后发送 UP 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(upTime,
                        PlatformAdaptationLayer.RawPointerEvent.Action.UP,
                        0,
                        upPointers,
                        metrics)
        );

        // 检查 UP 事件的处理结果
        frames = primitiveCollector.getPointerFrames();
        // 应该有两个帧：DOWN 和 UP
        assertThat(frames).hasSize(2);
        
        // 检查第二帧是 UP 帧
        InputAbstractionLayer.PointerFrame upFrame = frames.get(1);
        assertThat(upFrame.timeNanos).isEqualTo(upTime);
        // 指针在 UP 帧中应该仍然存在，但 phase 是 UP
        assertThat(upFrame.pointersById).hasSize(1);
        assertThat(upFrame.pointersById.get(0).phase).isEqualTo(InputAbstractionLayer.PointerState.Phase.UP);
        assertThat(upFrame.changedIds).containsExactly(0);
        assertThat(upFrame.canceled).isFalse();
    }

    /**
     * 用例 IA-PTR-002：MOVE 合并不吞关键帧
     * 验证：MOVE 事件被合并，但 DOWN/UP 关键帧不被合并
     */
    @Test
    public void testMoveMergeDoesNotSwallowKeyFrames() {
        // 准备 RawWindowEvent 模拟 metrics
        PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, metrics));

        // 发送 DOWN 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                        metrics)
        );

        // 发送密集的 MOVE 事件（模拟快速滑动）
        long baseTime = System.nanoTime();
        int moveCount = 10;
        for (int i = 0; i < moveCount; i++) {
            inputAbstractionLayer.onRawPointerEvent(
                    new PlatformAdaptationLayer.RawPointerEvent(baseTime + i * 1000, // 1us 间隔
                            PlatformAdaptationLayer.RawPointerEvent.Action.MOVE,
                            0,
                            List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f + i, 200f + i)),
                            metrics)
            );
        }

        // 发送 UP 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.UP,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 200f, 300f)),
                        metrics)
        );

        // 获取输出的帧
        List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();

        // 验证：输出帧数少于输入 MOVE 数（合并生效）
        assertThat(frames.size()).isLessThan(moveCount + 2); // 2 是 DOWN 和 UP 帧
        
        // 验证：DOWN 和 UP 帧都存在
        // 验证 DOWN 帧结构
        InputAbstractionLayer.PointerFrame downFrame = frames.get(0);
        assertThat(downFrame.pointersById).hasSize(1);
        assertThat(downFrame.changedIds).containsExactly(0);
        assertThat(downFrame.canceled).isFalse();
        
        // 验证 UP 帧结构
        InputAbstractionLayer.PointerFrame upFrame = frames.get(frames.size() - 1);
        assertThat(upFrame.pointersById).hasSize(1);
        assertThat(upFrame.changedIds).containsExactly(0);
        assertThat(upFrame.canceled).isFalse();
        
        // 验证 DOWN 帧在 UP 帧之前
        assertThat(downFrame.timeNanos).isLessThanOrEqualTo(upFrame.timeNanos);
    }

    /**
     * 用例 IA-PTR-003：多指并发与 changedIds
     * 验证：多指操作时 pointersById 正确，changedIds 精确反映变化的指针
     */
    @Test
    public void testMultiTouchConcurrentAndChangedIds() {
        // 准备 RawWindowEvent 模拟 metrics
        PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, metrics));

        // 发送 id0 DOWN 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f)),
                        metrics)
        );

        // 发送 id1 DOWN 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.DOWN,
                        1,
                        List.of(
                                new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 100f, 200f),
                                new PlatformAdaptationLayer.RawPointerEvent.Pointer(1, 300f, 400f)
                        ),
                        metrics)
        );

        // 发送 MOVE 事件（id0 移动）
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.MOVE,
                        0,
                        List.of(
                                new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 150f, 250f),
                                new PlatformAdaptationLayer.RawPointerEvent.Pointer(1, 300f, 400f)
                        ),
                        metrics)
        );

        // 发送 id1 UP 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.UP,
                        1,
                        List.of(
                                new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 150f, 250f),
                                new PlatformAdaptationLayer.RawPointerEvent.Pointer(1, 300f, 400f)
                        ),
                        metrics)
        );

        // 获取输出的帧
        List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();

        // 验证：存在包含两指的帧
        assertThat(frames).anyMatch(frame -> frame.pointersById.size() == 2);
        
        // 验证：两指位置不串
        frames.stream()
                .filter(frame -> frame.pointersById.size() == 2)
                .forEach(frame -> {
                    assertThat(frame.pointersById.get(0)).isNotNull();
                    assertThat(frame.pointersById.get(1)).isNotNull();
                    assertThat(frame.pointersById.get(0).x).isNotEqualTo(frame.pointersById.get(1).x);
                    assertThat(frame.pointersById.get(0).y).isNotEqualTo(frame.pointersById.get(1).y);
                });
        
        // 验证：changedIds 包含变化的指针
        assertThat(frames).anyMatch(frame -> frame.changedIds.contains(0));
        assertThat(frames).anyMatch(frame -> frame.changedIds.contains(1));
    }

    /**
     * 用例 IA-PTR-004：CANCEL 语义冻结
     * 验证：CANCEL 事件产生 canceled=true 的 PointerFrame，且后续不再有该指针
     */
    @Test
    public void testCancelSemanticsFreeze() {
        // 准备 RawWindowEvent 模拟 metrics
        PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(1080, 2400, 480, 0);
        inputAbstractionLayer.onRawWindowEvent(
                new PlatformAdaptationLayer.RawWindowEvent(System.nanoTime(), 
                        PlatformAdaptationLayer.RawWindowEvent.Kind.METRICS_CHANGED, metrics));

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

        // 发送 CANCEL 事件
        inputAbstractionLayer.onRawPointerEvent(
                new PlatformAdaptationLayer.RawPointerEvent(System.nanoTime(),
                        PlatformAdaptationLayer.RawPointerEvent.Action.CANCEL,
                        0,
                        List.of(new PlatformAdaptationLayer.RawPointerEvent.Pointer(0, 150f, 250f)),
                        metrics)
        );

        // 获取输出的帧
        List<InputAbstractionLayer.PointerFrame> frames = primitiveCollector.getPointerFrames();

        // 验证：输出中存在 canceled=true 的 PointerFrame
        assertThat(frames).anyMatch(InputAbstractionLayer.PointerFrame::isCanceled);
        
        // 验证：canceled frame 后 active pointers 为空
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
    }
}