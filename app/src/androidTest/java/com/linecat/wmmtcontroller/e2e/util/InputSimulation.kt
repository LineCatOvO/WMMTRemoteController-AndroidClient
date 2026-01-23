package com.linecat.wmmtcontroller.e2e.util

import android.app.Instrumentation
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Input simulation utility for testing various input types
 */
class InputSimulation {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /**
     * Simulate a single touch event at the specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     */
    fun simulateSingleTouch(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        // Create a touch down event
        val downEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0
        )

        // Inject the down event
        instrumentation.sendPointerSync(downEvent)
        downEvent.recycle()

        // Create a touch up event
        val upEvent = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis() + 100,
            MotionEvent.ACTION_UP,
            x,
            y,
            0
        )

        // Inject the up event
        instrumentation.sendPointerSync(upEvent)
        upEvent.recycle()
    }

    /**
     * Simulate multi-touch input
     * @param coordinates List of (x, y) coordinates for each touch point
     */
    fun simulateMultiTouch(coordinates: List<Pair<Float, Float>>) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        // Create a multi-touch down event
        val downEvents = ArrayList<MotionEvent>()
        coordinates.forEachIndexed { index, (x, y) ->
            val event = MotionEvent.obtain(
                downTime,
                eventTime + index * 50,
                if (index == 0) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_POINTER_DOWN or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                x,
                y,
                0
            )
            downEvents.add(event)
        }

        // Inject down events
        downEvents.forEach { event ->
            instrumentation.sendPointerSync(event)
            event.recycle()
        }

        // Wait a bit between down and up
        SystemClock.sleep(200)

        // Create a multi-touch up event
        val upEvents = ArrayList<MotionEvent>()
        coordinates.forEachIndexed { index, (x, y) ->
            val action = if (index == coordinates.size - 1) {
                MotionEvent.ACTION_UP
            } else {
                MotionEvent.ACTION_POINTER_UP or ((coordinates.size - 1 - index) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
            val event = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis() + (coordinates.size - 1 - index) * 50,
                action,
                x,
                y,
                0
            )
            upEvents.add(event)
        }

        // Inject up events in reverse order
        upEvents.reversed().forEach { event ->
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
    }

    /**
     * Simulate a touch slide from start to end coordinates
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX End X coordinate
     * @param endY End Y coordinate
     * @param duration Duration of the slide in milliseconds
     */
    fun simulateTouchSlide(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 500) {
        val downTime = SystemClock.uptimeMillis()
        val steps = 20
        val stepDuration = duration / steps

        // Touch down at start position
        val downEvent = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            startX,
            startY,
            0
        )
        instrumentation.sendPointerSync(downEvent)
        downEvent.recycle()

        // Move through steps
        for (i in 1 until steps) {
            val progress = i.toFloat() / steps
            val x = startX + (endX - startX) * progress
            val y = startY + (endY - startY) * progress

            val moveEvent = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis() + i * stepDuration,
                MotionEvent.ACTION_MOVE,
                x,
                y,
                0
            )
            instrumentation.sendPointerSync(moveEvent)
            moveEvent.recycle()

            SystemClock.sleep(stepDuration)
        }

        // Touch up at end position
        val upEvent = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis() + duration,
            MotionEvent.ACTION_UP,
            endX,
            endY,
            0
        )
        instrumentation.sendPointerSync(upEvent)
        upEvent.recycle()
    }

    /**
     * Simulate a physical button press
     * @param keyCode Key code to simulate
     */
    fun simulatePhysicalButton(keyCode: Int) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        // Create a key down event
        val downEvent = android.view.KeyEvent(
            downTime,
            eventTime,
            android.view.KeyEvent.ACTION_DOWN,
            keyCode,
            0,
            0,
            InputDevice.SOURCE_KEYBOARD,
            0,
            0
        )

        // Inject the down event
        instrumentation.sendKeySync(downEvent)

        // Create a key up event
        val upEvent = android.view.KeyEvent(
            downTime,
            SystemClock.uptimeMillis() + 100,
            android.view.KeyEvent.ACTION_UP,
            keyCode,
            0,
            0,
            InputDevice.SOURCE_KEYBOARD,
            0,
            0
        )

        // Inject the up event
        instrumentation.sendKeySync(upEvent)
    }
}
