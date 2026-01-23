package com.linecat.wmmtcontroller.e2e

import androidx.test.filters.LargeTest
import com.linecat.wmmtcontroller.e2e.util.InputSimulation
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.model.InputState
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.Assert.*

/**
 * E2E tests for ControlResultState Generation phase
 * Tests InputState generation, validation, and sequence behavior
 */
@LargeTest
class ControlResultStateE2E : TestEnv() {

    private val inputSimulation = InputSimulation()
    private val jsonAssertions = JsonAssertions()

    /**
     * Test sequence monotonicity
     * Expected: frameId should increase monotonically
     */
    @Test
    fun testSequenceMonotonicity() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate multiple touch events to generate multiple states
        val states = mutableListOf<InputState>()
        
        for (i in 0 until 5) {
            inputSimulation.simulateSingleTouch(100f + i * 50f, 100f + i * 50f)
            
            val request: RecordedRequest = mockWsServer.takeRequest(2000)
        assertNotNull(request)
        
        val state: InputState = JsonAssertions.parseInputState(request)
            states.add(state)
        }

        // Verify frameId increases monotonically
        for (i in 1 until states.size) {
            val previousFrameId = states[i-1].frameId
            val currentFrameId = states[i].frameId
            
            assertTrue("FrameId should increase monotonically: previous=$previousFrameId, current=$currentFrameId", 
                currentFrameId > previousFrameId)
        }
    }

    /**
     * Test field validity
     * Expected: All required fields should be present and valid
     */
    @Test
    fun testFieldValidity() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate touch input
        inputSimulation.simulateSingleTouch(500f, 500f)
        
        val request: RecordedRequest = mockWsServer.takeRequest(5000)
        assertNotNull(request)
        
        val state: InputState = JsonAssertions.parseInputState(request)
        
        // Verify all required fields are present and valid
        assertNotNull(state.frameId)
        assertNotNull(state.runtimeStatus)
        assertNotNull(state.keyboard)
        assertNotNull(state.mouse)
        assertNotNull(state.joystick)
        assertNotNull(state.gyroscope)
        
        // Verify keyboard is a list
        assertTrue("Keyboard should be a list, got ${state.keyboard.javaClass.simpleName}", 
            state.keyboard is List<*>) 
        
        // Verify mouse has valid coordinates
        assertTrue("Mouse X should be between 0.0 and 1.0, got ${state.mouse.x}", 
            state.mouse.x >= 0.0f && state.mouse.x <= 1.0f)
        assertTrue("Mouse Y should be between 0.0 and 1.0, got ${state.mouse.y}", 
            state.mouse.y >= 0.0f && state.mouse.y <= 1.0f)
        
        // Verify joystick has valid values
        assertTrue("Joystick X should be between -1.0 and 1.0, got ${state.joystick.x}", 
            state.joystick.x >= -1.0f && state.joystick.x <= 1.0f)
        assertTrue("Joystick Y should be between -1.0 and 1.0, got ${state.joystick.y}", 
            state.joystick.y >= -1.0f && state.joystick.y <= 1.0f)
    }

    /**
     * Test zero state generation
     * Expected: Should generate valid zero state when no input
     */
    @Test
    fun testZeroStateGeneration() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Wait a bit for any pending messages to be processed
        Thread.sleep(1000)
        
        // Should get zero state when no input is active
        val request: RecordedRequest? = try {
            mockWsServer.takeRequest(2000)
        } catch (e: Exception) {
            null
        }
        
        if (request != null) {
            val state: InputState = JsonAssertions.parseInputState(request)
            
            // Zero state should have empty keyboard
            assertTrue("Zero state should have empty keyboard, got ${state.keyboard}", 
                state.keyboard.isEmpty())
            
            // Mouse should be at default position
            assertTrue("Mouse should be at (0,0) in zero state, got (${state.mouse.x}, ${state.mouse.y})", 
                state.mouse.x == 0.0f && state.mouse.y == 0.0f)
            
            // Mouse buttons should be released
            assertFalse("Mouse left button should be released in zero state", state.mouse.isLeft())
            assertFalse("Mouse right button should be released in zero state", state.mouse.isRight())
            assertFalse("Mouse middle button should be released in zero state", state.mouse.isMiddle())
        }
    }

    /**
     * Test runtime status
     * Expected: Runtime status should be "ok" under normal conditions
     */
    @Test
    fun testRuntimeStatus() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate touch input
        inputSimulation.simulateSingleTouch(500f, 500f)
        
        val request: RecordedRequest = mockWsServer.takeRequest(5000)
        assertNotNull(request)
        
        val state: InputState = JsonAssertions.parseInputState(request)
        
        // Runtime status should be "ok" under normal conditions
        assertEquals("Runtime status should be 'ok', got '${state.runtimeStatus}'", 
            "ok", state.runtimeStatus)
    }
}
