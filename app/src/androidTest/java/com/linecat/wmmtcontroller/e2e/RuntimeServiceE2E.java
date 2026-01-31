package com.linecat.wmmtcontroller.e2e;

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.service.RuntimeEvents;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test Case 2: Runtime Service E2E
 * 
 * 证明目标：G3
 * 入口路径：应用启动后销毁 Activity
 * 断言依据：WebSocket 消息
 */
public class RuntimeServiceE2E extends TestEnv {

    @Test
    public void testRuntimeContinuesWhenActivityDestroyed() {
        // Step 1: Wait for initial WebSocket frame
        String initialMessage = runtimeAwaiter.awaitNextFrame(15000);
        
        // Assert 1: Initial frame should be valid JSON structure
        assertTrue("Initial WebSocket frame should be valid JSON structure",
            JsonAssertions.assertFrameJsonValid(initialMessage));
        
        // Extract initial frameId for comparison
        long initialFrameId = JsonAssertions.extractFrameId(initialMessage);
        
        // Assert 2: Initial frame should have valid runtimeStatus
        assertNotNull("Initial WebSocket frame should contain runtimeStatus field",
            JsonAssertions.extractRuntimeStatus(initialMessage));

        // Step 4: Destroy the Activity
        activityScenarioRule.getScenario().close();

        // Step 5: Verify the service continues sending WebSocket frames
        String postDestroyMessage = runtimeAwaiter.awaitNextFrame(15000);
        
        // Assert 3: Post-destruction frame should be valid JSON structure
        assertTrue("WebSocket frame after Activity destruction should be valid JSON structure",
            JsonAssertions.assertFrameJsonValid(postDestroyMessage));
        
        // Assert 4: frameId should continue to increment after Activity destruction
        long postDestroyFrameId = JsonAssertions.extractFrameId(postDestroyMessage);
        assertTrue("FrameId should continue to increment after Activity destruction (initial: " + initialFrameId + ", post: " + postDestroyFrameId + ")",
            postDestroyFrameId > initialFrameId);
        
        // Assert 5: Post-destruction frame should have valid runtimeStatus
        assertNotNull("WebSocket frame after Activity destruction should contain runtimeStatus field",
            JsonAssertions.extractRuntimeStatus(postDestroyMessage));
    }
}