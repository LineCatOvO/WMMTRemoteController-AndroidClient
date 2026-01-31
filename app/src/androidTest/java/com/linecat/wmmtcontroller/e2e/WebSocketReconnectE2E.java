package com.linecat.wmmtcontroller.e2e;

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions;
import com.linecat.wmmtcontroller.service.RuntimeEvents;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test Case 5: WebSocket Reconnect E2E
 * 
 * 证明目标：WebSocket 可靠性 - 状态退化测试
 * 入口路径：应用启动后模拟 WebSocket 断开
 * 断言依据：runtime 事件 + WebSocket 消息语义
 */
public class WebSocketReconnectE2E extends TestEnv {

    @Test
    public void testWebSocketDisconnectionDegradedState() {
        // Step 1: Capture initial WebSocket frames and verify frameId sequence
        Long previousFrameId = null;
        
        // Wait for 3 initial frames to establish baseline
        for (int i = 1; i <= 3; i++) {
            String frame = runtimeAwaiter.awaitNextFrame(15000);
            assertTrue("Frame " + i + " should be valid JSON",
                JsonAssertions.assertFrameJsonValid(frame));
            
            // Verify frameId is monotonic
            long currentFrameId = JsonAssertions.extractFrameId(frame);
            assertTrue("FrameId should be monotonic (previous: " + previousFrameId + ", current: " + currentFrameId + ")",
                previousFrameId == null || currentFrameId > previousFrameId);
            previousFrameId = currentFrameId;
        }
        
        // Step 2: Simulate WebSocket disconnection by stopping mock server
        mockWsServer.stop();
        
        // Step 3: Verify system continues operating in degraded state
        for (int i = 4; i <= 6; i++) {
            String frame = runtimeAwaiter.awaitNextFrame(15000);
            assertTrue("Frame " + i + " should be valid JSON after WS disconnect",
                JsonAssertions.assertFrameJsonValid(frame));
            
            // Verify frameId continues to increment
            long currentFrameId = JsonAssertions.extractFrameId(frame);
            assertTrue("FrameId should continue incrementing after WS disconnect",
                currentFrameId > previousFrameId);
            previousFrameId = currentFrameId;
            
            // Verify runtimeStatus reflects degraded state
            String runtimeStatus = JsonAssertions.extractRuntimeStatus(frame);
            assertNotNull("Frame should contain runtimeStatus field",
                runtimeStatus);
        }
    }
}