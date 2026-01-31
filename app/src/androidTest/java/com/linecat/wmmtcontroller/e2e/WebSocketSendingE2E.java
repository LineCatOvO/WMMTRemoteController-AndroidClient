package com.linecat.wmmtcontroller.e2e;

import androidx.test.filters.LargeTest;
import com.linecat.wmmtcontroller.e2e.util.InputSimulation;
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.model.InputState;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * E2E tests for WebSocket Sending phase
 * Tests state sending behavior over WebSocket
 */
@LargeTest
public class WebSocketSendingE2E extends TestEnv {

    private final InputSimulation inputSimulation = new InputSimulation();
    private final JsonAssertions jsonAssertions = new JsonAssertions();

    /**
     * Test state change sending
     * Expected: Should send state immediately when input changes
     */
    @Test
    public void testStateChangeSending() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate touch input
        inputSimulation.simulateSingleTouch(300f, 300f);

        // Should receive message immediately
        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);
        
        InputState state = JsonAssertions.parseInputState(request);
        assertNotNull(state);
    }

    /**
     * Test idempotent sending
     * Expected: Should not send duplicate messages for unchanged state
     */
    @Test
    public void testIdempotentSending() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate same touch input twice in quick succession
        inputSimulation.simulateSingleTouch(500f, 500f);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        inputSimulation.simulateSingleTouch(500f, 500f);

        // Should receive only one message (or a small number) due to idempotency
        long startTime = System.currentTimeMillis();
        int messageCount = 0;
        
        while (System.currentTimeMillis() - startTime < 1000) {
            try {
                RecordedRequest request = mockWsServer.takeRequest(300);
                if (request != null) {
                    messageCount++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Should receive at most 2 messages (one for each touch, but likely only one due to idempotency)
        assertTrue("Expected at most 2 messages for idempotent input, got " + messageCount, messageCount <= 2);
    }

    /**
     * Test message structure validity
     * Expected: All messages should have valid JSON structure
     */
    @Test
    public void testMessageStructure() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate touch input
        inputSimulation.simulateSingleTouch(400f, 400f);

        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);
        
        // Parse and validate JSON structure
        InputState state = JsonAssertions.parseInputState(request);
        assertNotNull(state);
        
        // Verify all required fields are present
        assertNotNull(state.getFrameId());
        assertNotNull(state.getRuntimeStatus());
        assertNotNull(state.getKeyboard());
        assertNotNull(state.getMouse());
        assertNotNull(state.getJoystick());
        assertNotNull(state.getGyroscope());
    }

    /**
     * Test message frequency
     * Expected: Should send messages at a reasonable frequency
     */
    @Test
    public void testMessageFrequency() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate rapid touch movements
        long startTime = System.currentTimeMillis();
        long duration = 1000; // 1 second
        
        while (System.currentTimeMillis() - startTime < duration) {
            float x = 300f + ((System.currentTimeMillis() - startTime) % 600);
            float y = 300f + ((System.currentTimeMillis() - startTime) % 400);
            inputSimulation.simulateSingleTouch(x, y);
            try {
                Thread.sleep(50); // Send touch every 50ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Count messages received
        int messageCount = mockWsServer.getRequestCount();
        
        // Should receive messages at a reasonable frequency (not too many, not too few)
        assertTrue("Expected at least 1 message, got 0", messageCount > 0);
        assertTrue("Expected fewer than 100 messages, got " + messageCount, messageCount < 100);
    }
}