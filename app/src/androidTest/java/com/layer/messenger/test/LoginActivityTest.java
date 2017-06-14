package com.layer.messenger.test;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.WindowManager;

import com.layer.messenger.LoginActivity;
import com.layer.messenger.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {

    private String mLoginEmail;
    private String mLoginPassword;

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(LoginActivity.class);

    /**
     * Wake up the device and try to unlock the screen. If there is a password/pattern lock on
     * the device, then the activity specified will get launched on top of the lock screen instead
     * of unlocking the device.
     */
    @Before
    public void setUp() {
        final LoginActivity activity = mActivityRule.getActivity();
        Runnable wakeUpDevice = new Runnable() {
            public void run() {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };
        activity.runOnUiThread(wakeUpDevice);

        mLoginEmail = "test@something.com";
        mLoginPassword = "some_password_!@#";
    }

    @Test
    public void loginActivityHasEmailField() {
        onView(withId(R.id.email)).perform(typeText(mLoginEmail), closeSoftKeyboard());
        onView(withId(R.id.password)).perform(typeText(mLoginPassword), closeSoftKeyboard());

        // Check if login text is set correctly
        onView(withId(R.id.email)).check(matches(withText(mLoginEmail)));

        // Check what password is displaying
        onView(withId(R.id.password)).check(matches(withText(mLoginPassword)));
    }
}