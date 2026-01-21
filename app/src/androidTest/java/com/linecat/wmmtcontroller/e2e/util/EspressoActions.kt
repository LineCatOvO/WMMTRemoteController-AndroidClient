package com.linecat.wmmtcontroller.e2e.util

import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import android.view.View

/**
 * Espresso Actions for E2E testing
 * Provides custom Espresso actions for UI testing
 */
object EspressoActions {

    /**
     * Wait for a specified amount of time
     * @param millis Time to wait in milliseconds
     * @return ViewAction that waits for the specified time
     */
    fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for $millis milliseconds"
            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
}
