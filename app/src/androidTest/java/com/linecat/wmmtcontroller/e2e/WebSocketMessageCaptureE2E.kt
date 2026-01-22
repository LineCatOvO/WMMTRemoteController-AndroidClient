package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test to capture real WebSocket messages for protocol audit
 */
class WebSocketMessageCaptureE2E : TestEnv() {

    @Test
    fun testCaptureWebSocketMessages() {
        // Step 1: Capture and print WebSocket messages
        println("=== WEBSOCKET MESSAGES ===")
        println()

        // Capture 3 messages
        var previousFrameId: Long? = null
        for (i in 1..3) {
            val wsMessage = runtimeAwaiter.awaitNextFrame(15000)
            
            println("Message $i:")
            println(wsMessage)
            println()
            
            // Assert 1: Verify frame is valid JSON structure
            assert(JsonAssertions.assertFrameJsonValid(wsMessage)) {
                "WebSocket frame $i should be valid JSON structure"
            }
            
            // Assert 2: Verify frameId is monotonic
            val currentFrameId = JsonAssertions.extractFrameId(wsMessage)
            assert(previousFrameId == null || currentFrameId > previousFrameId) {
                "FrameId should be monotonic (previous: $previousFrameId, current: $currentFrameId)"
            }
            previousFrameId = currentFrameId
            
            // Assert 3: Verify runtimeStatus field exists
            val runtimeStatus = JsonAssertions.extractRuntimeStatus(wsMessage)
            assert(runtimeStatus != null) {
                "WebSocket frame $i should contain runtimeStatus field"
            }
        }

        println("=== END OF MESSAGES ===")
    }
}