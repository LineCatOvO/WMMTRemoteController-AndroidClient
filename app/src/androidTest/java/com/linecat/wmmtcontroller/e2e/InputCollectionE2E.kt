package com.linecat.wmmtcontroller.e2e

import androidx.test.filters.LargeTest
import com.linecat.wmmtcontroller.e2e.util.InputSimulation
import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.model.InputState
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.Assert.*

/**
 * E2E tests for Input Collection phase
 * Tests various input types and their handling
 */
@LargeTest
class InputCollectionE2E : TestEnv() {

    private val inputSimulation = InputSimulation()
    private val jsonAssertions = JsonAssertions()

    /**
     * Test single touch input
     * Expected: Should generate corresponding InputState
     */
    @Test
    fun testSingleTouchInput() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate single touch input
        inputSimulation.simulateSingleTouch(500f, 500f)

        // Wait for WebSocket message
        val request: RecordedRequest = mockWsServer.takeRequest(5000)
        assertNotNull(request)

        // Parse and validate InputState
        val inputState: InputState = JsonAssertions.parseInputState(request)
        assertNotNull(inputState)
    }

    /**
     * Test multi-touch input
     * Expected: Should handle multiple touch points simultaneously
     */
    @Test
    fun testMultiTouchInput() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate multi-touch input with two points
        inputSimulation.simulateMultiTouch(
            listOf(
                Pair(300f, 300f),
                Pair(700f, 700f)
            )
        )

        // Wait for WebSocket messages
        val request: RecordedRequest = mockWsServer.takeRequest(5000)
        assertNotNull(request)

        // Parse and validate InputState
        val inputState: InputState = JsonAssertions.parseInputState(request)
        assertNotNull(inputState)
    }

    /**
     * Test touch sliding
     * Expected: Should generate continuous state changes
     */
    @Test
    fun testTouchSliding() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate touch sliding
        inputSimulation.simulateTouchSlide(200f, 200f, 800f, 800f, 300)

        // Wait for multiple WebSocket messages
        var messageCount = 0
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 2000) {
            try {
                val request: RecordedRequest = mockWsServer.takeRequest(500)
                if (request != null) {
                    messageCount++
                }
            } catch (e: Exception) {
                // Ignore timeout, just exit loop
                break
            }
        }

        // Should receive multiple messages for sliding
        assertTrue("Expected multiple messages for sliding, got $messageCount", messageCount > 1)
    }

    /**
     * Test input release behavior
     * Expected: Should handle input release correctly
     */
    @Test
    fun testInputRelease() {
        // Clear any existing messages
        while (mockWsServer.getRequestCount() > 0) {
            mockWsServer.takeRequest(1000)
        }

        // Simulate touch down and up
        inputSimulation.simulateSingleTouch(500f, 500f)

        // Wait for WebSocket message
        val request: RecordedRequest = mockWsServer.takeRequest(5000)
        assertNotNull(request)
    }
}
