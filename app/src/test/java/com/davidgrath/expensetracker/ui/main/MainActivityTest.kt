package com.davidgrath.expensetracker.ui.main

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.CategoryStringIdMatcher
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun recreateDialogTest() {
        onView(withId(R.id.fab_transactions)).perform(click())
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)
        onView(withId(R.id.edit_text_add_transaction_amount)).inRoot(isDialog()).perform(click(), replaceText("100.00"))
        onView(withId(R.id.edit_text_add_transaction_description)).inRoot(isDialog()).perform(typeText("Description"))
        onView(withId(R.id.image_view_add_transaction_debit_or_credit)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.spinner_add_transaction_category)).inRoot(isDialog()).perform(click())
        onData(allOf(CategoryStringIdMatcher("fitness"))).inAdapterView(
            withId(R.id.spinner_add_transaction_category)
        ).perform(click())
        rule.scenario.recreate()

        onView(withId(R.id.edit_text_add_transaction_amount)).inRoot(isDialog()).check(matches(withText("100")))
        onView(withId(R.id.edit_text_add_transaction_description)).inRoot(isDialog()).check(matches(withText("Description")))
        onView(withId(R.id.spinner_add_transaction_category)).inRoot(isDialog())
            .check(matches(withSpinnerText(Matchers.containsString("Fitness"))))
    }
}