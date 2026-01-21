package com.linecat.wmmtcontroller.input;

import android.content.Context;
import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ProfileManager 契约测试类
 * 测试Profile的切换语义，确保其符合规范
 */
public class ProfileManagerContractTests {
    private Context context;
    private ProfileManager profileManager;
    private InputScriptEngine mockEngine;
    private RawInput testRawInput;
    private InputState testInputState;

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
        mockEngine = new InputScriptEngine() {
            private EngineState state = EngineState.INITIALIZED;
            private String lastError = null;
            private boolean loadCalled = false;
            
            @Override
            public void init() {
                state = EngineState.INITIALIZED;
                lastError = null;
            }
            
            @Override
            public boolean loadScript(String scriptCode) {
                loadCalled = true;
                // 测试特定脚本是否会产生错误
                if (scriptCode.contains("throw new Error")) {
                    state = EngineState.ERROR;
                    lastError = "Test error";
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
                // 简单的脚本模拟：如果脚本包含"keyA"，则按下A键
                if (loadCalled && lastError == null) {
                    inputState.clearAllKeys();
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
        
        // 创建测试用的原始输入和输入状态
        testRawInput = new RawInput();
        testInputState = new InputState();
        
        // 创建ProfileManager实例，传入正确的构造函数参数
        profileManager = new ProfileManager(context, mockEngine);
    }

    @After
    public void tearDown() {
        // ProfileManager没有shutdown方法，使用unloadCurrentProfile代替
        profileManager.unloadCurrentProfile();
    }

    /**
     * 测试切换成功时的语义
     * 1. 新profile应该成功加载
     * 2. 切换应该返回true
     * 3. 当前profile应该被更新
     */
    @Test
    public void testSwitchProfileSuccess() {
        // 创建两个测试profile
        ScriptProfile profile1 = new ScriptProfile(
            "profile1",
            "1.0.0",
            "test",
            "test1.js",
            "function update(raw) { return {heldKeys: ['A']}; }"
        );
        
        ScriptProfile profile2 = new ScriptProfile(
            "profile2",
            "1.0.0",
            "test",
            "test2.js",
            "function update(raw) { return {heldKeys: ['B']}; }"
        );
        
        // 切换到第一个profile
        boolean result1 = profileManager.switchProfile(profile1);
        assertTrue("Profile1 should be switched successfully", result1);
        
        // 验证当前profile是profile1
        assertEquals("Current profile should be profile1", profile1, profileManager.getCurrentProfile());
        
        // 切换到第二个profile
        boolean result2 = profileManager.switchProfile(profile2);
        assertTrue("Profile2 should be switched successfully", result2);
        
        // 验证当前profile是profile2
        assertEquals("Current profile should be profile2", profile2, profileManager.getCurrentProfile());
    }

    /**
     * 测试切换失败时的回退语义
     * 1. 无效profile应该切换失败
     * 2. 应该保持当前profile不变
     */
    @Test
    public void testSwitchProfileFailure() {
        // 创建有效profile和无效profile
        ScriptProfile validProfile = new ScriptProfile(
            "valid",
            "1.0.0",
            "test",
            "valid.js",
            "function update(raw) { return {heldKeys: ['A']}; }"
        );
        
        ScriptProfile invalidProfile = new ScriptProfile(
            "invalid",
            "1.0.0",
            "test",
            "invalid.js",
            "invalid javascript code without update function"
        );
        
        // 先切换到有效profile
        boolean result1 = profileManager.switchProfile(validProfile);
        assertTrue("Valid profile should be switched successfully", result1);
        
        // 验证当前profile是有效profile
        assertEquals("Current profile should be valid profile", validProfile, profileManager.getCurrentProfile());
        
        // 尝试切换到无效profile，应该失败
        boolean result2 = profileManager.switchProfile(invalidProfile);
        assertFalse("Invalid profile should not be switched", result2);
        
        // 验证当前profile仍然是有效profile
        assertEquals("Current profile should remain valid profile", validProfile, profileManager.getCurrentProfile());
    }

    /**
     * 测试卸载profile时的语义
     * 1. 卸载profile应该释放所有按键
     * 2. 应该回到默认行为（无按键）
     */
    @Test
    public void testUnloadProfile() {
        // 创建测试profile
        ScriptProfile profile = new ScriptProfile(
            "test_profile",
            "1.0.0",
            "test",
            "test.js",
            "function update(raw) { return {heldKeys: ['A', 'B', 'C']}; }"
        );
        
        // 切换到profile
        boolean result = profileManager.switchProfile(profile);
        assertTrue("Profile should be switched successfully", result);
        
        // 验证当前profile是test_profile
        assertEquals("Current profile should be test_profile", profile, profileManager.getCurrentProfile());
        
        // 卸载profile
        profileManager.unloadCurrentProfile();
        
        // 验证当前profile为null
        assertNull("Current profile should be null after unload", profileManager.getCurrentProfile());
    }

    /**
     * 测试自动回滚机制
     * 1. 当脚本引擎处于错误状态时自动回滚
     * 2. 回滚应该符合rollbackProfile()的约定
     */
    @Test
    public void testAutoRollback() {
        // 创建两个测试profile，一个会产生错误
        ScriptProfile normalProfile = new ScriptProfile(
            "normal",
            "1.0.0",
            "test",
            "normal.js",
            "function update(raw) { return {heldKeys: ['A']}; }"
        );
        
        ScriptProfile errorProfile = new ScriptProfile(
            "error",
            "1.0.0",
            "test",
            "error.js",
            "function update(raw) { throw new Error('Test error'); return {heldKeys: ['A']}; }"
        );
        
        // 先切换到正常profile
        boolean result1 = profileManager.switchProfile(normalProfile);
        assertTrue("Normal profile should be switched successfully", result1);
        
        // 验证当前profile是normalProfile
        assertEquals("Current profile should be normalProfile", normalProfile, profileManager.getCurrentProfile());
        
        // 尝试切换到错误profile，应该失败
        boolean result2 = profileManager.switchProfile(errorProfile);
        assertFalse("Error profile should not be switched", result2);
        
        // 验证当前profile仍然是normalProfile
        assertEquals("Current profile should remain normalProfile", normalProfile, profileManager.getCurrentProfile());
        
        // 测试needRollback()方法
        assertTrue("Should need rollback after error", profileManager.needRollback());
        
        // 测试autoRollback()方法
        boolean rollbackResult = profileManager.autoRollback();
        assertTrue("Auto rollback should succeed", rollbackResult);
    }

    /**
     * 测试回滚语义
     * 1. 回滚应该回到上一个可用Profile
     * 2. 回滚失败时应该清空所有heldKeys
     */
    @Test
    public void testRollbackProfile() {
        // 创建两个测试profile
        ScriptProfile profile1 = new ScriptProfile(
            "profile1",
            "1.0.0",
            "test",
            "test1.js",
            "function update(raw) { return {heldKeys: ['A']}; }"
        );
        
        ScriptProfile profile2 = new ScriptProfile(
            "profile2",
            "1.0.0",
            "test",
            "test2.js",
            "function update(raw) { return {heldKeys: ['B']}; }"
        );
        
        // 切换到第一个profile
        boolean result1 = profileManager.switchProfile(profile1);
        assertTrue("Profile1 should be switched successfully", result1);
        
        // 切换到第二个profile
        boolean result2 = profileManager.switchProfile(profile2);
        assertTrue("Profile2 should be switched successfully", result2);
        
        // 回滚到上一个profile
        boolean rollbackResult = profileManager.rollbackProfile();
        assertTrue("Rollback should succeed", rollbackResult);
        
        // 验证当前profile是profile1
        assertEquals("Current profile should be profile1 after rollback", profile1, profileManager.getCurrentProfile());
        
        // 再次回滚，应该回到默认行为
        boolean rollbackResult2 = profileManager.rollbackProfile();
        assertTrue("Second rollback should succeed", rollbackResult2);
    }

    /**
     * 测试配置文件验证
     * 1. 有效的profile应该通过验证
     * 2. 无效的profile应该验证失败
     */
    @Test
    public void testProfileValidation() {
        // 创建有效profile
        ScriptProfile validProfile = new ScriptProfile(
            "valid",
            "1.0.0",
            "test",
            "valid.js",
            "function update(raw) { return {heldKeys: ['A']}; }"
        );
        
        // 验证有效profile
        boolean validResult = profileManager.validateProfile(validProfile);
        assertTrue("Valid profile should pass validation", validResult);
        
        // 创建缺少update函数的无效profile
        ScriptProfile invalidProfile1 = new ScriptProfile(
            "invalid1",
            "1.0.0",
            "test",
            "invalid1.js",
            "function missingUpdate(raw) { return {heldKeys: ['A']}; }"
        );
        
        // 验证无效profile1
        boolean invalidResult1 = profileManager.validateProfile(invalidProfile1);
        assertFalse("Profile missing update function should fail validation", invalidResult1);
        
        // 创建缺少必要字段的无效profile
        ScriptProfile invalidProfile2 = new ScriptProfile(
            "", // 空名称
            "1.0.0",
            "test",
            "invalid2.js",
            "function update(raw) { return {heldKeys: ['A']}; }"
        );
        
        // 验证无效profile2
        boolean invalidResult2 = profileManager.validateProfile(invalidProfile2);
        assertFalse("Profile with empty name should fail validation", invalidResult2);
    }
}
