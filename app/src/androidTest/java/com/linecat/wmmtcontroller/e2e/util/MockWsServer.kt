package com.linecat.wmmtcontroller.e2e.util

import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.IOException

/**
 * Mock WebSocket Server for E2E testing
 * Provides a controlled WebSocket server environment for testing
 */
class MockWsServer {

    private val mockServer = MockWebServer()

    /**
     * Start the mock server
     */
    fun start() {
        try {
            mockServer.start()
        } catch (e: IOException) {
            throw RuntimeException("Failed to start mock server", e)
        }
    }

    /**
     * Stop the mock server
     */
    fun stop() {
        try {
            mockServer.shutdown()
        } catch (e: IOException) {
            throw RuntimeException("Failed to stop mock server", e)
        }
    }

    /**
     * Get the WebSocket URL for the mock server
     * @return WebSocket URL
     */
    fun getWsUrl(): String {
        return "ws://127.0.0.1:${mockServer.port}/ws/input"
    }

    /**
     * Take the next WebSocket message from the queue
     * @param timeoutMs Timeout in milliseconds
     * @return RecordedRequest containing the message
     */
    @Throws(InterruptedException::class)
    fun takeRequest(timeoutMs: Long = 5000): RecordedRequest {
        val request = mockServer.takeRequest(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        checkNotNull(request) { "No WebSocket message received within timeout: ${timeoutMs}ms" }
        return request
    }

    /**
     * Get the request count
     * @return Number of requests received
     */
    fun getRequestCount(): Int {
        return mockServer.requestCount
    }
}
