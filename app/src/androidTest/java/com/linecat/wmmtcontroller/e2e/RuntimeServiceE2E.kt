package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test Case 2: Runtime Service E2E
 * 
 * 证明目标：G3
 * 入口路径：应用启动后销毁 Activity
 * 断言依据：WebSocket 消息
 */
class RuntimeServiceE2E : TestEnv() {

    @Test
    fun testRuntimeContinuesWhenActivityDestroyed() {
        // Step 1: Wait for initial WebSocket frame
        val initialMessage = runtimeAwaiter.awaitNextFrame(15000)
        
        // Assert 1: Initial frame should be valid JSON structure
        assert(JsonAssertions.assertFrameJsonValid(initialMessage)) {
            "Initial WebSocket frame should be valid JSON structure"
        }
        
        // Extract initial frameId for comparison
        val initialFrameId = JsonAssertions.extractFrameId(initialMessage)
        
        // Assert 2: Initial frame should have valid runtimeStatus
        assert(JsonAssertions.extractRuntimeStatus(initialMessage) != null) {
            "Initial WebSocket frame should contain runtimeStatus field"
        }

        // Step 4: Destroy the Activity
        activityScenarioRule.scenario.close()

        // Step 5: Verify the service continues sending WebSocket frames
        val postDestroyMessage = runtimeAwaiter.awaitNextFrame(15000)
        
        // Assert 3: Post-destruction frame should be valid JSON structure
        assert(JsonAssertions.assertFrameJsonValid(postDestroyMessage)) {
            "WebSocket frame after Activity destruction should be valid JSON structure"
        }
        
        // Assert 4: frameId should continue to increment after Activity destruction
        val postDestroyFrameId = JsonAssertions.extractFrameId(postDestroyMessage)
        assert(postDestroyFrameId > initialFrameId) {
            "FrameId should continue to increment after Activity destruction (initial: $initialFrameId, post: $postDestroyFrameId)"
        }
        
        // Assert 5: Post-destruction frame should have valid runtimeStatus
        assert(JsonAssertions.extractRuntimeStatus(postDestroyMessage) != null) {
            "WebSocket frame after Activity destruction should contain runtimeStatus field"
        }
    }
}
