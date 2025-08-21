package com.davidgrath.expensetracker.ui.main.statistics

import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.entities.ui.TempStatisticsConfig
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.google.android.material.tabs.TabLayout
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatisticsFragmentTest {

    @get:Rule
    val mainActivityScenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        onView(withId(R.id.tab_layout_main)).perform(TabLayoutItemClick(1))
    }

    @After
    fun tearDown() {
        mainActivityScenario.scenario.onActivity {
            it.viewModel.setConfig(TempStatisticsConfig())
        }
    }


    @Test
    fun givenModeIsPastXDaysThenArrowsDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(TempStatisticsConfig.Mode.PastXDays)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsPastWeekThenArrowsDisabledAndConfigureDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(TempStatisticsConfig.Mode.PastWeek)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.button_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsPastMonthThenArrowsDisabledAndConfigureDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(TempStatisticsConfig.Mode.PastMonth)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.button_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsPastYearThenArrowsDisabledAndConfigureDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(TempStatisticsConfig.Mode.PastYear)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.button_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsRangeThenArrowsDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(TempStatisticsConfig.Mode.Range)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.button_statistics_configure_current_mode)).check(matches(isEnabled()))
    }


}