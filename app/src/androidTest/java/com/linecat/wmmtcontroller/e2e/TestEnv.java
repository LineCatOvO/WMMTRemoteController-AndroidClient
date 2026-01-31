package com.linecat.wmmtcontroller.e2e;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.linecat.wmmtcontroller.MainActivity;
import com.linecat.wmmtcontroller.e2e.util.MockWsServer;
import com.linecat.wmmtcontroller.e2e.util.RuntimeAwaiter;
import com.linecat.wmmtcontroller.service.InputRuntimeService;
import com.linecat.wmmtcontroller.service.RuntimeConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 * Base test environment for all E2E tests
 * Provides shared setup and teardown functionality
 */
public class TestEnv {

    protected final Context context = ApplicationProvider.getApplicationContext();
    protected final MockWsServer mockWsServer = new MockWsServer();
    protected final RuntimeAwaiter runtimeAwaiter = new RuntimeAwaiter();
    protected RuntimeConfig runtimeConfig;

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() {
        // Start the mock WebSocket server
        mockWsServer.start();
        System.out.println("Mock WebSocket server started at " + mockWsServer.getWsUrl());

        // Initialize RuntimeConfig
        runtimeConfig = new RuntimeConfig(context);

        // Clear previous configuration
        runtimeConfig.clear();

        // Set test configuration
        runtimeConfig.setWebSocketUrl(mockWsServer.getWsUrl());
        runtimeConfig.setProfileId("official-profiles/wmmt_keyboard_basic");
        runtimeConfig.setUseScriptRuntime(true);
        System.out.println("Test configuration set");

        // Explicitly start InputRuntimeService
        Intent serviceIntent = new Intent(context, InputRuntimeService.class);
        context.startService(serviceIntent);
        System.out.println("InputRuntimeService started");

        // Wait for Runtime to start completely
        boolean started = runtimeAwaiter.awaitRuntimeStarted(10000);
        System.out.println("Runtime started: " + started);
    }

    @After
    public void teardown() {
        // Stop the mock WebSocket server
        mockWsServer.stop();

        // Clear configuration
        runtimeConfig.clear();
    }
    
    /**
     * Dump diagnostics information - call manually for debugging
     */
    public void dumpDiagnostics() {
        System.out.println("=== TEST DIAGNOSTICS ===");
        
        // WebSocket messages
        System.out.println("\n1. WebSocket Messages:");
        System.out.println("   Received " + mockWsServer.getRequestCount() + " messages");
        
        // Current profile and configuration
        System.out.println("\n2. Current Configuration:");
        System.out.println("   Profile ID: " + runtimeConfig.getProfileId());
        System.out.println("   WebSocket URL: " + runtimeConfig.getWebSocketUrl());
        System.out.println("   Use Script Runtime: " + runtimeConfig.useScriptRuntime());
        
        System.out.println("\n=== END DIAGNOSTICS ===");
    }
}