package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.RecyclerClickItemAction
import com.davidgrath.expensetracker.TestContentProvider
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.UriTypeAdapter
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File


@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionActivityTest {

    @get:Rule
    val intentsRule = IntentsRule()

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        draftFolder.deleteRecursively()
    }

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
                it.putExtra(
                    AddDetailedTransactionActivity.ARG_INITIAL_DESCRIPTION,
                    basicDescription
                )
            }
            val addDetailedTransactionActivityScenario =
                ActivityScenario.launch<AddDetailedTransactionActivity>(intent)
        }

        onView(withId(R.id.edit_text_add_detailed_transaction_item_amount)).check(
            matches(
                withText(
                    basicAmount
                )
            )
        )
        onView(withId(R.id.edit_text_add_detailed_transaction_item_description)).check(
            matches(
                withText(basicDescription)
            )
        )
        onView(withId(R.id.spinner_add_detailed_transaction_item_category)).check(
            matches(
                withSpinnerText("Fitness")
            )
        )
    }

    @Test
    @Ignore("Not ready yet")
    fun givenIntentHasArgsAndDraftExistsWhenStartActivityThenArgsMovedToTopOfList() {

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
    fun givenUserSelectsSameImageMultipleTimesForSameItemThenImageOnlyAddedOnce() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }

        //Open Image from system

        //This doesn't need to happen because the sub-views aren't bound by the constraints and so effectiveVisibility GONE doesn't make a difference. Maybe fix
//        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )

        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .check { view, noViewFoundException ->
                val v = view as RecyclerView
                val subRecyclerViewHolder =
                    v.findViewHolderForAdapterPosition(0) as AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder
                val count =
                    (subRecyclerViewHolder.binding.recyclerviewAddDetailedTransactionItemImages.adapter as AddTransactionItemImagesRecyclerAdapter).itemCount
                assertEquals(1, count)
            }
        //Open same image
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        //TODO Rely on repository instead of draft file
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val draft = context.addDetailedTransactionRepository().getDraftValue()
        val images = draft.items[0].images
        assertEquals(1, images.size)
    }

    @Test
    @Ignore("Not ready yet")
    fun givenUserSelectsSameImageThatSomehowHasDifferentUrisThenImageOnlyAddedOnce() {
        val duplicateBread = TestData.Companion.Images.BREAD.copy(fileName = "bread_duplicate.jpg")
        addImages(TestData.Companion.Images.BREAD, duplicateBread)
        val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }
        val duplicateBreadIntent = Intent().also {
            it.data = duplicateBread.uri
        }
        clickRecyclerViewItem<AddTransactionItemImagesRecyclerAdapter.AddTransactionItemImagesViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                duplicateBreadIntent
            )
        )
        //TODO Rely on repository instead of draft file
        val gson = Gson()
        val draftFile = File(
            File(
                ApplicationProvider.getApplicationContext<ExpenseTracker>().filesDir,
                Constants.FOLDER_NAME_DRAFT
            ), Constants.DRAFT_FILE_NAME
        )
        val reader = draftFile.reader()
        val draft = gson.fromJson(reader, AddDetailedTransactionDraft::class.java)
        val images = draft.items[0].images
        assertEquals(1, images.size)
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

    fun addImages(vararg images: TestData.Companion.Images) {
        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val temp = File(app.filesDir, "temp")
        for (image in images) {
            val resourceInputStream =
                AddDetailedTransactionActivityTest::class.java.classLoader.getResourceAsStream(image.resourceName)
            val file = File(temp, image.resourceName)
            val outputStream = file.outputStream()
            resourceInputStream.copyTo(outputStream)
            resourceInputStream.close()
            outputStream.close()
        }
    }
}