package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 脚本测试工具
 * 支持离线验证脚本映射正确性
 */
public class ScriptTestHarness {
    private static final String TAG = "ScriptTestHarness";
    
    private Context context;
    private Gson gson;
    private JsInputScriptEngine scriptEngine;
    
    /**
     * 测试用例类
     */
    public static class TestCase {
        private String name;
        private String profile;
        private List<RawInput> inputSequence;
        private List<ExpectedOutput> expectedOutputs;
        
        // getter and setter
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getProfile() {
            return profile;
        }
        
        public void setProfile(String profile) {
            this.profile = profile;
        }
        
        public List<RawInput> getInputSequence() {
            return inputSequence;
        }
        
        public void setInputSequence(List<RawInput> inputSequence) {
            this.inputSequence = inputSequence;
        }
        
        public List<ExpectedOutput> getExpectedOutputs() {
            return expectedOutputs;
        }
        
        public void setExpectedOutputs(List<ExpectedOutput> expectedOutputs) {
            this.expectedOutputs = expectedOutputs;
        }
    }
    
    /**
     * 期望输出类
     */
    public static class ExpectedOutput {
        private long frameId;
        private List<String> heldKeys;
        
        // getter and setter
        public long getFrameId() {
            return frameId;
        }
        
        public void setFrameId(long frameId) {
            this.frameId = frameId;
        }
        
        public List<String> getHeldKeys() {
            return heldKeys;
        }
        
        public void setHeldKeys(List<String> heldKeys) {
            this.heldKeys = heldKeys;
        }
    }
    
    /**
     * 测试结果类
     */
    public static class TestResult {
        public enum ErrorType {
            FRAME_MISMATCH,
            SCRIPT_ERROR,
            TIMEOUT,
            UNKNOWN
        }
        
        private String testName;
        private boolean passed;
        private int totalFrames;
        private int passedFrames;
        private List<FrameError> errors;
        
        /**
         * 帧错误详情类
         */
        public static class FrameError {
            private long frameId;
            private List<String> expectedKeys;
            private List<String> actualKeys;
            private ErrorType errorType;
            private String errorMessage;
            
            public FrameError(long frameId, List<String> expectedKeys, List<String> actualKeys, ErrorType errorType, String errorMessage) {
                this.frameId = frameId;
                this.expectedKeys = expectedKeys;
                this.actualKeys = actualKeys;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
            }
            
            public long getFrameId() {
                return frameId;
            }
            
            public List<String> getExpectedKeys() {
                return expectedKeys;
            }
            
            public List<String> getActualKeys() {
                return actualKeys;
            }
            
            public ErrorType getErrorType() {
                return errorType;
            }
            
            public String getErrorMessage() {
                return errorMessage;
            }
            
            @Override
            public String toString() {
                return String.format("Frame %d: %s - Expected %s, Got %s - %s", 
                        frameId, errorType.name(), expectedKeys, actualKeys, errorMessage);
            }
        }
        
        public TestResult(String testName) {
            this.testName = testName;
            this.passed = true;
            this.totalFrames = 0;
            this.passedFrames = 0;
            this.errors = new ArrayList<>();
        }
        
        // getter and setter
        public String getTestName() {
            return testName;
        }
        
        public void setTestName(String testName) {
            this.testName = testName;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public void setPassed(boolean passed) {
            this.passed = passed;
        }
        
        public int getTotalFrames() {
            return totalFrames;
        }
        
        public void setTotalFrames(int totalFrames) {
            this.totalFrames = totalFrames;
        }
        
        public int getPassedFrames() {
            return passedFrames;
        }
        
        public void incrementPassedFrames() {
            this.passedFrames++;
        }
        
        public List<FrameError> getErrors() {
            return errors;
        }
        
        public void addFrameError(long frameId, List<String> expectedKeys, List<String> actualKeys, ErrorType errorType, String errorMessage) {
            FrameError error = new FrameError(frameId, expectedKeys, actualKeys, errorType, errorMessage);
            this.errors.add(error);
            this.passed = false;
        }
        
        public void addScriptError(String errorMessage) {
            FrameError error = new FrameError(-1, new ArrayList<>(), new ArrayList<>(), ErrorType.SCRIPT_ERROR, errorMessage);
            this.errors.add(error);
            this.passed = false;
        }
        
        public void addTimeoutError(long frameId) {
            FrameError error = new FrameError(frameId, new ArrayList<>(), new ArrayList<>(), ErrorType.TIMEOUT, "Script execution timed out");
            this.errors.add(error);
            this.passed = false;
        }
        
        // 兼容旧API，返回字符串列表形式的错误
        public List<String> getErrorStrings() {
            List<String> errorStrings = new ArrayList<>();
            for (FrameError error : errors) {
                errorStrings.add(error.toString());
            }
            return errorStrings;
        }
    }
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public ScriptTestHarness(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.scriptEngine = new JsInputScriptEngine(context);
    }
    
    /**
     * 从文件加载测试用例
     * @param filePath 测试用例文件路径
     * @return 测试用例
     */
    public TestCase loadTestCaseFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return gson.fromJson(content.toString(), TestCase.class);
    }
    
    /**
     * 运行测试用例
     * @param testCase 测试用例
     * @param scriptCode 脚本代码
     * @return 测试结果
     */
    public TestResult runTestCase(TestCase testCase, String scriptCode) {
        TestResult result = new TestResult(testCase.getName());
        
        try {
            // 初始化脚本引擎
            scriptEngine.init();
            
            // 加载脚本
            boolean loadSuccess = scriptEngine.loadScript(scriptCode);
            if (!loadSuccess) {
                result.addScriptError("Failed to load script: " + scriptEngine.getLastError());
                return result;
            }
            
            // 运行测试序列
            List<RawInput> inputSequence = testCase.getInputSequence();
            List<ExpectedOutput> expectedOutputs = testCase.getExpectedOutputs();
            
            result.setTotalFrames(inputSequence.size());
            
            for (int i = 0; i < inputSequence.size(); i++) {
                RawInput rawInput = inputSequence.get(i);
                ExpectedOutput expectedOutput = expectedOutputs.get(i);
                
                // 执行脚本
                InputState inputState = new InputState();
                boolean updateSuccess = scriptEngine.update(rawInput, inputState);
                
                // 验证结果
                boolean framePassed = updateSuccess && validateOutput(inputState, expectedOutput);
                if (framePassed) {
                    result.incrementPassedFrames();
                } else {
                    String errorMessage = updateSuccess ? 
                            "Frame output mismatch" : "Script execution failed";
                    TestResult.ErrorType errorType = updateSuccess ? 
                            TestResult.ErrorType.FRAME_MISMATCH : TestResult.ErrorType.SCRIPT_ERROR;
                    
                    result.addFrameError(
                            expectedOutput.getFrameId(),
                            expectedOutput.getHeldKeys(),
                            new ArrayList<>(inputState.getKeyboard()),
                            errorType,
                            errorMessage
                    );
                }
            }
            
        } catch (Exception e) {
            result.addScriptError("Test failed with exception: " + e.getMessage());
            Log.e(TAG, "Test failed", e);
        } finally {
            // 关闭脚本引擎
            scriptEngine.shutdown();
        }
        
        return result;
    }
    
    /**
     * 验证输出结果
     * @param actual 实际输出
     * @param expected 期望输出
     * @return 是否匹配
     */
    private boolean validateOutput(InputState actual, ExpectedOutput expected) {
        // 比较heldKeys
        return new ArrayList<>(actual.getKeyboard()).equals(expected.getHeldKeys());
    }
    
    /**
     * 生成测试报告
     * @param results 测试结果列表
     * @return 测试报告
     */
    public String generateTestReport(List<TestResult> results) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== Script Test Report ===\n\n");
        
        int totalTests = results.size();
        int passedTests = 0;
        int totalFrames = 0;
        int passedFrames = 0;
        
        for (TestResult result : results) {
            report.append("Test: ").append(result.getTestName()).append("\n");
            report.append("Status: ").append(result.isPassed() ? "PASSED" : "FAILED").append("\n");
            report.append("Frames: ").append(result.getPassedFrames()).append("/").append(result.getTotalFrames()).append("\n");
            
            if (!result.isPassed()) {
                report.append("Errors:\n");
                for (TestResult.FrameError error : result.getErrors()) {
                    report.append("  - ").append(error.toString()).append("\n");
                }
            }
            
            report.append("\n");
            
            if (result.isPassed()) {
                passedTests++;
            }
            
            totalFrames += result.getTotalFrames();
            passedFrames += result.getPassedFrames();
        }
        
        report.append("=== Summary ===\n");
        report.append("Total Tests: ").append(totalTests).append("\n");
        report.append("Passed Tests: ").append(passedTests).append("\n");
        report.append("Total Frames: ").append(totalFrames).append("\n");
        report.append("Passed Frames: ").append(passedFrames).append("\n");
        report.append("Frame Pass Rate: ").append(String.format("%.2f%%", (double) passedFrames / totalFrames * 100)).append("\n");
        
        return report.toString();
    }
}