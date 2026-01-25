package com.linecat.wmmtcontroller.layer;

import com.linecat.wmmtcontroller.layer.test.PrimitiveCollector;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InputAbstractionLayer Gyro 映射与精度测试
 */
public class InputAbstractionLayerGyroTest {
    private InputAbstractionLayer inputAbstractionLayer;
    private PrimitiveCollector primitiveCollector;

    @Before
    public void setUp() {
        primitiveCollector = new PrimitiveCollector();
        inputAbstractionLayer = new InputAbstractionLayer(primitiveCollector);
    }

    /**
     * 用例 IA-GYR-001：轴映射冻结
     * 验证：GyroFrame 的 yaw/pitch/roll 与冻结的映射一致
     * 映射规则：yawRate = values[2], pitchRate = values[0], rollRate = values[1]
     */
    @Test
    public void testGyroAxisMappingFreeze() {
        // 准备 RawSensorEvent
        float[] values = {0.5f, 1.0f, 1.5f}; // pitch=0.5, roll=1.0, yaw=1.5
        PlatformAdaptationLayer.RawSensorEvent sensorEvent = 
                new PlatformAdaptationLayer.RawSensorEvent(
                        System.nanoTime(),
                        PlatformAdaptationLayer.RawSensorEvent.SensorType.GYROSCOPE,
                        values,
                        PlatformAdaptationLayer.RawSensorEvent.Accuracy.HIGH
                );

        // 发送传感器事件
        inputAbstractionLayer.onRawSensorEvent(sensorEvent);

        // 获取输出的 GyroFrame
        List<InputAbstractionLayer.GyroFrame> gyroFrames = primitiveCollector.getGyroFrames();

        // 验证：GyroFrame 的 yaw/pitch/roll 与映射规则一致
        assertThat(gyroFrames).hasSize(1);
        InputAbstractionLayer.GyroFrame gyroFrame = gyroFrames.get(0);
        assertThat(gyroFrame.yawRate).isEqualTo(1.5f);
        assertThat(gyroFrame.pitchRate).isEqualTo(0.5f);
        assertThat(gyroFrame.rollRate).isEqualTo(1.0f);
        
        // 验证：符号方向固定（正负不被偷偷翻转）
        float[] negativeValues = {-0.5f, -1.0f, -1.5f}; // 负值测试
        sensorEvent = new PlatformAdaptationLayer.RawSensorEvent(
                System.nanoTime(),
                PlatformAdaptationLayer.RawSensorEvent.SensorType.GYROSCOPE,
                negativeValues,
                PlatformAdaptationLayer.RawSensorEvent.Accuracy.HIGH
        );
        inputAbstractionLayer.onRawSensorEvent(sensorEvent);
        
        gyroFrames = primitiveCollector.getGyroFrames();
        assertThat(gyroFrames).hasSize(2);
        gyroFrame = gyroFrames.get(1);
        assertThat(gyroFrame.yawRate).isEqualTo(-1.5f);
        assertThat(gyroFrame.pitchRate).isEqualTo(-0.5f);
        assertThat(gyroFrame.rollRate).isEqualTo(-1.0f);
    }

    /**
     * 用例 IA-GYR-002：accuracy 透传且不门控
     * 验证：GyroFrame 仍输出，只是 accuracy 字段变化
     */
    @Test
    public void testGyroAccuracyPassthrough() {
        // 测试不同精度值
        PlatformAdaptationLayer.RawSensorEvent.Accuracy[] accuracies = {
                PlatformAdaptationLayer.RawSensorEvent.Accuracy.UNRELIABLE,
                PlatformAdaptationLayer.RawSensorEvent.Accuracy.LOW,
                PlatformAdaptationLayer.RawSensorEvent.Accuracy.MEDIUM,
                PlatformAdaptationLayer.RawSensorEvent.Accuracy.HIGH
        };

        for (PlatformAdaptationLayer.RawSensorEvent.Accuracy accuracy : accuracies) {
            // 发送传感器事件
            PlatformAdaptationLayer.RawSensorEvent sensorEvent = 
                    new PlatformAdaptationLayer.RawSensorEvent(
                            System.nanoTime(),
                            PlatformAdaptationLayer.RawSensorEvent.SensorType.GYROSCOPE,
                            new float[]{0.1f, 0.2f, 0.3f},
                            accuracy
                    );
            inputAbstractionLayer.onRawSensorEvent(sensorEvent);
        }

        // 获取输出的 GyroFrame
        List<InputAbstractionLayer.GyroFrame> gyroFrames = primitiveCollector.getGyroFrames();

        // 验证：所有精度值都有对应的 GyroFrame 输出
        assertThat(gyroFrames).hasSize(accuracies.length);
        
        // 验证：accuracy 字段正确透传
        for (int i = 0; i < accuracies.length; i++) {
            InputAbstractionLayer.GyroFrame gyroFrame = gyroFrames.get(i);
            PlatformAdaptationLayer.RawSensorEvent.Accuracy expectedAccuracy = accuracies[i];
            InputAbstractionLayer.GyroFrame.Accuracy actualAccuracy = gyroFrame.accuracy;
            
            switch (expectedAccuracy) {
                case UNRELIABLE:
                    assertThat(actualAccuracy).isEqualTo(InputAbstractionLayer.GyroFrame.Accuracy.UNRELIABLE);
                    break;
                case LOW:
                    assertThat(actualAccuracy).isEqualTo(InputAbstractionLayer.GyroFrame.Accuracy.LOW);
                    break;
                case MEDIUM:
                    assertThat(actualAccuracy).isEqualTo(InputAbstractionLayer.GyroFrame.Accuracy.MEDIUM);
                    break;
                case HIGH:
                    assertThat(actualAccuracy).isEqualTo(InputAbstractionLayer.GyroFrame.Accuracy.HIGH);
                    break;
            }
        }
    }

    /**
     * 用例 IA-GYR-003：timeNanos 单调性不被破坏
     * 验证：输出 GyroFrame timeNanos 不倒退
     */
    @Test
    public void testGyroTimeNanosMonotonic() {
        // 发送严格递增 timeNanos 的 raw 事件
        long previousTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            // 递增时间戳
            long currentTime = previousTime + 1000000; // 增加 1ms
            
            PlatformAdaptationLayer.RawSensorEvent sensorEvent = 
                    new PlatformAdaptationLayer.RawSensorEvent(
                            currentTime,
                            PlatformAdaptationLayer.RawSensorEvent.SensorType.GYROSCOPE,
                            new float[]{0.1f * i, 0.2f * i, 0.3f * i},
                            PlatformAdaptationLayer.RawSensorEvent.Accuracy.HIGH
                    );
            inputAbstractionLayer.onRawSensorEvent(sensorEvent);
            
            previousTime = currentTime;
        }

        // 获取输出的 GyroFrame
        List<InputAbstractionLayer.GyroFrame> gyroFrames = primitiveCollector.getGyroFrames();

        // 验证：输出 GyroFrame 数量与输入一致
        assertThat(gyroFrames).hasSize(10);
        
        // 验证：timeNanos 单调递增
        long lastTimeNanos = 0;
        for (InputAbstractionLayer.GyroFrame gyroFrame : gyroFrames) {
            long currentTimeNanos = gyroFrame.timeNanos;
            assertThat(currentTimeNanos).isGreaterThan(lastTimeNanos);
            lastTimeNanos = currentTimeNanos;
        }
    }
}