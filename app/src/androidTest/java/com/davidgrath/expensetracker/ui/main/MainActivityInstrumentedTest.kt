package com.davidgrath.expensetracker.ui.main

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository

    @Before
    fun setUp() {
        (ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>().appComponent as InstrumentedTestComponent).inject(this)
    }

    @After
    fun tearDown() {
        transactionItemDao.deleteAll().blockingSubscribe()
        transactionDao.deleteAll().blockingSubscribe()
        addDetailedTransactionRepository.deleteDraft()
    }

    @Test
    fun givenFieldsAreValidWhenSubmitThenTransactionAdded() {
        onView(withId(R.id.fab_transactions)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).perform(click(), typeText("100.00"))
        onView(withId(R.id.edit_text_add_transaction_description)).perform(typeText("Basic Description"))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.text_view_transaction_item_description)).check(ViewAssertions.matches(ViewMatchers.withText("Basic Description")))
    }

    @Test
    fun givenAmountIsNotPositiveWhenSubmitThenFail() {
        onView(withId(R.id.fab_transactions)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).perform(click(),  typeText("0"))
        onView(withId(R.id.edit_text_add_transaction_description)).perform(typeText("Basic Description"))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).check(matches(ViewMatchers.hasErrorText("Invalid")))
    }
    @Test
    fun givenAmountIsEmptyWhenSubmitThenFail() {
        onView(withId(R.id.fab_transactions)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).perform(click(),  replaceText(""))
        onView(withId(R.id.edit_text_add_transaction_description)).perform(typeText("Basic Description"))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).check(matches(ViewMatchers.hasErrorText("Invalid")))
    }


    @Test
    fun givenDescriptionIsNotValidWhenSubmitThenFail() {
        onView(withId(R.id.fab_transactions)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_amount)).perform(click(),  replaceText("100.00"))
        onView(withId(R.id.edit_text_add_transaction_description)).perform(typeText(""))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_description)).check(matches(hasErrorText("Empty")))
        onView(withId(R.id.edit_text_add_transaction_description)).perform(typeText("\t  "))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.edit_text_add_transaction_description)).check(matches(hasErrorText("Empty")))
    }
}