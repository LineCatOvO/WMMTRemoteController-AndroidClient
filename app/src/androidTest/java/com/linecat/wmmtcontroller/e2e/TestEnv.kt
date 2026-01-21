package com.linecat.wmmtcontroller.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecat.wmmtcontroller.MainActivity
import com.linecat.wmmtcontroller.e2e.util.MockWsServer
import com.linecat.wmmtcontroller.e2e.util.RuntimeAwaiter
import com.linecat.wmmtcontroller.service.RuntimeConfig
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base test environment for all E2E tests
 * Provides shared setup and teardown functionality
 */
@RunWith(AndroidJUnit4::class)
open class TestEnv {

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val mockWsServer = MockWsServer()
    protected val runtimeAwaiter = RuntimeAwaiter()
    protected lateinit var runtimeConfig: RuntimeConfig

    @Rule
    @JvmField
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        // Start the mock WebSocket server
        mockWsServer.start()

        // Initialize RuntimeConfig
        runtimeConfig = RuntimeConfig(context)

        // Clear previous configuration
        runtimeConfig.clear()

        // Set test configuration
        runtimeConfig.setWebSocketUrl(mockWsServer.getWsUrl())
        runtimeConfig.setProfileId("official-profiles/wmmt_keyboard_basic")
        runtimeConfig.setUseScriptRuntime(true)
    }

    @After
    fun teardown() {
        // Stop the mock WebSocket server
        mockWsServer.stop()

        // Clear configuration
        runtimeConfig.clear()
    }
    
    /**
     * Dump diagnostics information - call manually for debugging
     */
    fun dumpDiagnostics() {
        println("=== TEST DIAGNOSTICS ===")
        
        // WebSocket messages
        println("\n1. WebSocket Messages:")
        println("   Received ${mockWsServer.getRequestCount()} messages")
        
        // Current profile and configuration
        println("\n2. Current Configuration:")
        println("   Profile ID: ${runtimeConfig.getProfileId()}")
        println("   WebSocket URL: ${runtimeConfig.getWebSocketUrl()}")
        println("   Use Script Runtime: ${runtimeConfig.useScriptRuntime()}")
        
        println("\n=== END DIAGNOSTICS ===")
    }
}
