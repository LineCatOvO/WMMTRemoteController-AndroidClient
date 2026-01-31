package com.linecat.wmmtcontroller.e2e;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.service.RuntimeEvents;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test Case 1: App Launch E2E
 * 
 * 证明目标：G1, G2
 * 入口路径：应用启动
 * 断言依据：runtime 事件 + WebSocket 消息
 */
public class AppLaunchE2E extends TestEnv {

    @Test
    public void testAppLaunchAndRuntimeStartup() {
        // Step 1: Wait for initial WebSocket frame
        String wsMessage = runtimeAwaiter.awaitNextFrame(15000);
        String previousFrame = wsMessage;

        // Step 3.1: Structure Assertion - Verify frame is valid JSON
        assertTrue("WebSocket message is not a valid InputFrame structure",
            JsonAssertions.assertFrameJsonValid(wsMessage));

        // Step 3.2: Semantic Assertion - Verify keyboard is empty initially
        assertTrue("Expected keyboard field to be empty in initial WebSocket message",
            JsonAssertions.assertHeldKeysEmpty(wsMessage));

        // Step 3.3: Semantic Assertion - Verify frameId is valid
        assertTrue("FrameId is not valid in initial WebSocket message",
            JsonAssertions.assertFrameHasMonotonicFrameId(null, wsMessage));

        // Step 3.4: Additional WebSocket frames should be sent and have increasing frameId
        for (int i = 2; i <= 3; i++) {
            String nextWsMessage = runtimeAwaiter.awaitNextFrame(10000);
            
            // Assert frame is valid
            assertTrue("WebSocket frame " + i + " is not valid JSON structure",
                JsonAssertions.assertFrameJsonValid(nextWsMessage));
            
            // Assert frameId is monotonically increasing
            assertTrue("FrameId is not monotonically increasing for frame " + i,
                JsonAssertions.assertFrameHasMonotonicFrameId(wsMessage, nextWsMessage));
            
            // Update previous frame for next iteration
            previousFrame = nextWsMessage;
        }
    }
}