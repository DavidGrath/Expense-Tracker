package com.davidgrath.expensetracker.ui.main.statistics

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class StatisticsFragmentInstrumentedTest {
    @get:Rule
    val mainActivityScenario = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: InstrumentedTestExpenseTracker

    @Before
    fun setUp() {
        onView(ViewMatchers.withId(R.id.tab_layout_main)).perform(TabLayoutItemClick(1))
        app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
        (app.appComponent as InstrumentedTestComponent).inject(this)
    }

    @After
    fun tearDown() {
        mainActivityScenario.scenario.onActivity {
            it.viewModel.setConfig(StatisticsConfig(timeAndLocaleHandler.getLocale()))
        }
    }

    @Test
    @Ignore("Not ready yet")
    fun givenModeIsDailyWhenClickConfigureThenDateDialogAppears() {
        onView(ViewMatchers.withId(R.id.spinner_statistics_current_mode))
            .perform(ViewActions.click())
        onData(Matchers.equalTo(StatisticsConfig.DateMode.Daily))
            .perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.image_view_statistics_configure_current_mode))
            .perform(ViewActions.click())
    }


    @Test
    @Ignore("Not ready yet")
    fun givenRangeIsSelectedAndNoRangesSelectedBeforeThenDateRangeDialogAppears() {

    }
}