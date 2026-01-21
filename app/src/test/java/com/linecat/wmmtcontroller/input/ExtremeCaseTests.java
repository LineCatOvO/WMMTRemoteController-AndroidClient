package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.content.res.AssetManager;
import com.linecat.wmmtcontroller.model.RawInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 极端情况测试类 - JUnit版本
 * 验证系统在各种极端情况下的行为
 */
public class ExtremeCaseTests {
    private Context context;
    private ScriptTestHarness testHarness;

    @Before
    public void setUp() {
        // 使用一个简单的Context模拟
        context = new android.content.ContextWrapper(null) {
            @Override
            public android.content.Context getApplicationContext() {
                return this;
            }
        };
        testHarness = new ScriptTestHarness(context);
    }

    @After
    public void tearDown() {
        // 清理资源
    }

    /**
     * 测试正常执行情况
     */
    @Test
    public void testNormalExecution() {
        // 创建测试用例
        ScriptTestHarness.TestCase testCase = new ScriptTestHarness.TestCase();
        testCase.setName("Normal Execution Test");

        // 创建输入序列
        List<RawInput> inputSequence = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RawInput rawInput = new RawInput();
            rawInput.setGyroRoll(i * 0.05f);
            inputSequence.add(rawInput);
        }
        testCase.setInputSequence(inputSequence);

        // 创建期望输出
        List<ScriptTestHarness.ExpectedOutput> expectedOutputs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ScriptTestHarness.ExpectedOutput expectedOutput = new ScriptTestHarness.ExpectedOutput();
            expectedOutput.setFrameId(i);

            List<String> heldKeys = new ArrayList<>();
            if (i * 0.05f > 0.1) {
                heldKeys.add("A");
            }
            expectedOutput.setHeldKeys(heldKeys);

            expectedOutputs.add(expectedOutput);
        }
        testCase.setExpectedOutputs(expectedOutputs);

        // 加载测试脚本
        String scriptCode = loadTestScript("test_extreme_cases.js");
        assertNotNull("Script code should not be null", scriptCode);

        // 运行测试
        ScriptTestHarness.TestResult result = testHarness.runTestCase(testCase, scriptCode);

        // 断言结果
        assertTrue("Normal execution test should pass", result.isPassed());
        assertEquals("All frames should pass", result.getTotalFrames(), result.getPassedFrames());
    }

    /**
     * 测试异常处理情况
     */
    @Test
    public void testExceptionHandling() {
        // 创建测试用例
        ScriptTestHarness.TestCase testCase = new ScriptTestHarness.TestCase();
        testCase.setName("Exception Handling Test");

        // 创建输入序列
        List<RawInput> inputSequence = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            RawInput rawInput = new RawInput();
            rawInput.setGyroRoll(i * 0.05f);
            inputSequence.add(rawInput);
        }
        testCase.setInputSequence(inputSequence);

        // 创建期望输出（异常情况下应该回退，所以期望空结果）
        List<ScriptTestHarness.ExpectedOutput> expectedOutputs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ScriptTestHarness.ExpectedOutput expectedOutput = new ScriptTestHarness.ExpectedOutput();
            expectedOutput.setFrameId(i);
            expectedOutput.setHeldKeys(new ArrayList<>()); // 异常情况下期望空结果
            expectedOutputs.add(expectedOutput);
        }
        testCase.setExpectedOutputs(expectedOutputs);

        // 加载测试脚本并修改为异常模式
        String scriptCode = loadTestScript("test_extreme_cases.js");
        scriptCode = scriptCode.replace("let testMode = 'normal';", "let testMode = 'error';");

        // 运行测试
        ScriptTestHarness.TestResult result = testHarness.runTestCase(testCase, scriptCode);

        // 断言结果 - 应该通过，因为异常被正确处理
        assertTrue("Exception handling test should pass", result.isPassed());
        assertEquals("All frames should pass", result.getTotalFrames(), result.getPassedFrames());
    }

    /**
     * 测试超时处理情况
     */
    @Test
    public void testTimeoutHandling() {
        // 创建测试用例
        ScriptTestHarness.TestCase testCase = new ScriptTestHarness.TestCase();
        testCase.setName("Timeout Handling Test");

        // 创建输入序列（只需要一帧，因为会超时）
        List<RawInput> inputSequence = new ArrayList<>();
        RawInput rawInput = new RawInput();
        rawInput.setGyroRoll(0.1f);
        inputSequence.add(rawInput);
        testCase.setInputSequence(inputSequence);

        // 创建期望输出（超时情况下应该回退，所以期望空结果）
        List<ScriptTestHarness.ExpectedOutput> expectedOutputs = new ArrayList<>();
        ScriptTestHarness.ExpectedOutput expectedOutput = new ScriptTestHarness.ExpectedOutput();
        expectedOutput.setFrameId(0);
        expectedOutput.setHeldKeys(new ArrayList<>()); // 超时情况下期望空结果
        expectedOutputs.add(expectedOutput);
        testCase.setExpectedOutputs(expectedOutputs);

        // 加载测试脚本并修改为超时模式
        String scriptCode = loadTestScript("test_extreme_cases.js");
        scriptCode = scriptCode.replace("let testMode = 'normal';", "let testMode = 'timeout';");

        // 运行测试
        ScriptTestHarness.TestResult result = testHarness.runTestCase(testCase, scriptCode);

        // 断言结果 - 应该通过，因为超时被正确处理
        assertTrue("Timeout handling test should pass", result.isPassed());
        assertEquals("All frames should pass", result.getTotalFrames(), result.getPassedFrames());
    }

    /**
     * 测试粘键问题
     */
    @Test
    public void testStickyKeys() {
        // 创建测试用例
        ScriptTestHarness.TestCase testCase = new ScriptTestHarness.TestCase();
        testCase.setName("Sticky Keys Test");

        // 创建输入序列：先按下按键，然后异常，然后恢复
        List<RawInput> inputSequence = new ArrayList<>();

        // 第一帧：正常按下按键
        RawInput rawInput1 = new RawInput();
        rawInput1.setGyroRoll(0.5f); // 应该触发按键A
        rawInput1.setButtonA(true); // 应该触发按键W
        inputSequence.add(rawInput1);

        // 第二帧：异常情况
        RawInput rawInput2 = new RawInput();
        rawInput2.setGyroRoll(0.0f);
        rawInput2.setButtonA(false);
        inputSequence.add(rawInput2);

        // 第三帧：恢复正常，所有按键应该释放
        RawInput rawInput3 = new RawInput();
        rawInput3.setGyroRoll(0.0f);
        rawInput3.setButtonA(false);
        inputSequence.add(rawInput3);

        testCase.setInputSequence(inputSequence);

        // 创建期望输出
        List<ScriptTestHarness.ExpectedOutput> expectedOutputs = new ArrayList<>();

        // 第一帧：期望按键A和W被按下
        ScriptTestHarness.ExpectedOutput expected1 = new ScriptTestHarness.ExpectedOutput();
        expected1.setFrameId(0);
        List<String> keys1 = new ArrayList<>();
        keys1.add("A");
        keys1.add("W");
        expected1.setHeldKeys(keys1);
        expectedOutputs.add(expected1);

        // 第二帧：异常情况，期望空结果
        ScriptTestHarness.ExpectedOutput expected2 = new ScriptTestHarness.ExpectedOutput();
        expected2.setFrameId(1);
        expected2.setHeldKeys(new ArrayList<>());
        expectedOutputs.add(expected2);

        // 第三帧：恢复正常，所有按键应该释放
        ScriptTestHarness.ExpectedOutput expected3 = new ScriptTestHarness.ExpectedOutput();
        expected3.setFrameId(2);
        expected3.setHeldKeys(new ArrayList<>());
        expectedOutputs.add(expected3);

        testCase.setExpectedOutputs(expectedOutputs);

        // 加载测试脚本
        String scriptCode = loadTestScript("test_extreme_cases.js");

        // 运行测试
        ScriptTestHarness.TestResult result = testHarness.runTestCase(testCase, scriptCode);

        // 断言结果
        assertTrue("Sticky keys test should pass", result.isPassed());
        assertEquals("All frames should pass", result.getTotalFrames(), result.getPassedFrames());
    }

    /**
     * 从assets加载测试脚本
     * @param scriptName 脚本名称
     * @return 脚本代码
     */
    private String loadTestScript(String scriptName) {
        AssetManager assetManager = context.getAssets();
        StringBuilder scriptCode = new StringBuilder();

        try (InputStream is = assetManager.open(scriptName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                scriptCode.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to load test script: " + scriptName);
        }

        return scriptCode.toString();
    }
}