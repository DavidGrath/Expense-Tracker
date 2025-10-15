package com.davidgrath.expensetracker.ui.main.statistics

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsFilterActivityInstrumentedTest {

    lateinit var app: InstrumentedTestExpenseTracker
    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
    }

    @Test
    @Ignore("Next commit")
    fun whenChooseFilterThenFilterAppliedAndFileDeleted() {
        // Open Activity
        // Change things
        // Finish
        // Assert relevant LiveData updated - income total, expense total, transaction count, item count
        // Assert file deleted
    }

    @Test
    @Ignore("Next commit")
    fun whenCancelFilterThenFilterNotAppliedAndFileDeleted() {
        // Open Activity
        // Change things
        // Return/Back Button maybe?
        // Assert relevant LiveData not updated - income total, expense total, transaction count, item count
        // Assert file deleted
    }
}