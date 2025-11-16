package com.davidgrath.expensetracker.ui.main.statistics

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class StatisticsFilterActivityInstrumentedTest {

    lateinit var app: InstrumentedTestExpenseTracker
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var expenseTrackerDatabase: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
        (ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>().appComponent as InstrumentedTestComponent).inject(this)
        onView(withId(R.id.tab_layout_main)).perform(TabLayoutItemClick(1))
    }

    @Test
    fun whenChooseFilterThenFilterAppliedAndFileDeleted() {
        val dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
        dataBuilder.createBasicTransaction("Description", "miscellaneous", BigDecimal(100))

        dataBuilder.createTransaction()
            .withItem("Dumbells", "fitness", BigDecimal(200))
            .withNewAccount("USD", "Second")
            .commit()

        dataBuilder.createTransaction()
            .withNewAccount("GBP", "British")
            .withItem("Description 2", "miscellaneous", BigDecimal(400))
            .commit()

        // Open Activity
        onView(withId(R.id.image_view_statistics_open_filter)).perform(click())
        // Change things
        onView(withText(containsString("Default Account"))).perform(click())
        onView(withText(containsString("Second"))).perform(click())
        // Finish
        onView(withId(R.id.fab_statistics_filter_done)).perform(click())
        // Assert relevant LiveData updated - income total, expense total, transaction count, item count
        onView(withId(R.id.fragment_statistics)).check(matches(isDisplayed()))
        onView(withId(R.id.text_view_statistics_total_expenses)).check(matches(withText(Matchers.matchesRegex(".*\\b300\\b.*"))))
        onView(withId(R.id.text_view_statistics_transaction_count)).check(matches(withText(Matchers.matchesRegex(".*\\b2\\b.*")))) //TODO Maybe use ViewModel's LiveData.value instead
        onView(withId(R.id.text_view_statistics_item_count)).check(matches(withText(Matchers.matchesRegex(".*\\b2\\b.*"))))
        // Assert file deleted
        val file = File(app.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
        assertFalse(file.exists())
    }

    @Test
    fun whenCancelFilterThenFileDeleted() {

        // Open Activity
        onView(withId(R.id.image_view_statistics_open_filter)).perform(click())
        // Return/Back Button
        pressBack()
        onView(withId(R.id.fragment_statistics)).check(matches(isDisplayed()))
        // Assert file deleted
        val file = File(app.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
        assertFalse(file.exists())
    }
}