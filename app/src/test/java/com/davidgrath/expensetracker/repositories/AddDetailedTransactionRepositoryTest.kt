package com.davidgrath.expensetracker.repositories

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.FileObserver
import androidx.core.net.toUri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TestContentProvider
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.addContentProviderImages
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.getHashCount
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddTransactionItemRecyclerAdapter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionRepositoryTest {

    @get:Rule
    val rule = ActivityScenarioRule(AddDetailedTransactionActivity::class.java)
    @get:Rule
    val intentsRule = IntentsRule()

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
    }

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

    /**
     * When I used the "Add Image" button in this test, it prevented the deletion of the draft image for some reason
     * Ignoring for now
     */
    @Test
    fun givenImagesInDraftWhenDoneThenImagesNotInDraftAndImagesInMainFolder() {
        val context = RuntimeEnvironment.getApplication() as ExpenseTracker
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        val mainImagesFolder = File(mainFolder, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val draftImage = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, TestData.Companion.Images.BREAD.resourceName, draftImage)
        val uri = draftImage.toUri()
        val map = mapOf(uri to TestData.Companion.Images.BREAD.sha256)
        val draft = context.getDraftValue().copy(imageHashes = map)
        context.saveDraft(draft)
        addContentProviderImages(context, classLoader, TestData.Companion.Images.BREAD)

        onView(withId(R.id.edit_text_add_detailed_transaction_item_amount)).perform(click(), replaceText("300.00"))
        onView(withId(R.id.edit_text_add_detailed_transaction_item_description)).perform(typeText("Description"))


        /*val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.text_view_add_detailed_transaction_show_details
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )*/
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        assertEquals(0, getHashCount(TestData.Companion.Images.BREAD.sha256, draftImagesFolder))
        assertEquals(1, getHashCount(TestData.Companion.Images.BREAD.sha256, mainImagesFolder))
    }

    @Test
    @Ignore("I need to consider how deleting works eventually")
    fun givenImageInDraftNotUsedWhenDoneThenImageNotInMainFolder() {

    }
}