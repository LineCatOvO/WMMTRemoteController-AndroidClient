package com.linecat.wmmtcontroller.e2e

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test Case 1: App Launch E2E
 * 
 * 证明目标：G1, G2
 * 入口路径：应用启动
 * 断言依据：runtime 事件 + WebSocket 消息
 */
class AppLaunchE2E : TestEnv() {

    @Test
    fun testAppLaunchAndRuntimeStartup() {
        // Step 1: Wait for runtime startup events
        val eventsToWaitFor = listOf(
            RuntimeEvents.ACTION_RUNTIME_STARTED,
            RuntimeEvents.ACTION_PROFILE_LOADED,
            RuntimeEvents.ACTION_SCRIPT_ENGINE_READY,
            RuntimeEvents.ACTION_WS_CONNECTED
        )

        // Wait for all events in order
        assert(runtimeAwaiter.awaitEventsInOrder(eventsToWaitFor, 5000)) {
            "Failed to receive all required startup events"
        }

        // Step 2: Wait for a WebSocket frame to be sent
        assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
            "Failed to receive WS_SENT_FRAME event"
        }

        // Step 3: Verify WebSocket message content
        val wsRequest = mockWsServer.takeRequest()
        val wsMessage = wsRequest.body.readUtf8()
        var previousFrame: String? = wsMessage

        // Step 3.1: Structure Assertion - Verify frame is valid JSON
        assert(JsonAssertions.assertFrameJsonValid(wsMessage)) {
            "WebSocket message is not a valid InputFrame structure"
        }

        // Step 3.2: Semantic Assertion - Verify keyboard is empty initially
        assert(JsonAssertions.assertHeldKeysEmpty(wsMessage)) {
            "Expected keyboard field to be empty in initial WebSocket message"
        }

        // Step 3.3: Semantic Assertion - Verify frameId is valid
        assert(JsonAssertions.assertFrameHasMonotonicFrameId(null, wsMessage)) {
            "FrameId is not valid in initial WebSocket message"
        }

        // Step 3.4: Additional WebSocket frames should be sent and have increasing frameId
        for (i in 2..3) {
            assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
                "Failed to receive WS_SENT_FRAME event $i"
            }
            val nextWsRequest = mockWsServer.takeRequest()
            val nextWsMessage = nextWsRequest.body.readUtf8()
            
            // Assert frame is valid
            assert(JsonAssertions.assertFrameJsonValid(nextWsMessage)) {
                "WebSocket frame $i is not valid JSON structure"
            }
            
            // Assert frameId is monotonically increasing
            assert(JsonAssertions.assertFrameHasMonotonicFrameId(wsMessage, nextWsMessage)) {
                "FrameId is not monotonically increasing for frame $i"
            }
            
            // Update previous frame for next iteration
            previousFrame = nextWsMessage
        }
    }
}
