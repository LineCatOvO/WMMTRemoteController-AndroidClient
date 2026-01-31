package com.linecat.wmmtcontroller.e2e;

import androidx.test.filters.LargeTest;
import com.linecat.wmmtcontroller.e2e.util.InputSimulation;
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.model.InputState;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * E2E tests for ControlResultState Generation phase
 * Tests InputState generation, validation, and sequence behavior
 */
@LargeTest
public class ControlResultStateE2E extends TestEnv {

    private final InputSimulation inputSimulation = new InputSimulation();
    private final JsonAssertions jsonAssertions = new JsonAssertions();

    /**
     * Test sequence monotonicity
     * Expected: frameId should increase monotonically
     */
    @Test
    public void testSequenceMonotonicity() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Simulate multiple touch events to generate multiple states
        List<InputState> states = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            inputSimulation.simulateSingleTouch(100f + i * 50f, 100f + i * 50f);
            
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
            states.add(state);
        }

        // Verify frameId increases monotonically
        for (int i = 1; i < states.size(); i++) {
            long previousFrameId = states.get(i-1).getFrameId();
            long currentFrameId = states.get(i).getFrameId();
            
            assertTrue("FrameId should increase monotonically: previous=" + previousFrameId + ", current=" + currentFrameId, 
                currentFrameId > previousFrameId);
        }
    }

    /**
     * Test field validity
     * Expected: All required fields should be present and valid
     */
    @Test
    public void testFieldValidity() {
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
        inputSimulation.simulateSingleTouch(500f, 500f);
        
        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);
        
        InputState state = JsonAssertions.parseInputState(request);
        
        // Verify all required fields are present and valid
        assertNotNull(state.getFrameId());
        assertNotNull(state.getRuntimeStatus());
        assertNotNull(state.getKeyboard());
        assertNotNull(state.getMouse());
        assertNotNull(state.getJoystick());
        assertNotNull(state.getGyroscope());
        
        // Verify keyboard is a list
        assertTrue("Keyboard should be a list, got " + state.getKeyboard().getClass().getSimpleName(), 
            state.getKeyboard() instanceof List); 
        
        // Verify mouse has valid coordinates
        assertTrue("Mouse X should be between 0.0 and 1.0, got " + state.getMouse().getX(), 
            state.getMouse().getX() >= 0.0f && state.getMouse().getX() <= 1.0f);
        assertTrue("Mouse Y should be between 0.0 and 1.0, got " + state.getMouse().getY(), 
            state.getMouse().getY() >= 0.0f && state.getMouse().getY() <= 1.0f);
        
        // Verify joystick has valid values
        assertTrue("Joystick X should be between -1.0 and 1.0, got " + state.getJoystick().getX(), 
            state.getJoystick().getX() >= -1.0f && state.getJoystick().getX() <= 1.0f);
        assertTrue("Joystick Y should be between -1.0 and 1.0, got " + state.getJoystick().getY(), 
            state.getJoystick().getY() >= -1.0f && state.getJoystick().getY() <= 1.0f);
    }

    /**
     * Test zero state generation
     * Expected: Should generate valid zero state when no input
     */
    @Test
    public void testZeroStateGeneration() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            try {
                mockWsServer.takeRequest(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Wait a bit for any pending messages to be processed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Should get zero state when no input is active
        RecordedRequest request = null;
        try {
            request = mockWsServer.takeRequest(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (request != null) {
            InputState state = JsonAssertions.parseInputState(request);
            
            // Zero state should have empty keyboard
            assertTrue("Zero state should have empty keyboard, got " + state.getKeyboard(), 
                state.getKeyboard().isEmpty());
            
            // Mouse should be at default position
            assertTrue("Mouse should be at (0,0) in zero state, got (" + state.getMouse().getX() + ", " + state.getMouse().getY() + ")", 
                state.getMouse().getX() == 0.0f && state.getMouse().getY() == 0.0f);
            
            // Mouse buttons should be released
            assertFalse("Mouse left button should be released in zero state", state.getMouse().isLeft());
            assertFalse("Mouse right button should be released in zero state", state.getMouse().isRight());
            assertFalse("Mouse middle button should be released in zero state", state.getMouse().isMiddle());
        }
    }

    /**
     * Test runtime status
     * Expected: Runtime status should be "ok" under normal conditions
     */
    @Test
    public void testRuntimeStatus() {
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
        inputSimulation.simulateSingleTouch(500f, 500f);
        
        RecordedRequest request;
        try {
            request = mockWsServer.takeRequest(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for WebSocket message");
            return;
        }
        assertNotNull(request);
        
        InputState state = JsonAssertions.parseInputState(request);
        
        // Runtime status should be "ok" under normal conditions
        assertEquals("Runtime status should be 'ok', got '" + state.getRuntimeStatus() + "'", 
            "ok", state.getRuntimeStatus());
    }
}