package com.linecat.wmmtcontroller.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * 静态处理器测试用例
 * 验证 DeadzoneProcessor、RangeMapper、CurveProcessor 和 InvertProcessor 的功能正确性
 */
public class StaticProcessorTests {
    
    /**
     * 测试 DeadzoneProcessor 功能
     */
    @Test
    public void testDeadzoneProcessor() {
        // 测试死区内部值
        assertEquals(0f, DeadzoneProcessor.process(0.1f, 0.2f), 0.001f);
        assertEquals(0f, DeadzoneProcessor.process(-0.1f, 0.2f), 0.001f);
        
        // 测试死区边界值
        assertEquals(0f, DeadzoneProcessor.process(0.2f, 0.2f), 0.001f);
        assertEquals(0f, DeadzoneProcessor.process(-0.2f, 0.2f), 0.001f);
        
        // 测试死区外部值
        assertEquals(0.125f, DeadzoneProcessor.process(0.3f, 0.2f), 0.001f);
        assertEquals(-0.125f, DeadzoneProcessor.process(-0.3f, 0.2f), 0.001f);
        
        // 测试最大值
        assertEquals(1f, DeadzoneProcessor.process(1f, 0.2f), 0.001f);
        assertEquals(-1f, DeadzoneProcessor.process(-1f, 0.2f), 0.001f);
    }
    
    /**
     * 测试 RangeMapper 功能
     */
    @Test
    public void testRangeMapper() {
        // 测试范围映射
        assertEquals(50f, RangeMapper.map(0.5f, 0f, 1f, 0f, 100f), 0.001f);
        assertEquals(0f, RangeMapper.map(0f, 0f, 1f, 0f, 100f), 0.001f);
        assertEquals(100f, RangeMapper.map(1f, 0f, 1f, 0f, 100f), 0.001f);
        
        // 测试负范围映射
        assertEquals(-50f, RangeMapper.map(0.5f, 0f, 1f, -100f, 0f), 0.001f);
        
        // 测试裁剪功能
        assertEquals(10f, RangeMapper.clamp(15f, 0f, 10f), 0.001f);
        assertEquals(0f, RangeMapper.clamp(-5f, 0f, 10f), 0.001f);
        assertEquals(5f, RangeMapper.clamp(5f, 0f, 10f), 0.001f);
    }
    
    /**
     * 测试 CurveProcessor 功能
     */
    @Test
    public void testCurveProcessor() {
        // 测试线性曲线
        assertEquals(0.5f, CurveProcessor.applyCurve(0.5f, "linear", 1.0f), 0.001f);
        assertEquals(-0.5f, CurveProcessor.applyCurve(-0.5f, "linear", 1.0f), 0.001f);
        
        // 测试指数曲线
        assertEquals(0.25f, CurveProcessor.applyCurve(0.5f, "exponential", 2.0f), 0.001f);
        assertEquals(-0.25f, CurveProcessor.applyCurve(-0.5f, "exponential", 2.0f), 0.001f);
        
        // 测试正弦曲线
        assertEquals(0.707f, CurveProcessor.applyCurve(0.5f, "sine", 1.0f), 0.001f);
        assertEquals(-0.707f, CurveProcessor.applyCurve(-0.5f, "sine", 1.0f), 0.001f);
    }
    
    /**
     * 测试 InvertProcessor 功能
     */
    @Test
    public void testInvertProcessor() {
        // 测试不反转
        assertEquals(0.5f, InvertProcessor.invert(0.5f, false), 0.001f);
        assertEquals(-0.5f, InvertProcessor.invert(-0.5f, false), 0.001f);
        
        // 测试反转
        assertEquals(-0.5f, InvertProcessor.invert(0.5f, true), 0.001f);
        assertEquals(0.5f, InvertProcessor.invert(-0.5f, true), 0.001f);
    }
    
    /**
     * 测试处理器组合使用
     */
    @Test
    public void testProcessorCombination() {
        // 模拟完整的输入处理链：Deadzone → Curve → Invert → Range
        float input = 0.3f;
        float deadzone = 0.2f;
        float curveParam = 2.0f;
        boolean invert = true;
        float outputMin = 0f;
        float outputMax = 100f;
        
        float processed = DeadzoneProcessor.process(input, deadzone);
        processed = CurveProcessor.applyCurve(processed, "exponential", curveParam);
        processed = InvertProcessor.invert(processed, invert);
        processed = RangeMapper.map(processed, -1f, 1f, outputMin, outputMax);
        
        // 预期结果计算
        // 0.3 → 0.125 (deadzone) → 0.015625 (exponential) → -0.015625 (invert) → 49.21875 (range map)
        float expected = 49.21875f;
        assertEquals(expected, processed, 0.001f);
    }
}