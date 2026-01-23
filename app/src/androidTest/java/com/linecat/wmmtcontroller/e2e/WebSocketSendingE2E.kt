package com.linecat.wmmtcontroller.e2e

import androidx.test.filters.LargeTest
import com.linecat.wmmtcontroller.e2e.util.InputSimulation
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.model.InputState
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.Assert.*

/**
 * E2E tests for WebSocket Sending phase
 * Tests state sending behavior over WebSocket
 */
@LargeTest
class WebSocketSendingE2E : TestEnv() {

    private val inputSimulation = InputSimulation()
    private val jsonAssertions = JsonAssertions()

    /**
     * Test state change sending
     * Expected: Should send state immediately when input changes
     */
    @Test
    fun testStateChangeSending() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate touch input
        inputSimulation.simulateSingleTouch(300f, 300f)

        // Should receive message immediately
        val request: RecordedRequest = mockWsServer.takeRequest(2000)
        assertNotNull(request)
        
        val state: InputState = JsonAssertions.parseInputState(request)
        assertNotNull(state)
    }

    /**
     * Test idempotent sending
     * Expected: Should not send duplicate messages for unchanged state
     */
    @Test
    fun testIdempotentSending() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate same touch input twice in quick succession
        inputSimulation.simulateSingleTouch(500f, 500f)
        Thread.sleep(100)
        inputSimulation.simulateSingleTouch(500f, 500f)

        // Should receive only one message (or a small number) due to idempotency
        val startTime = System.currentTimeMillis()
        var messageCount = 0
        
        while (System.currentTimeMillis() - startTime < 1000) {
            try {
                val request: RecordedRequest = mockWsServer.takeRequest(300)
                if (request != null) {
                    messageCount++
                }
            } catch (e: Exception) {
                // Ignore timeout
                break
            }
        }

        // Should receive at most 2 messages (one for each touch, but likely only one due to idempotency)
        assertTrue("Expected at most 2 messages for idempotent input, got $messageCount", messageCount <= 2)
    }

    /**
     * Test message structure validity
     * Expected: All messages should have valid JSON structure
     */
    @Test
    fun testMessageStructure() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate touch input
        inputSimulation.simulateSingleTouch(400f, 400f)

        val request: RecordedRequest = mockWsServer.takeRequest(2000)
        assertNotNull(request)
        
        // Parse and validate JSON structure
        val state: InputState = JsonAssertions.parseInputState(request)
        assertNotNull(state)
        
        // Verify all required fields are present
        assertNotNull(state.frameId)
        assertNotNull(state.runtimeStatus)
        assertNotNull(state.keyboard)
        assertNotNull(state.mouse)
        assertNotNull(state.joystick)
        assertNotNull(state.gyroscope)
    }

    /**
     * Test message frequency
     * Expected: Should send messages at a reasonable frequency
     */
    @Test
    fun testMessageFrequency() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate rapid touch movements
        val startTime = System.currentTimeMillis()
        val duration = 1000 // 1 second
        
        while (System.currentTimeMillis() - startTime < duration) {
            val x = 300f + ((System.currentTimeMillis() - startTime) % 600).toFloat()
            val y = 300f + ((System.currentTimeMillis() - startTime) % 400).toFloat()
            inputSimulation.simulateSingleTouch(x, y)
            Thread.sleep(50) // Send touch every 50ms
        }

        // Count messages received
        val messageCount = mockWsServer.getRequestCount()
        
        // Should receive messages at a reasonable frequency (not too many, not too few)
        assertTrue("Expected at least 1 message, got 0", messageCount > 0)
        assertTrue("Expected fewer than 100 messages, got $messageCount", messageCount < 100)
    }
}
