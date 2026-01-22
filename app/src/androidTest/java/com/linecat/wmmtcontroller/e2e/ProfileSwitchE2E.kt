package com.linecat.wmmtcontroller.e2e

import com.linecat.wmmtcontroller.e2e.util.JsonAssertions
import com.linecat.wmmtcontroller.service.RuntimeEvents
import org.junit.Test

/**
 * Test Case 3: Profile Switch E2E
 * 
 * 证明目标：G4
 * 入口路径：应用启动后切换 Profile
 * 断言依据：WebSocket 消息内容
 */
class ProfileSwitchE2E : TestEnv() {

    @Test
    fun testProfileSwitchAndFullRelease() {
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

        // Step 3: Switch to a different profile
        runtimeConfig.setProfileId("official-profiles/wmmt_gamepad_standard")

        // Step 4: Send a broadcast to trigger profile reload (this would typically be done via UI)
        // Note: In a real UI test, we would use Espresso to click on a profile selection UI element
        // For this test, we'll simulate the profile switch by restarting the service
        val intent = android.content.Intent(context, com.linecat.wmmtcontroller.service.InputRuntimeService::class.java)
        context.stopService(intent)
        context.startService(intent)

        // Step 5: Wait for profile loaded event
        assert(runtimeAwaiter.awaitProfileLoaded(5000)) {
            "Failed to receive PROFILE_LOADED event after profile switch"
        }

        // Step 6: Wait for WebSocket frame after profile switch
        val wsMessage = runtimeAwaiter.awaitNextFrame(5000)

        // Step 7: Verify the "full release" semantics - heldKeys should be empty after profile switch

        // Assert frame is valid JSON
        assert(JsonAssertions.assertFrameJsonValid(wsMessage)) {
            "WebSocket frame after profile switch is not valid JSON structure"
        }
        
        // Verify the "full release" semantics - heldKeys should be empty after profile switch
        assert(JsonAssertions.assertHeldKeysEmpty(wsMessage)) {
            "Expected heldKeys to be empty after profile switch, but got: $wsMessage"
        }
    }
}
