package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test Case 4: Script Failure Rollback E2E
 * 
 * 证明目标：G5
 * 入口路径：应用启动后使用故障脚本
 * 断言依据：runtime 事件 + WebSocket 消息
 */
class ScriptFailureRollbackE2E : TestEnv() {

    @Test
    fun testScriptFailureAutoRollback() {
        // Step 1: Wait for initial WebSocket frames and verify frameId monotonicity
        var previousFrame: String? = null
        for (i in 1..3) {
            val wsMessage = runtimeAwaiter.awaitNextFrame(15000)
            
            // Assert frame is valid JSON
            assert(JsonAssertions.assertFrameJsonValid(wsMessage)) {
                "WebSocket frame $i is not valid JSON structure"
            }
            
            // Assert frameId is monotonically increasing
            assert(JsonAssertions.assertFrameHasMonotonicFrameId(previousFrame, wsMessage)) {
                "FrameId is not monotonically increasing for frame $i"
            }
            
            previousFrame = wsMessage
        }

        // Step 3: Switch to the faulty script profile
        runtimeConfig.setProfileId("official-profiles/fault_script_throw")

        // Step 4: Restart the service to apply the faulty profile
        val intent = android.content.Intent(context, com.linecat.wmmtcontroller.service.InputRuntimeService::class.java)
        context.stopService(intent)
        // Remove Thread.sleep - use events to wait for service restart
        
        // Step 5: Wait for runtime error event
        assert(runtimeAwaiter.awaitEvent(RuntimeEvents.ACTION_RUNTIME_ERROR, 10000)) {
            "Failed to receive RUNTIME_ERROR event from faulty script"
        }

        // Step 6: Wait for profile rollback event
        assert(runtimeAwaiter.awaitProfileRollback(5000)) {
            "Failed to receive PROFILE_ROLLBACK event after script error"
        }

        // Step 7: Wait for WebSocket frame after rollback
        val wsMessage = runtimeAwaiter.awaitNextFrame(5000)

        // Step 8: Verify the system continues sending WebSocket frames
        // Assert frame is valid JSON
        assert(JsonAssertions.assertFrameJsonValid(wsMessage)) {
            "WebSocket frame after rollback is not valid JSON structure"
        }
        
        // Verify the message has expected structure
        assert(wsMessage.contains("keyboard")) {
            "Expected keyboard field in WebSocket message after rollback"
        }
        assert(wsMessage.contains("mouse")) {
            "Expected mouse field in WebSocket message after rollback"
        }
        assert(wsMessage.contains("joystick")) {
            "Expected joystick field in WebSocket message after rollback"
        }
        assert(wsMessage.contains("gyroscope")) {
            "Expected gyroscope field in WebSocket message after rollback"
        }

        // Step 9: Verify the system continues running
        for (i in 1..3) {
            runtimeAwaiter.awaitNextFrame(5000)
        }
    }
}
