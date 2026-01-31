package com.linecat.wmmtcontroller.e2e.util;

import com.google.gson.Gson;
import com.linecat.wmmtcontroller.model.InputState;
import okhttp3.mockwebserver.RecordedRequest;
import static org.junit.Assert.*;

/**
 * JSON assertion utilities for E2E tests
 * Provides methods for parsing and validating InputState from WebSocket messages
 */
public class JsonAssertions {

    private static final Gson gson = new Gson();
    private static long lastFrameId = -1;

    /**
     * Parse InputState from WebSocket request
     * @param request Recorded WebSocket request
     * @return Parsed InputState
     */
    public static InputState parseInputState(RecordedRequest request) {
        String body = request.getBody().readUtf8();
        return gson.fromJson(body, InputState.class);
    }

    /**
     * Validate InputState sequence
     * @param previousState Previous InputState
     * @param currentState Current InputState
     * @return True if sequence is valid (current frameId > previous frameId)
     */
    public static boolean validateSequence(InputState previousState, InputState currentState) {
        return currentState.getFrameId() > previousState.getFrameId();
    }

    /**
     * Validate InputState is zero state
     * @param state InputState to check
     * @return True if state is zero state
     */
    public static boolean validateZeroState(InputState state) {
        return state.getKeyboard().isEmpty() &&
               state.getMouse().getX() == 0.0f &&
               state.getMouse().getY() == 0.0f &&
               !state.getMouse().isLeft() &&
               !state.getMouse().isRight() &&
               !state.getMouse().isMiddle() &&
               state.getJoystick().getX() == 0.0f &&
               state.getJoystick().getY() == 0.0f &&
               state.getGyroscope().getPitch() == 0.0f &&
               state.getGyroscope().getRoll() == 0.0f &&
               state.getGyroscope().getYaw() == 0.0f;
    }

    /**
     * Assert that frame JSON is valid
     * @param request Recorded WebSocket request
     * @return True if frame is valid
     */
    public static boolean assertFrameJsonValid(RecordedRequest request) {
        InputState state = parseInputState(request);
        assertNotNull("InputState should not be null", state);
        return true;
    }

    /**
     * Assert that frame JSON is valid
     * @param jsonString JSON string to validate
     * @return True if frame is valid
     */
    public static boolean assertFrameJsonValid(String jsonString) {
        InputState state = gson.fromJson(jsonString, InputState.class);
        assertNotNull("InputState should not be null", state);
        return true;
    }

    /**
     * Parse InputState from JSON string
     * @param jsonString JSON string to parse
     * @return Parsed InputState
     */
    public static InputState parseInputState(String jsonString) {
        return gson.fromJson(jsonString, InputState.class);
    }

    /**
     * Assert that keyboard keys are empty
     * @param state InputState to check
     * @return True if keyboard is empty
     */
    public static boolean assertHeldKeysEmpty(InputState state) {
        assertTrue("Keyboard should be empty, got " + state.getKeyboard(), state.getKeyboard().isEmpty());
        return true;
    }

    /**
     * Assert that keyboard keys are empty
     * @param jsonString JSON string to check
     * @return True if keyboard is empty
     */
    public static boolean assertHeldKeysEmpty(String jsonString) {
        InputState state = parseInputState(jsonString);
        return assertHeldKeysEmpty(state);
    }

    /**
     * Assert that frame has monotonic frameId
     * @param state InputState to check
     */
    public static void assertFrameHasMonotonicFrameId(InputState state) {
        if (lastFrameId >= 0) {
            assertTrue("FrameId should be monotonic: last=" + lastFrameId + ", current=" + state.getFrameId(),
                state.getFrameId() > lastFrameId);
        }
        lastFrameId = state.getFrameId();
    }

    /**
     * Assert that frame has monotonic frameId compared to previous frame
     * @param previousFrameJson Previous frame JSON string
     * @param currentFrameJson Current frame JSON string
     * @return True if frameId is monotonic
     */
    public static boolean assertFrameHasMonotonicFrameId(String previousFrameJson, String currentFrameJson) {
        InputState currentState = parseInputState(currentFrameJson);
        if (previousFrameJson != null) {
            InputState previousState = parseInputState(previousFrameJson);
            boolean result = currentState.getFrameId() > previousState.getFrameId();
            if (!result) {
                System.out.println("FrameId is not monotonic: previous=" + previousState.getFrameId() + ", current=" + currentState.getFrameId());
            }
            return result;
        } else {
            lastFrameId = currentState.getFrameId();
            return true;
        }
    }

    /**
     * Extract frameId from InputState
     * @param state InputState
     * @return frameId
     */
    public static long extractFrameId(InputState state) {
        return state.getFrameId();
    }

    /**
     * Extract frameId from JSON string
     * @param jsonString JSON string
     * @return frameId
     */
    public static long extractFrameId(String jsonString) {
        InputState state = parseInputState(jsonString);
        return state.getFrameId();
    }

    /**
     * Extract runtimeStatus from InputState
     * @param state InputState
     * @return runtimeStatus
     */
    public static String extractRuntimeStatus(InputState state) {
        return state.getRuntimeStatus();
    }

    /**
     * Extract runtimeStatus from JSON string
     * @param jsonString JSON string
     * @return runtimeStatus
     */
    public static String extractRuntimeStatus(String jsonString) {
        InputState state = parseInputState(jsonString);
        return state.getRuntimeStatus();
    }
}