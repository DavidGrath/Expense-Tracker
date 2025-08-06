package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Ignore
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionActivityTest {
    @Test
    @Ignore("Not ready yet")
    fun givenIntentHasArgsWhenStartActivityThenDetailsArePopulated() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        val basicAmount = "300.00"
        val basicDescription = "Description"
        val categoryId = "fitness"
        mainActivityScenario.onActivity {
            val intent = Intent(it, AddDetailedTransactionActivity::class.java).also {
                it.putExtra(AddDetailedTransactionActivity.ARG_INITIAL_CATEGORY_ID, categoryId)
                it.putExtra(AddDetailedTransactionActivity.ARG_INITIAL_AMOUNT, basicAmount)
                it.putExtra(AddDetailedTransactionActivity.ARG_INITIAL_DESCRIPTION, basicDescription)
            }
            val addDetailedTransactionActivityScenario = ActivityScenario.launch<AddDetailedTransactionActivity>(intent)
        }

        onView(withId(R.id.edit_text_add_detailed_transaction_item_amount)).check(matches(withText(basicAmount)))
        onView(withId(R.id.edit_text_add_detailed_transaction_item_description)).check(matches(
            withText(basicDescription)
        ))
        onView(withId(R.id.spinner_add_detailed_transaction_item_category)).check(matches(
            withSpinnerText("Fitness")
        ))
    }



    @Test
    @Ignore("Not ready yet")
    fun givenAnyAmountNonPositiveOrEmptyWhenDoneThenFail() {
        //More than one item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(click())
    }

    @Test
    @Ignore("Not ready yet")
    fun givenAnyDescriptionEmptyWhenDoneThenFail() {
        //More than one item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(click())
    }

    @Test
    @Ignore("Not ready yet")
    //Should be impossible with UI restrictions but I'm paranoid
    fun givenUseCustomTimestampAndTimestampInFutureWhenSubmitThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenUseSellerAndSellerNotSuppliedWhenSubmitThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenEvidenceThresholdLimitReachedWhenAddMoreThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenImageThresholdReachedWhenAddMoreThenFail() {
        //TODO get at least 10 free stock images for this
    }

    @Test
    @Ignore("Not ready yet")
    fun twoDecimalPlacesTest() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenDetailedTransactionInDraftWhenStartActivityThenTransactionEditRestored() {
        //Open Add Detailed Transaction Screen
        //Add An Item with all details
        //Close App
        //Open App
        //Assert Screen is present with same details
        //This might end up becoming a toggleable preference like: "restoreDraftTransactionWhenReopenApplication"
    }

    @Test
    @Ignore("Not ready yet")
    fun givenUserSelectsSameImageMultipleTimesForSameItemThenImageOnlyAddedOnce() {
        //Open Image from system
        //Open same image
    }


    @Test
    @Ignore("Not ready yet")
    fun givenUserSelectsSameImageMultipleTimesAcrossMultipleItemsThenImageOnlyCopiedOnceToInternalStorage() {
        //Open Image from system
        //Add new item
        //Open same
    }


    @Test
    @Ignore("Not ready yet")
    fun givenUserSelectsImageThatHasBeenSavedToMainStorageWhenSelectImageThenImageNotCopiedToInternalStorage() {
        //Copy Image to DB
        //Add same item
        //Assert no new files, or assert no existing files match same hash, use deleteOnExit maybe?
    }

    @Test
    @Ignore("Not ready yet")
    fun givenExternalImageWasModifiedAndOriginalImageAddedToDraftWhenAddImageThenNewImageExistsInInternalStorage() {

    }

}