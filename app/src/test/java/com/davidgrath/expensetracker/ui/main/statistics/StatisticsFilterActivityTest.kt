package com.davidgrath.expensetracker.ui.main.statistics

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.TransactionBuilder
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class StatisticsFilterActivityTest {

    @Inject
    lateinit var expenseTrackerDatabase: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: TestExpenseTracker

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    fun givenProfileHasMultipleDistinctCurrenciesWhenAttemptToSelectAccountWithMoreThanOneThenFail() {
        val builder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
        builder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal.TEN)
            .commit()

        builder.createTransaction()
            .withNewAccount("USD", "Second")
            .withItem("Description", "miscellaneous", BigDecimal.TEN)
            .commit()

        builder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal.TEN)
            .withNewAccount("GBP", "British")

        //No ActivityScenarioRule because of pre-seeding
        val statisticsFilterActivity = ActivityScenario.launch(StatisticsFilterActivity::class.java)

        onView(withText(containsString("Default Account"))).perform(click())
        onView(withText(containsString("Second"))).perform(click())

        onView(withText(containsString("Default Account"))).check(matches(isChecked()))
        onView(withText(containsString("Second"))).check(matches(isChecked()))

        onView(withText(containsString("British"))).perform(click())
        onView(withText(containsString("Default Account"))).check(matches(isNotChecked()))
        onView(withText(containsString("Second"))).check(matches(isNotChecked()))
        onView(withText(containsString("British"))).check(matches(isChecked()))
    }
}