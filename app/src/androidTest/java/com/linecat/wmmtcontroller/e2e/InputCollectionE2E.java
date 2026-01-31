package com.linecat.wmmtcontroller.e2e;

import androidx.test.filters.LargeTest;
import com.linecat.wmmtcontroller.e2e.util.InputSimulation;
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.model.InputState;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/**
 * E2E tests for Input Collection phase
 * Tests various input types and their handling
 */
@LargeTest
public class InputCollectionE2E extends TestEnv {

    private final InputSimulation inputSimulation = new InputSimulation();
    private final JsonAssertions jsonAssertions = new JsonAssertions();

    /**
     * Test single touch input
     * Expected: Should generate corresponding InputState
     */
    @Test
    public void testSingleTouchInput() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate single touch input
        inputSimulation.simulateSingleTouch(500f, 500f);

        // Wait for WebSocket message
        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);

        // Parse and validate InputState
        InputState inputState = JsonAssertions.parseInputState(request);
        assertNotNull(inputState);
    }

    /**
     * Test multi-touch input
     * Expected: Should handle multiple touch points simultaneously
     */
    @Test
    public void testMultiTouchInput() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate multi-touch input with two points
        inputSimulation.simulateMultiTouch(
            Arrays.asList(
                Arrays.asList(300f, 300f),
                Arrays.asList(700f, 700f)
            )
        );

        // Wait for WebSocket messages
        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);

        // Parse and validate InputState
        InputState inputState = JsonAssertions.parseInputState(request);
        assertNotNull(inputState);
    }

    /**
     * Test touch sliding
     * Expected: Should generate continuous state changes
     */
    @Test
    public void testTouchSliding() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate touch sliding
        inputSimulation.simulateTouchSlide(200f, 200f, 800f, 800f, 300);

        // Wait for multiple WebSocket messages
        int messageCount = 0;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 2000) {
            try {
                RecordedRequest request = mockWsServer.takeRequest(500);
                if (request != null) {
                    messageCount++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Should receive multiple messages for sliding
        assertTrue("Expected multiple messages for sliding, got " + messageCount, messageCount > 1);
    }

    /**
     * Test input release behavior
     * Expected: Should handle input release correctly
     */
    @Test
    public void testInputRelease() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate touch down and up
        inputSimulation.simulateSingleTouch(500f, 500f);

        // Wait for WebSocket message
        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);
    }
}