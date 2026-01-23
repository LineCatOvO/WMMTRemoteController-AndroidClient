package com.linecat.wmmtcontroller.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for InputState model
 * Tests InputState generation, validation, and behavior
 */
public class InputStateTest {

    private InputState inputState;

    @Before
    public void setUp() {
        // Create a new InputState for each test
        inputState = new InputState();
    }

    @Test
    public void testInitialState() {
        // Verify initial state values
        assertEquals(0, inputState.getFrameId());
        assertEquals("ok", inputState.getRuntimeStatus());
        assertNotNull(inputState.getKeyboard());
        assertTrue(inputState.getKeyboard().isEmpty());
        assertNotNull(inputState.getMouse());
        assertNotNull(inputState.getJoystick());
        assertNotNull(inputState.getGyroscope());
    }

    @Test
    public void testCopyConstructor() {
        // Set up original state
        inputState.setFrameId(42);
        inputState.setRuntimeStatus("warning");
        inputState.getKeyboard().add("W");
        inputState.getKeyboard().add("A");
        inputState.getMouse().setX(100.0f);
        inputState.getMouse().setY(200.0f);
        inputState.getMouse().setLeft(true);
        inputState.getJoystick().setX(0.5f);
        inputState.getJoystick().setY(-0.5f);
        inputState.getGyroscope().setPitch(10.0f);

        // Create copy
        InputState copy = new InputState(inputState);

        // Verify copy matches original
        assertEquals(inputState.getFrameId(), copy.getFrameId());
        assertEquals(inputState.getRuntimeStatus(), copy.getRuntimeStatus());
        assertEquals(inputState.getKeyboard(), copy.getKeyboard());
        assertEquals(inputState.getMouse().getX(), copy.getMouse().getX(), 0.001f);
        assertEquals(inputState.getMouse().getY(), copy.getMouse().getY(), 0.001f);
        assertEquals(inputState.getMouse().isLeft(), copy.getMouse().isLeft());
        assertEquals(inputState.getJoystick().getX(), copy.getJoystick().getX(), 0.001f);
        assertEquals(inputState.getJoystick().getY(), copy.getJoystick().getY(), 0.001f);
        assertEquals(inputState.getGyroscope().getPitch(), copy.getGyroscope().getPitch(), 0.001f);

        // Verify deep copy
        inputState.getKeyboard().add("S");
        assertNotEquals(inputState.getKeyboard(), copy.getKeyboard());
    }

    @Test
    public void testClearAllKeys() {
        // Add some keys
        inputState.getKeyboard().add("W");
        inputState.getKeyboard().add("A");
        inputState.getKeyboard().add("S");
        assertFalse(inputState.getKeyboard().isEmpty());

        // Clear all keys
        inputState.clearAllKeys();
        assertTrue(inputState.getKeyboard().isEmpty());
    }

    @Test
    public void testMouseState() {
        // Test mouse state manipulation
        InputState.MouseState mouse = inputState.getMouse();
        mouse.setX(150.0f);
        mouse.setY(250.0f);
        mouse.setLeft(true);
        mouse.setRight(false);
        mouse.setMiddle(true);

        assertEquals(150.0f, mouse.getX(), 0.001f);
        assertEquals(250.0f, mouse.getY(), 0.001f);
        assertTrue(mouse.isLeft());
        assertFalse(mouse.isRight());
        assertTrue(mouse.isMiddle());
    }

    @Test
    public void testJoystickState() {
        // Test joystick state manipulation
        InputState.JoystickState joystick = inputState.getJoystick();
        joystick.setX(0.75f);
        joystick.setY(-0.25f);
        joystick.setDeadzone(0.1f);
        joystick.setSmoothing(0.5f);

        assertEquals(0.75f, joystick.getX(), 0.001f);
        assertEquals(-0.25f, joystick.getY(), 0.001f);
        assertEquals(0.1f, joystick.getDeadzone(), 0.001f);
        assertEquals(0.5f, joystick.getSmoothing(), 0.001f);
    }

    @Test
    public void testGyroscopeState() {
        // Test gyroscope state manipulation
        InputState.GyroscopeState gyroscope = inputState.getGyroscope();
        gyroscope.setPitch(15.0f);
        gyroscope.setRoll(-10.0f);
        gyroscope.setYaw(5.0f);
        gyroscope.setDeadzone(2.0f);
        gyroscope.setSmoothing(0.3f);

        assertEquals(15.0f, gyroscope.getPitch(), 0.001f);
        assertEquals(-10.0f, gyroscope.getRoll(), 0.001f);
        assertEquals(5.0f, gyroscope.getYaw(), 0.001f);
        assertEquals(2.0f, gyroscope.getDeadzone(), 0.001f);
        assertEquals(0.3f, gyroscope.getSmoothing(), 0.001f);
    }

    @Test
    public void testSequenceMonotonicity() {
        // Test that frameId can be incremented monotonically
        long initialFrameId = inputState.getFrameId();
        inputState.setFrameId(initialFrameId + 1);
        assertEquals(initialFrameId + 1, inputState.getFrameId());
        
        inputState.setFrameId(initialFrameId + 2);
        assertEquals(initialFrameId + 2, inputState.getFrameId());
    }

    @Test
    public void testKeyboardKeys() {
        // Test keyboard key addition and removal
        List<String> keys = new ArrayList<>();
        keys.add("W");
        keys.add("A");
        keys.add("D");
        
        inputState.setKeyboard(keys);
        assertEquals(3, inputState.getKeyboard().size());
        assertTrue(inputState.getKeyboard().contains("W"));
        assertTrue(inputState.getKeyboard().contains("A"));
        assertTrue(inputState.getKeyboard().contains("D"));
        
        // Remove a key
        keys.remove("A");
        inputState.setKeyboard(keys);
        assertEquals(2, inputState.getKeyboard().size());
        assertFalse(inputState.getKeyboard().contains("A"));
    }

    @Test
    public void testRuntimeStatus() {
        // Test runtime status changes
        inputState.setRuntimeStatus("ok");
        assertEquals("ok", inputState.getRuntimeStatus());
        
        inputState.setRuntimeStatus("warning");
        assertEquals("warning", inputState.getRuntimeStatus());
        
        inputState.setRuntimeStatus("error");
        assertEquals("error", inputState.getRuntimeStatus());
    }
}
