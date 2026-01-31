package com.linecat.wmmtcontroller.e2e.util;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Mock WebSocket Server for E2E testing
 * Provides a controlled WebSocket server environment for testing
 */
public class MockWsServer {

    private final MockWebServer mockServer = new MockWebServer();

    /**
     * Start the mock server
     */
    public void start() {
        try {
            mockServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start mock server", e);
        }
    }

    /**
     * Stop the mock server
     */
    public void stop() {
        try {
            mockServer.shutdown();
        } catch (IOException e) {
            throw new RuntimeException("Failed to stop mock server", e);
        }
    }

    /**
     * Get the WebSocket URL for the mock server
     * @return WebSocket URL
     */
    public String getWsUrl() {
        // Use 10.0.2.2 instead of 127.0.0.1 for emulator to access host machine
        return "ws://10.0.2.2:" + mockServer.getPort() + "/ws/input";
    }

    /**
     * Take the next WebSocket message from the queue
     * @param timeoutMs Timeout in milliseconds
     * @return RecordedRequest containing the message
     * @throws InterruptedException if interrupted while waiting
     */
    public RecordedRequest takeRequest(long timeoutMs) throws InterruptedException {
        RecordedRequest request = mockServer.takeRequest(timeoutMs, TimeUnit.MILLISECONDS);
        if (request == null) {
            throw new IllegalStateException("No WebSocket message received within timeout: " + timeoutMs + "ms");
        }
        return request;
    }

    /**
     * Take the next WebSocket message from the queue with default timeout
     * @return RecordedRequest containing the message
     * @throws InterruptedException if interrupted while waiting
     */
    public RecordedRequest takeRequest() throws InterruptedException {
        return takeRequest(5000);
    }

    /**
     * Get the request count
     * @return Number of requests received
     */
    public int getRequestCount() {
        return mockServer.getRequestCount();
    }
}