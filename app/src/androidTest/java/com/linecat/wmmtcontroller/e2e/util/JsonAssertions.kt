package com.linecat.wmmtcontroller.e2e.util

import com.google.gson.Gson
import com.linecat.wmmtcontroller.model.InputState
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*

/**
 * JSON assertion utilities for E2E tests
 * Provides methods for parsing and validating InputState from WebSocket messages
 */
class JsonAssertions {

    companion object {
        private val gson = Gson()
        private var lastFrameId: Long = -1

        /**
         * Parse InputState from WebSocket request
         * @param request Recorded WebSocket request
         * @return Parsed InputState
         */
        fun parseInputState(request: RecordedRequest): InputState {
            val body = request.body.readUtf8()
            return gson.fromJson(body, InputState::class.java)
        }

        /**
         * Validate InputState sequence
         * @param previousState Previous InputState
         * @param currentState Current InputState
         * @return True if sequence is valid (current frameId > previous frameId)
         */
        fun validateSequence(previousState: InputState, currentState: InputState): Boolean {
            return currentState.frameId > previousState.frameId
        }

        /**
         * Validate InputState is zero state
         * @param state InputState to check
         * @return True if state is zero state
         */
        fun validateZeroState(state: InputState): Boolean {
            return state.keyboard.isEmpty() &&
                   state.mouse.x == 0.0f &&
                   state.mouse.y == 0.0f &&
                   !state.mouse.isLeft() &&
                   !state.mouse.isRight() &&
                   !state.mouse.isMiddle() &&
                   state.joystick.x == 0.0f &&
                   state.joystick.y == 0.0f &&
                   state.gyroscope.pitch == 0.0f &&
                   state.gyroscope.roll == 0.0f &&
                   state.gyroscope.yaw == 0.0f
        }

        /**
         * Assert that frame JSON is valid
         * @param request Recorded WebSocket request
         * @return True if frame is valid
         */
        fun assertFrameJsonValid(request: RecordedRequest): Boolean {
            val state = parseInputState(request)
            assertNotNull("InputState should not be null", state)
            return true
        }

        /**
         * Assert that frame JSON is valid
         * @param jsonString JSON string to validate
         * @return True if frame is valid
         */
        fun assertFrameJsonValid(jsonString: String): Boolean {
            val state = gson.fromJson(jsonString, InputState::class.java)
            assertNotNull("InputState should not be null", state)
            return true
        }

        /**
         * Parse InputState from JSON string
         * @param jsonString JSON string to parse
         * @return Parsed InputState
         */
        fun parseInputState(jsonString: String): InputState {
            return gson.fromJson(jsonString, InputState::class.java)
        }

        /**
         * Assert that keyboard keys are empty
         * @param state InputState to check
         * @return True if keyboard is empty
         */
        fun assertHeldKeysEmpty(state: InputState): Boolean {
            assertTrue("Keyboard should be empty, got ${state.keyboard}", state.keyboard.isEmpty())
            return true
        }

        /**
         * Assert that keyboard keys are empty
         * @param jsonString JSON string to check
         * @return True if keyboard is empty
         */
        fun assertHeldKeysEmpty(jsonString: String): Boolean {
            val state = parseInputState(jsonString)
            return assertHeldKeysEmpty(state)
        }

        /**
         * Assert that frame has monotonic frameId
         * @param state InputState to check
         */
        fun assertFrameHasMonotonicFrameId(state: InputState) {
            if (lastFrameId >= 0) {
                assertTrue("FrameId should be monotonic: last=$lastFrameId, current=${state.frameId}",
                    state.frameId > lastFrameId)
            }
            lastFrameId = state.frameId
        }

        /**
         * Assert that frame has monotonic frameId compared to previous frame
         * @param previousFrameJson Previous frame JSON string
         * @param currentFrameJson Current frame JSON string
         */
        fun assertFrameHasMonotonicFrameId(previousFrameJson: String?, currentFrameJson: String): Boolean {
            val currentState = parseInputState(currentFrameJson)
            return if (previousFrameJson != null) {
                val previousState = parseInputState(previousFrameJson)
                val result = currentState.frameId > previousState.frameId
                if (!result) {
                    println("FrameId is not monotonic: previous=${previousState.frameId}, current=${currentState.frameId}")
                }
                result
            } else {
                lastFrameId = currentState.frameId
                true
            }
        }

        /**
         * Extract frameId from InputState
         * @param state InputState
         * @return frameId
         */
        fun extractFrameId(state: InputState): Long {
            return state.frameId
        }

        /**
         * Extract frameId from JSON string
         * @param jsonString JSON string
         * @return frameId
         */
        fun extractFrameId(jsonString: String): Long {
            val state = parseInputState(jsonString)
            return state.frameId
        }

        /**
         * Extract runtimeStatus from InputState
         * @param state InputState
         * @return runtimeStatus
         */
        fun extractRuntimeStatus(state: InputState): String {
            return state.runtimeStatus
        }

        /**
         * Extract runtimeStatus from JSON string
         * @param jsonString JSON string
         * @return runtimeStatus
         */
        fun extractRuntimeStatus(jsonString: String): String {
            val state = parseInputState(jsonString)
            return state.runtimeStatus
        }
    }
}
