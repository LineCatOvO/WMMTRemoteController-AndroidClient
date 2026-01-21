package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test Case 5: WebSocket Reconnect E2E
 * 
 * 证明目标：WebSocket 可靠性
 * 入口路径：应用启动后断开并重新连接 WebSocket
 * 断言依据：runtime 事件 + WebSocket 消息
 */
class WebSocketReconnectE2E : TestEnv() {

    @Test
    fun testWebSocketDisconnectAndReconnect() {
        // Step 1: Wait for runtime startup and WebSocket connection
        val startupEvents = listOf(
            RuntimeEvents.ACTION_RUNTIME_STARTED,
            RuntimeEvents.ACTION_PROFILE_LOADED,
            RuntimeEvents.ACTION_SCRIPT_ENGINE_READY,
            RuntimeEvents.ACTION_WS_CONNECTED
        )

        assert(runtimeAwaiter.awaitEventsInOrder(startupEvents, 5000)) {
            "Failed to receive all required startup events"
        }

        // Step 2: Wait for initial WebSocket frames
        for (i in 1..3) {
            assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
                "Failed to receive WS_SENT_FRAME event $i"
            }
            mockWsServer.takeRequest()
        }

        // Step 3: Stop the mock server to simulate disconnection
        mockWsServer.stop()

        // Step 4: Wait for WebSocket disconnected event
        assert(runtimeAwaiter.awaitEvent(RuntimeEvents.ACTION_WS_DISCONNECTED, 10000)) {
            "Failed to receive WS_DISCONNECTED event after server shutdown"
        }

        // Step 5: Start the mock server again
        mockWsServer.start()

        // Step 6: Update the WebSocket URL in runtime config (port may have changed)
        runtimeConfig.setWebSocketUrl(mockWsServer.getWsUrl())

        // Step 7: Wait for WebSocket connected event after reconnection
        assert(runtimeAwaiter.awaitEvent(RuntimeEvents.ACTION_WS_CONNECTED, 15000)) {
            "Failed to receive WS_CONNECTED event after server restart"
        }

        // Step 8: Wait for WebSocket frames after reconnection
        for (i in 1..3) {
            assert(runtimeAwaiter.awaitWsSentFrame(5000)) {
                "Failed to receive WS_SENT_FRAME event $i after reconnection"
            }
            val wsRequest = mockWsServer.takeRequest()
            val wsMessage = wsRequest.body.readUtf8()
            assert(wsMessage.isNotEmpty()) {
                "WebSocket message $i after reconnection should not be empty"
            }
        }
    }
}
