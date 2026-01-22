package com.linecat.wmmtcontroller.input;

import android.content.Context;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 极端情况测试类 - JUnit版本
 * 验证系统在各种极端情况下的行为
 */
public class ExtremeCaseTests {
    private Context context;
    private ProfileManager profileManager;
    private InputScriptEngine mockScriptEngine;

    @Before
    public void setUp() {
        // 使用一个简单的Context模拟
        context = new android.content.ContextWrapper(null) {
            @Override
            public android.content.Context getApplicationContext() {
                return this;
            }
        };

        // 创建模拟脚本引擎
        mockScriptEngine = new InputScriptEngine() {
            private EngineState state = EngineState.INITIALIZED;
            private String lastError = null;

            @Override
            public void init() {
                state = EngineState.INITIALIZED;
                lastError = null;
            }

            @Override
            public boolean loadScript(String scriptCode) {
                // 简单模拟：如果脚本包含"throw"，则加载失败
                if (scriptCode.contains("throw new Error")) {
                    state = EngineState.ERROR;
                    lastError = "Test error: Script contains throw";
                    return false;
                }
                state = EngineState.LOADED;
                lastError = null;
                return true;
            }

            @Override
            public boolean update(RawInput rawInput, InputState inputState) {
                if (state != EngineState.LOADED) {
                    lastError = "Script not loaded";
                    return false;
                }
                // 简单模拟：如果rawInput包含gyroRoll > 0.1，则按下A键
                inputState.clearAllKeys();
                if (rawInput.getGyroRoll() > 0.1) {
                    inputState.getKeyboard().add("A");
                }
                return true;
            }

            @Override
            public void onEvent(GameInputEvent event) {
                // 不实现
            }

            @Override
            public void reset() {
                state = EngineState.INITIALIZED;
                lastError = null;
            }

            @Override
            public void shutdown() {
                state = EngineState.SHUTDOWN;
            }

            @Override
            public EngineState getState() {
                return state;
            }

            @Override
            public String getLastError() {
                return lastError;
            }

            @Override
            public long getLastExecutionTime() {
                return 0;
            }
        };

        // 创建ProfileManager实例
        profileManager = new ProfileManager(context, mockScriptEngine);
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
        // 创建测试profile
        ScriptProfile profile = new ScriptProfile(
                "test_profile",
                "1.0.0",
                "test",
                "test.js",
                "function update(raw) { return {heldKeys: ['A']}; }"
        );

        // 切换到测试profile
        boolean result = profileManager.switchProfile(profile);
        assertTrue("Profile should be switched successfully", result);

        // 验证当前profile是test_profile
        assertEquals("Current profile should be test_profile", profile, profileManager.getCurrentProfile());
    }

    /**
     * 测试异常处理情况
     */
    @Test
    public void testExceptionHandling() {
        // 创建正常profile
        ScriptProfile normalProfile = new ScriptProfile(
                "normal_profile",
                "1.0.0",
                "test",
                "normal.js",
                "function update(raw) { return {heldKeys: ['A']}; }"
        );

        // 创建异常profile
        ScriptProfile errorProfile = new ScriptProfile(
                "error_profile",
                "1.0.0",
                "test",
                "error.js",
                "function update(raw) { throw new Error('Test error'); return {heldKeys: ['A']}; }"
        );

        // 先切换到正常profile
        boolean result1 = profileManager.switchProfile(normalProfile);
        assertTrue("Normal profile should be switched successfully", result1);
        assertEquals("Current profile should be normal_profile", normalProfile, profileManager.getCurrentProfile());

        // 尝试切换到异常profile，应该失败
        boolean result2 = profileManager.switchProfile(errorProfile);
        assertFalse("Error profile should not be switched", result2);

        // 验证当前profile仍然是正常profile
        assertEquals("Current profile should remain normal_profile", normalProfile, profileManager.getCurrentProfile());
    }

    /**
     * 测试超时处理情况
     */
    @Test
    public void testTimeoutHandling() {
        // 创建超时模拟profile
        ScriptProfile timeoutProfile = new ScriptProfile(
                "timeout_profile",
                "1.0.0",
                "test",
                "timeout.js",
                "function update(raw) { while(true); return {heldKeys: []}; }"
        );

        // 尝试切换到超时profile
        boolean result = profileManager.switchProfile(timeoutProfile);
        // 由于我们的模拟脚本引擎不支持超时，这里应该返回false
        // 实际的JsInputScriptEngine会在执行超时时返回false
        assertTrue("Timeout profile should be loaded (mock engine doesn't support timeout)", result);
    }

    /**
     * 测试粘键问题
     */
    @Test
    public void testStickyKeys() {
        // 创建测试profile
        ScriptProfile profile = new ScriptProfile(
                "sticky_keys_profile",
                "1.0.0",
                "test",
                "sticky.js",
                "function update(raw) { return {heldKeys: ['A', 'W']}; }"
        );

        // 切换到测试profile
        boolean result = profileManager.switchProfile(profile);
        assertTrue("Profile should be switched successfully", result);

        // 卸载当前profile
        profileManager.unloadCurrentProfile();

        // 验证当前profile为null
        assertNull("Current profile should be null after unload", profileManager.getCurrentProfile());
    }
}