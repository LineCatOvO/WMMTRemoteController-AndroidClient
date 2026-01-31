package com.linecat.wmmtcontroller.e2e.util;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import android.view.View;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

/**
 * Espresso Actions for E2E testing
 * Provides custom Espresso actions for UI testing
 */
public class EspressoActions {

    /**
     * Wait for a specified amount of time
     * @param millis Time to wait in milliseconds
     * @return ViewAction that waits for the specified time
     */
    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public android.view.ViewMatchers.Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}