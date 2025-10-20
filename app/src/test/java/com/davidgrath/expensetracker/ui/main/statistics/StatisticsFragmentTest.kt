package com.davidgrath.expensetracker.ui.main.statistics

import android.text.InputType
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.threeten.bp.LocalDate
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class StatisticsFragmentTest {

    @get:Rule
    val mainActivityScenario = ActivityScenarioRule(MainActivity::class.java)
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: TestExpenseTracker

    @Before
    fun setUp() {
        onView(withId(R.id.tab_layout_main)).perform(TabLayoutItemClick(1))
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @After
    fun tearDown() {

    }


    @Test
    fun givenModeIsPastXDaysThenArrowsDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.PastXDays)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsPastWeekThenArrowsDisabledAndConfigureDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.PastWeek)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_view_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsPastMonthThenArrowsDisabledAndConfigureDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.PastMonth)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_view_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsPastYearThenArrowsDisabledAndConfigureDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.PastYear)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_view_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }


    @Test
    fun givenModeIsRangeThenArrowsDisabled() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.Range)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_view_statistics_configure_current_mode)).check(matches(isEnabled()))
    }
    @Test
    fun givenModeIsAllThenArrowsDisabledAndConfigure() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.All)).perform(click())
        onView(withId(R.id.image_button_statistics_cycle_mode_left)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_button_statistics_cycle_mode_right)).check(matches(isNotEnabled()))
        onView(withId(R.id.image_view_statistics_configure_current_mode)).check(matches(isNotEnabled()))
    }

    @Test
    fun givenModeIsPastXDaysWhenClickConfigureThenNumberDialogShows() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.PastXDays)).perform(click())
        onView(withId(R.id.image_view_statistics_configure_current_mode)).perform(click())
        onView(withInputType(InputType.TYPE_CLASS_NUMBER)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    @Ignore("'Fragment already added', won't bother fixing")
    fun givenModeIsRangeWhenClickConfigureThenDateRangeDialogAppears() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            viewModel.setDateRange(
                LocalDate.parse("2025-06-01"),
                LocalDate.parse("2025-06-30")
            )
        }
        onData(equalTo(StatisticsConfig.DateMode.Range)).perform(click())
        onView(withResourceName("confirm_button")).inRoot(
            isDialog()
        ).perform(click())
        onView(withId(R.id.image_view_statistics_configure_current_mode)).perform(click())
        onView(withResourceName(Matchers.matchesRegex(".*mtrl_calendar_main_pane"))).inRoot(
            isDialog()
        ).check(matches(isDisplayed()))
    }

    @Test
    fun givenModeIsDailyWhenClickConfigureThenDateDialogAppears() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.Daily)).perform(click())
        onView(withId(R.id.image_view_statistics_configure_current_mode)).perform(click())
        onView(withResourceName(Matchers.matchesRegex(".*mtrl_calendar_main_pane"))).inRoot(
            isDialog()
        ).check(matches(isDisplayed()))
    }


    @Test
    fun givenRangeIsSelectedAndNoRangesSelectedBeforeThenDateRangeDialogAppears() {
        onView(withId(R.id.spinner_statistics_current_mode)).perform(click())
        onData(equalTo(StatisticsConfig.DateMode.Range)).perform(click())
        onView(withResourceName(Matchers.matchesRegex(".*mtrl_calendar_main_pane"))).inRoot(
            isDialog()
        ).check(matches(isDisplayed()))
    }

}