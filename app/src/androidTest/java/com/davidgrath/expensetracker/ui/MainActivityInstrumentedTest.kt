package com.davidgrath.expensetracker.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun givenFieldsAreValidWhenSubmitThenTransactionAdded() {
        onView(withId(R.id.fab_main)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).perform(typeText("100.00"))
        onView(withId(R.id.edit_text_add_transaction_description)).perform(typeText("Basic Description"))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.text_view_transaction_item_description)).check(ViewAssertions.matches(ViewMatchers.withText("Basic Description")))
    }

    @Test
    @Ignore("Not ready yet")
    fun givenAmountIsNotPositiveOrEmptyWhenSubmitThenFail() {

    }


    @Test
    @Ignore("Not ready yet")
    fun givenDescriptionIsNotValidWhenSubmitThenFail() {

    }
}