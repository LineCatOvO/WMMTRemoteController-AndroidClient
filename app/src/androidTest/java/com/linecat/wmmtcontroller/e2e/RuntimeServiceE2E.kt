package com.linecat.wmmtcontroller.e2e

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
        // Step 1: Wait for runtime startup and WebSocket connection
        val startupEvents = listOf(
            RuntimeEvents.ACTION_RUNTIME_STARTED,
            RuntimeEvents.ACTION_PROFILE_LOADED,
            RuntimeEvents.ACTION_SCRIPT_ENGINE_READY,
            RuntimeEvents.ACTION_WS_CONNECTED
        )

        assert(runtimeAwaiter.awaitEventsInOrder(startupEvents, 5000)) {
            "Failed to receive all required startup events"
        }

        // Step 2: Wait for a WebSocket frame to be sent
        assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
            "Failed to receive WS_SENT_FRAME event"
        }

        // Step 3: Verify initial WebSocket communication
        val initialRequest = mockWsServer.takeRequest()
        val initialMessage = initialRequest.body.readUtf8()
        assert(initialMessage.isNotEmpty()) {
            "Initial WebSocket message should not be empty"
        }

        // Step 4: Destroy the Activity
        activityScenarioRule.scenario.close()

        // Step 5: Verify the service continues sending WebSocket frames
        assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
            "Failed to receive WS_SENT_FRAME event after Activity destruction"
        }

        val postDestroyRequest = mockWsServer.takeRequest()
        val postDestroyMessage = postDestroyRequest.body.readUtf8()
        assert(postDestroyMessage.isNotEmpty()) {
            "WebSocket message after Activity destruction should not be empty"
        }

        // Step 7: Verify the service maintains consistent communication
        assert(mockWsServer.getRequestCount() >= 2) {
            "Expected at least 2 WebSocket messages, but got ${mockWsServer.getRequestCount()}"
        }
    }
}
