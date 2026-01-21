package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test to capture real WebSocket messages for protocol audit
 */
class WebSocketMessageCaptureE2E : TestEnv() {

    @Test
    fun testCaptureWebSocketMessages() {
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

        // Step 2: Capture and print real WebSocket messages
        println("=== REAL WEBSOCKET MESSAGES ===")
        println()

        // Capture 3 messages
        for (i in 1..3) {
            assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
                "Failed to receive WS_SENT_FRAME event $i"
            }
            
            val wsRequest = mockWsServer.takeRequest()
            val wsMessage = wsRequest.body.readUtf8()
            
            println("Message $i:")
            println(wsMessage)
            println()
        }

        println("=== END OF MESSAGES ===")
    }
}