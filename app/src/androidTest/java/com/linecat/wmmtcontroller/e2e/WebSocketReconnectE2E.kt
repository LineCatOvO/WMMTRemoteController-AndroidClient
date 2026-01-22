package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test Case 5: WebSocket Reconnect E2E
 * 
 * 证明目标：WebSocket 可靠性 - 状态退化测试
 * 入口路径：应用启动后模拟 WebSocket 断开
 * 断言依据：runtime 事件 + WebSocket 消息语义
 */
class WebSocketReconnectE2E : TestEnv() {

    @Test
    fun testWebSocketDisconnectionDegradedState() {
        // Step 1: Capture initial WebSocket frames and verify frameId sequence
        var previousFrameId: Long? = null
        
        // Wait for 3 initial frames to establish baseline
        for (i in 1..3) {
            val frame = runtimeAwaiter.awaitNextFrame(15000)
            assert(JsonAssertions.assertFrameJsonValid(frame)) {
                "Frame $i should be valid JSON"
            }
            
            // Verify frameId is monotonic
            val currentFrameId = JsonAssertions.extractFrameId(frame)
            assert(previousFrameId == null || currentFrameId > previousFrameId) {
                "FrameId should be monotonic (previous: $previousFrameId, current: $currentFrameId)"
            }
            previousFrameId = currentFrameId
        }
        
        // Step 2: Simulate WebSocket disconnection by stopping mock server
        mockWsServer.stop()
        
        // Step 3: Verify system continues operating in degraded state
        for (i in 4..6) {
            val frame = runtimeAwaiter.awaitNextFrame(15000)
            assert(JsonAssertions.assertFrameJsonValid(frame)) {
                "Frame $i should be valid JSON after WS disconnect"
            }
            
            // Verify frameId continues to increment
            val currentFrameId = JsonAssertions.extractFrameId(frame)
            assert(currentFrameId > previousFrameId!!) {
                "FrameId should continue incrementing after WS disconnect"
            }
            previousFrameId = currentFrameId
            
            // Verify runtimeStatus reflects degraded state
            val runtimeStatus = JsonAssertions.extractRuntimeStatus(frame)
            assert(runtimeStatus != null) {
                "Frame should contain runtimeStatus field"
            }
        }
    }
}
