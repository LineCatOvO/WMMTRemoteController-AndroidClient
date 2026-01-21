package com.linecat.wmmtcontroller.e2e.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import com.linecat.wmmtcontroller.service.RuntimeEvents
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Runtime Event Awaiter for E2E testing
 * Provides a way to wait for specific runtime events to occur
 */
class RuntimeAwaiter {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var receiver: BroadcastReceiver? = null

    /**
     * Wait for a specific runtime event
     * @param eventAction The event action to wait for
     * @param timeoutMs Timeout in milliseconds
     * @return True if the event was received, false if timed out
     */
    fun awaitEvent(eventAction: String, timeoutMs: Long = 10000): Boolean {
        val latch = CountDownLatch(1)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == eventAction) {
                    latch.countDown()
                }
            }
        }

        val filter = IntentFilter(eventAction)
        context.registerReceiver(receiver, filter)

        val result = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        context.unregisterReceiver(receiver)
        receiver = null
        return result
    }

    /**
     * Wait for multiple runtime events in order
     * @param eventActions List of event actions to wait for in order
     * @param timeoutMs Timeout per event in milliseconds
     * @return True if all events were received in order, false if timed out
     */
    fun awaitEventsInOrder(eventActions: List<String>, timeoutMs: Long = 5000): Boolean {
        for (eventAction in eventActions) {
            if (!awaitEvent(eventAction, timeoutMs)) {
                return false
            }
        }
        return true
    }

    /**
     * Wait for WebSocket connected event
     * @param timeoutMs Timeout in milliseconds
     * @return True if connected, false if timed out
     */
    fun awaitWsConnected(timeoutMs: Long = 10000): Boolean {
        return awaitEvent(RuntimeEvents.ACTION_WS_CONNECTED, timeoutMs)
    }

    /**
     * Wait for WebSocket sent frame event
     * @param timeoutMs Timeout in milliseconds
     * @return True if frame was sent, false if timed out
     */
    fun awaitWsSentFrame(timeoutMs: Long = 10000): Boolean {
        return awaitEvent(RuntimeEvents.ACTION_WS_SENT_FRAME, timeoutMs)
    }

    /**
     * Wait for profile loaded event
     * @param timeoutMs Timeout in milliseconds
     * @return True if profile loaded, false if timed out
     */
    fun awaitProfileLoaded(timeoutMs: Long = 5000): Boolean {
        return awaitEvent(RuntimeEvents.ACTION_PROFILE_LOADED, timeoutMs)
    }

    /**
     * Wait for runtime started event
     * @param timeoutMs Timeout in milliseconds
     * @return True if runtime started, false if timed out
     */
    fun awaitRuntimeStarted(timeoutMs: Long = 5000): Boolean {
        return awaitEvent(RuntimeEvents.ACTION_RUNTIME_STARTED, timeoutMs)
    }

    /**
     * Wait for script engine ready event
     * @param timeoutMs Timeout in milliseconds
     * @return True if engine ready, false if timed out
     */
    fun awaitScriptEngineReady(timeoutMs: Long = 5000): Boolean {
        return awaitEvent(RuntimeEvents.ACTION_SCRIPT_ENGINE_READY, timeoutMs)
    }

    /**
     * Wait for profile rollback event
     * @param timeoutMs Timeout in milliseconds
     * @return True if profile rolled back, false if timed out
     */
    fun awaitProfileRollback(timeoutMs: Long = 5000): Boolean {
        return awaitEvent(RuntimeEvents.ACTION_PROFILE_ROLLBACK, timeoutMs)
    }
}
