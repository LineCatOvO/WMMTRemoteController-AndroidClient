package com.linecat.wmmtcontroller.e2e.util;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Input simulation utility for testing various input types
 */
public class InputSimulation {

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    /**
     * Simulate a single touch event at the specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void simulateSingleTouch(float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        // Create a touch down event
        MotionEvent downEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0
        );

        // Inject the down event
        instrumentation.sendPointerSync(downEvent);
        downEvent.recycle();

        // Create a touch up event
        MotionEvent upEvent = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis() + 100,
            MotionEvent.ACTION_UP,
            x,
            y,
            0
        );

        // Inject the up event
        instrumentation.sendPointerSync(upEvent);
        upEvent.recycle();
    }

    /**
     * Simulate multi-touch input
     * @param coordinates List of (x, y) coordinates for each touch point
     */
    public void simulateMultiTouch(List<List<Float>> coordinates) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        // Create a multi-touch down event
        List<MotionEvent> downEvents = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            List<Float> coord = coordinates.get(i);
            float x = coord.get(0);
            float y = coord.get(1);
            int action = (i == 0) ? MotionEvent.ACTION_DOWN : 
                MotionEvent.ACTION_POINTER_DOWN | (i << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            MotionEvent event = MotionEvent.obtain(
                downTime,
                eventTime + i * 50,
                action,
                x,
                y,
                0
            );
            downEvents.add(event);
        }

        // Inject down events
        for (MotionEvent event : downEvents) {
            instrumentation.sendPointerSync(event);
            event.recycle();
        }

        // Wait a bit between down and up
        SystemClock.sleep(200);

        // Create a multi-touch up event
        List<MotionEvent> upEvents = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            List<Float> coord = coordinates.get(i);
            float x = coord.get(0);
            float y = coord.get(1);
            int action;
            if (i == coordinates.size() - 1) {
                action = MotionEvent.ACTION_UP;
            } else {
                action = MotionEvent.ACTION_POINTER_UP | ((coordinates.size() - 1 - i) << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
            MotionEvent event = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis() + (coordinates.size() - 1 - i) * 50,
                action,
                x,
                y,
                0
            );
            upEvents.add(event);
        }

        // Inject up events in reverse order
        for (int i = upEvents.size() - 1; i >= 0; i--) {
            MotionEvent event = upEvents.get(i);
            instrumentation.sendPointerSync(event);
            event.recycle();
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
    public void simulateTouchSlide(float startX, float startY, float endX, float endY, long duration) {
        long downTime = SystemClock.uptimeMillis();
        int steps = 20;
        long stepDuration = duration / steps;

        // Touch down at start position
        MotionEvent downEvent = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            startX,
            startY,
            0
        );
        instrumentation.sendPointerSync(downEvent);
        downEvent.recycle();

        // Move through steps
        for (int i = 1; i < steps; i++) {
            float progress = (float) i / steps;
            float x = startX + (endX - startX) * progress;
            float y = startY + (endY - startY) * progress;

            MotionEvent moveEvent = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis() + i * stepDuration,
                MotionEvent.ACTION_MOVE,
                x,
                y,
                0
            );
            instrumentation.sendPointerSync(moveEvent);
            moveEvent.recycle();

            SystemClock.sleep(stepDuration);
        }

        // Touch up at end position
        MotionEvent upEvent = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis() + duration,
            MotionEvent.ACTION_UP,
            endX,
            endY,
            0
        );
        instrumentation.sendPointerSync(upEvent);
        upEvent.recycle();
    }

    /**
     * Simulate a touch slide with default duration
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX End X coordinate
     * @param endY End Y coordinate
     */
    public void simulateTouchSlide(float startX, float startY, float endX, float endY) {
        simulateTouchSlide(startX, startY, endX, endY, 500);
    }

    /**
     * Simulate a physical button press
     * @param keyCode Key code to simulate
     */
    public void simulatePhysicalButton(int keyCode) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        // Create a key down event
        android.view.KeyEvent downEvent = new android.view.KeyEvent(
            downTime,
            eventTime,
            android.view.KeyEvent.ACTION_DOWN,
            keyCode,
            0,
            0,
            0,
            InputDevice.SOURCE_KEYBOARD,
            0,
            0
        );

        // Inject the down event
        instrumentation.sendKeySync(downEvent);

        // Create a key up event
        android.view.KeyEvent upEvent = new android.view.KeyEvent(
            downTime,
            SystemClock.uptimeMillis() + 100,
            android.view.KeyEvent.ACTION_UP,
            keyCode,
            0,
            0,
            0,
            InputDevice.SOURCE_KEYBOARD,
            0,
            0
        );

        // Inject the up event
        instrumentation.sendKeySync(upEvent);
    }
}