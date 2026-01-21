package com.linecat.wmmtcontroller.e2e.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON Assertions for E2E testing
 * Provides methods to assert JSON structure and content in WebSocket messages
 */
object JsonAssertions {

    /**
     * Assert that a JSON string contains a specific field with a specific value
     * @param jsonString The JSON string to assert
     * @param fieldName The field name to check
     * @param expectedValue The expected value of the field
     * @return True if the assertion passes, false otherwise
     */
    fun assertJsonField(jsonString: String, fieldName: String, expectedValue: Any): Boolean {
        try {
            val jsonObject = JSONObject(jsonString)
            return when (expectedValue) {
                is String -> jsonObject.getString(fieldName) == expectedValue
                is Int -> jsonObject.getInt(fieldName) == expectedValue
                is Long -> jsonObject.getLong(fieldName) == expectedValue
                is Double -> jsonObject.getDouble(fieldName) == expectedValue
                is Boolean -> jsonObject.getBoolean(fieldName) == expectedValue
                else -> false
            }
        } catch (e: JSONException) {
            return false
        }
    }

    /**
     * Assert that a JSON string contains a specific field with an empty value
     * @param jsonString The JSON string to assert
     * @param fieldName The field name to check
     * @return True if the assertion passes, false otherwise
     */
    fun assertJsonFieldEmpty(jsonString: String, fieldName: String): Boolean {
        try {
            val jsonObject = JSONObject(jsonString)
            val fieldValue = jsonObject.get(fieldName)
            return when (fieldValue) {
                is String -> fieldValue.isEmpty()
                is JSONArray -> fieldValue.length() == 0
                is JSONObject -> fieldValue.length() == 0
                else -> false
            }
        } catch (e: JSONException) {
            return false
        }
    }

    /**
     * Assert that a JSON string contains a specific field with a list that contains all expected values
     * @param jsonString The JSON string to assert
     * @param fieldName The field name to check
     * @param expectedValues The expected values in the list
     * @return True if the assertion passes, false otherwise
     */
    fun assertJsonFieldContainsValues(jsonString: String, fieldName: String, expectedValues: List<String>): Boolean {
        try {
            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray(fieldName)
            
            val actualValues = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                actualValues.add(jsonArray.getString(i))
            }
            
            return actualValues.containsAll(expectedValues)
        } catch (e: JSONException) {
            return false
        }
    }

    /**
     * Assert that a JSON string contains a specific field with an empty list
     * @param jsonString The JSON string to assert
     * @param fieldName The field name to check
     * @return True if the assertion passes, false otherwise
     */
    fun assertJsonFieldEmptyList(jsonString: String, fieldName: String): Boolean {
        try {
            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray(fieldName)
            return jsonArray.length() == 0
        } catch (e: JSONException) {
            return false
        }
    }

    /**
     * Assert that a WebSocket frame contains valid JSON with expected fields
     * @param wsMessage The WebSocket message to assert
     * @return True if the assertion passes, false otherwise
     */
    fun assertFrameJsonValid(wsMessage: String): Boolean {
        try {
            val jsonObject = JSONObject(wsMessage)
            
            // Check required fields
            jsonObject.getString("type")
            jsonObject.getString("version")
            jsonObject.getLong("frameId")
            
            // Check input state fields
            jsonObject.getJSONArray("keyboard")
            jsonObject.getJSONObject("mouse")
            jsonObject.getJSONObject("joystick")
            jsonObject.getJSONObject("gyroscope")
            
            return true
        } catch (e: JSONException) {
            return false
        }
    }

    /**
     * Assert that frameId is monotonically increasing
     * @param prevFrame Previous frame JSON string
     * @param nextFrame Next frame JSON string
     * @return True if the assertion passes, false otherwise
     */
    fun assertFrameHasMonotonicFrameId(prevFrame: String?, nextFrame: String): Boolean {
        try {
            if (prevFrame == null) {
                // First frame, just check frameId exists
                val nextJson = JSONObject(nextFrame)
                nextJson.getLong("frameId")
                return true
            }
            
            val prevJson = JSONObject(prevFrame)
            val nextJson = JSONObject(nextFrame)
            
            val prevFrameId = prevJson.getLong("frameId")
            val nextFrameId = nextJson.getLong("frameId")
            
            return nextFrameId > prevFrameId
        } catch (e: JSONException) {
            return false
        }
    }

    /**
     * Assert that heldKeys is empty in a WebSocket frame
     * @param wsMessage The WebSocket message to assert
     * @return True if the assertion passes, false otherwise
     */
    fun assertHeldKeysEmpty(wsMessage: String): Boolean {
        return assertJsonFieldEmptyList(wsMessage, "keyboard")
    }

    /**
     * Assert that an event sequence matches the expected prefix
     * @param events Actual events received
     * @param expectedPrefix Expected sequence of events
     * @return True if the assertion passes, false otherwise
     */
    fun assertEventSequence(events: List<String>, expectedPrefix: List<String>): Boolean {
        if (events.size < expectedPrefix.size) {
            return false
        }
        
        for (i in expectedPrefix.indices) {
            if (events[i] != expectedPrefix[i]) {
                return false
            }
        }
        
        return true
    }
}
