package com.linecat.wmmtcontroller.e2e;

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.service.RuntimeEvents;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test to capture real WebSocket messages for protocol audit
 */
public class WebSocketMessageCaptureE2E extends TestEnv {

    @Test
    public void testCaptureWebSocketMessages() {
        // Step 1: Capture and print WebSocket messages
        System.out.println("=== WEBSOCKET MESSAGES ===");
        System.out.println();

        // Capture 3 messages
        Long previousFrameId = null;
        for (int i = 1; i <= 3; i++) {
            String wsMessage = runtimeAwaiter.awaitNextFrame(15000);
            
            System.out.println("Message " + i + ":");
            System.out.println(wsMessage);
            System.out.println();
            
            // Assert 1: Verify frame is valid JSON structure
            assertTrue("WebSocket frame " + i + " should be valid JSON structure",
                JsonAssertions.assertFrameJsonValid(wsMessage));
            
            // Assert 2: Verify frameId is monotonic
            long currentFrameId = JsonAssertions.extractFrameId(wsMessage);
            assertTrue("FrameId should be monotonic (previous: " + previousFrameId + ", current: " + currentFrameId + ")",
                previousFrameId == null || currentFrameId > previousFrameId);
            previousFrameId = currentFrameId;
            
            // Assert 3: Verify runtimeStatus field exists
            String runtimeStatus = JsonAssertions.extractRuntimeStatus(wsMessage);
            assertNotNull("WebSocket frame " + i + " should contain runtimeStatus field",
                runtimeStatus);
        }

        System.out.println("=== END OF MESSAGES ===");
    }
}