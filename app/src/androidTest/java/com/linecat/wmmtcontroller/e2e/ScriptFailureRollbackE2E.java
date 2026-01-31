package com.linecat.wmmtcontroller.e2e;

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.service.RuntimeEvents;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test Case 4: Script Failure Rollback E2E
 * 
 * 证明目标：G5
 * 入口路径：应用启动后使用故障脚本
 * 断言依据：runtime 事件 + WebSocket 消息
 */
public class ScriptFailureRollbackE2E extends TestEnv {

    @Test
    public void testScriptFailureAutoRollback() {
        // Step 1: Wait for initial WebSocket frames and verify frameId monotonicity
        String previousFrame = null;
        for (int i = 1; i <= 3; i++) {
            String wsMessage = runtimeAwaiter.awaitNextFrame(15000);
            
            // Assert frame is valid JSON
            assertTrue("WebSocket frame " + i + " is not valid JSON structure",
                JsonAssertions.assertFrameJsonValid(wsMessage));
            
            // Assert frameId is monotonically increasing
            assertTrue("FrameId is not monotonically increasing for frame " + i,
                JsonAssertions.assertFrameHasMonotonicFrameId(previousFrame, wsMessage));
            
            previousFrame = wsMessage;
        }

        // Step 3: Switch to the faulty script profile
        runtimeConfig.setProfileId("official-profiles/fault_script_throw");

        // Step 4: Restart the service to apply the faulty profile
        android.content.Intent intent = new android.content.Intent(context, com.linecat.wmmtcontroller.service.InputRuntimeService.class);
        context.stopService(intent);
        // Remove Thread.sleep - use events to wait for service restart
        
        // Step 5: Wait for runtime error event
        assertTrue("Failed to receive RUNTIME_ERROR event from faulty script",
            runtimeAwaiter.awaitEvent(RuntimeEvents.ACTION_RUNTIME_ERROR, 10000));

        // Step 6: Wait for profile rollback event
        assertTrue("Failed to receive PROFILE_ROLLBACK event after script error",
            runtimeAwaiter.awaitProfileRollback(5000));

        // Step 7: Wait for WebSocket frame after rollback
        String wsMessage = runtimeAwaiter.awaitNextFrame(5000);

        // Step 8: Verify the system continues sending WebSocket frames
        // Assert frame is valid JSON
        assertTrue("WebSocket frame after rollback is not valid JSON structure",
            JsonAssertions.assertFrameJsonValid(wsMessage));
        
        // Verify the message has expected structure
        assertTrue("Expected keyboard field in WebSocket message after rollback",
            wsMessage.contains("keyboard"));
        assertTrue("Expected mouse field in WebSocket message after rollback",
            wsMessage.contains("mouse"));
        assertTrue("Expected joystick field in WebSocket message after rollback",
            wsMessage.contains("joystick"));
        assertTrue("Expected gyroscope field in WebSocket message after rollback",
            wsMessage.contains("gyroscope"));

        // Step 9: Verify the system continues running
        for (int i = 1; i <= 3; i++) {
            runtimeAwaiter.awaitNextFrame(5000);
        }
    }
}