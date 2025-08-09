package com.davidgrath.expensetracker.repositories

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionRepositoryTest {

    @get:Rule
    val rule = ActivityScenarioRule(AddDetailedTransactionActivity::class.java)

    @Test
    fun givenDetailedTransactionInDraftWhenDoneThenDraftFileNoLongerExists() {
        val context = RuntimeEnvironment.getApplication()
        val folder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(folder, Constants.DRAFT_FILE_NAME)
        assertTrue(file.exists())
        //Enter valid input
        onView(withId(R.id.edit_text_add_detailed_transaction_item_amount)).perform(click(), replaceText("300.00"))
        onView(withId(R.id.edit_text_add_detailed_transaction_item_description)).perform(typeText("Description"))
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        assertFalse(file.exists())
    }
}