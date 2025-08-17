package com.davidgrath.expensetracker.ui

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.TempStatisticsConfig
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.google.android.material.tabs.TabLayout
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsFragmentInstrumentedTest {
    @get:Rule
    val mainActivityScenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        onView(ViewMatchers.withId(R.id.tab_layout_main)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return Matchers.allOf(
                    ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    ViewMatchers.isDisplayed()
                )
            }

            override fun getDescription(): String {
                return "Clicking statistics tab"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val tabLayout = view as TabLayout
                tabLayout.getTabAt(1)!!.view.performClick()
            }
        })
    }

    @After
    fun tearDown() {
        mainActivityScenario.scenario.onActivity {
            it.viewModel.setConfig(TempStatisticsConfig())
        }
    }

    @Test
    fun givenModeIsDailyWhenClickConfigureThenDateDialogAppears() {
        onView(ViewMatchers.withId(R.id.spinner_statistics_current_mode))
            .perform(ViewActions.click())
        onData(Matchers.equalTo(TempStatisticsConfig.Mode.Daily))
            .perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.button_statistics_configure_current_mode))
            .perform(ViewActions.click())
    }

    @Test
    fun givenModeIsPastXDaysWhenClickConfigureThenNumberDialogShows() {
        onView(ViewMatchers.withId(R.id.spinner_statistics_current_mode))
            .perform(ViewActions.click())
        onData(Matchers.equalTo(TempStatisticsConfig.Mode.PastXDays))
            .perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.button_statistics_configure_current_mode))
            .perform(ViewActions.click())
    }


    @Test
    @Ignore("Not ready yet")
    fun givenRangeIsSelectedAndNoRangesSelectedBeforeThenDateRangeDialogAppears() {

    }
    @Test
    fun givenModeIsRangeWhenClickConfigureThenDateRangeDialogAppears() {
        onView(ViewMatchers.withId(R.id.spinner_statistics_current_mode))
            .perform(ViewActions.click())
        onData(Matchers.equalTo(TempStatisticsConfig.Mode.Range))
            .perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.button_statistics_configure_current_mode))
            .perform(ViewActions.click())
    }
}