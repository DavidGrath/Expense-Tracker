package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TestConstants
import com.davidgrath.expensetracker.TestContentProvider
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.addContentProviderImages
import com.davidgrath.expensetracker.assertRecyclerViewItemSpinnerText
import com.davidgrath.expensetracker.assertRecyclerViewItemText
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.ui.main.MainActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.math.BigDecimal


@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionActivityTest {

    @get:Rule
    val intentsRule = IntentsRule()

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        draftFolder.deleteRecursively()
        val contentFolder = File(context.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER)
        contentFolder.deleteRecursively()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        mainFolder.deleteRecursively()
    }

    @Test
    fun givenIntentHasArgsWhenStartActivityThenDetailsArePopulated() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val categoryRepository = context.categoryRepository()
        val category = categoryRepository.findByStringId("fitness").blockingGet()!!
        val basicAmount = "300.00"
        val basicDescription = "Description"
        val categoryId = category.id!!
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
                withSpinnerText(Matchers.containsString("Fitness"))
            )
        )
    }

    @Test
    fun givenIntentHasArgsAndDraftExistsWhenStartActivityThenArgsMovedToTopOfList() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val categoryRepository = context.categoryRepository()
        val category = categoryRepository.findByStringId("food").blockingGet()!!
        val draft = buildDraft(BigDecimal.TEN, "Candy", categoryDbToCategoryUi(category))
        val fileHandler: DraftFileHandler = context
        fileHandler.createDraft()
        fileHandler.saveDraft(draft)
        //Start on MainActivity
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        val dumbbellCategory = categoryRepository.findByStringId("fitness").blockingGet()!!
        val basicAmount = "300.00"
        val basicDescription = "Dumbbells"
        val categoryId = dumbbellCategory.id!!
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
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, "300.00"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description, "Dumbbells"
        )
        assertRecyclerViewItemSpinnerText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.spinner_add_detailed_transaction_item_category, "Fitness"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount, "10.00"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_description, "Candy"
        )
        assertRecyclerViewItemSpinnerText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.spinner_add_detailed_transaction_item_category, "Food"
        )

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
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderImages(context, AddDetailedTransactionActivityTest::class.java.classLoader, TestData.Companion.Images.BREAD)

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
        val draft = context.addDetailedTransactionRepository().getDraftValue()
        val images = draft.items[0].images
        assertEquals(1, images.size)
    }

    @Test
    fun givenUserSelectsSameImageThatSomehowHasDifferentUrisThenImageOnlyAddedOnce() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val duplicateBread = TestData.Companion.Images.BREAD.fileName("bread_duplicate.jpg")
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderImages(context, AddDetailedTransactionActivityTest::class.java.classLoader, TestData.Companion.Images.BREAD, duplicateBread)
        val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }
        val duplicateBreadIntent = Intent().also {
            it.data = duplicateBread.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                duplicateBreadIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        val draft = context.addDetailedTransactionRepository().getDraftValue()
        val images = draft.items[0].images
        assertEquals(1, images.size)
    }


    @Test
    @Config(qualifiers = "h720dp")
    fun givenUserSelectsSameImageMultipleTimesAcrossMultipleItemsThenImageOnlyCopiedOnceToDraftStorage() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderImages(context, AddDetailedTransactionActivityTest::class.java.classLoader, TestData.Companion.Images.BREAD)
        val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        //Open Image from system
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        //Add new item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(click())
        //Open same at different position
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val imagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)

        assertEquals(1, getHashCount(TestData.Companion.Images.BREAD.sha256, imagesFolder))
    }


    @Test
    fun givenUserSelectsImageThatHasBeenSavedToMainStorageWhenSelectImageThenImageNotCopiedToDraftStorage() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        addContentProviderImages(context, classLoader, TestData.Companion.Images.BREAD)
        //Copy Image to DB
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        val mainImagesFolder = File(mainFolder, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val existingImage = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val resourceInputStream = classLoader.getResourceAsStream(TestData.Companion.Images.BREAD.resourceName)
        val outputStream = existingImage.outputStream()
        resourceInputStream.copyTo(outputStream)
        resourceInputStream.close()
        outputStream.close()
        context.imagesDao().addImage(ImageDb(null, 0, TestData.Companion.Images.BREAD.sha256, "image/jpeg", existingImage.toUri().toString(), "2025-08-09T12:08:00", "-04:00", "America/New_York")).blockingGet()

        //Add same item
        val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )

        //Assert no new files, or assert no existing files match same hash, use deleteOnExit maybe?

        val draft = context.addDetailedTransactionRepository().getDraftValue()
        assertEquals(0, getHashCount(TestData.Companion.Images.BREAD.sha256, draftImagesFolder))
        assertEquals(1, draft.items[0].images.size)
    }

    @Test
    fun givenExternalImageWasModifiedAndOriginalImageAddedToDraftWhenAddImageThenNewImageExistsInDraftStorage() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val bread = TestData.Companion.Images.BREAD
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        addContentProviderImages(context, classLoader, bread)
        val returnIntent = Intent().also {
            it.data = TestData.Companion.Images.BREAD.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        val modifiedBread = bread.copy(resourceName = TestData.Companion.Images.TOOTHBRUSH.resourceName)
        addContentProviderImages(context, classLoader, modifiedBread)
        val modifiedBreadIntent = Intent().also {
            it.data = modifiedBread.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                modifiedBreadIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        val draft = context.addDetailedTransactionRepository().getDraftValue()
        val images = draft.items[0].images
        assertEquals(2, images.size)
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val imagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        assertEquals(1, getHashCount(bread.sha256, imagesFolder))
        assertEquals(1, getHashCount(TestData.Companion.Images.TOOTHBRUSH.sha256, imagesFolder))
    }

    @Test
    @Ignore("Not ready yet")
    fun givenCustomCategoryModifiedWhenRestoreDraftThenDraftCategoryCorrect() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenCustomCategoryDeletedWhenRestoreDraftThenCategoryBecomesMisc() {

    }

    fun getHashCount(sha256: String, folder: File): Int {
        if(!folder.exists() || !folder.isDirectory) {
            return 0
        }
        var hashCount = 0
        for(f in folder.listFiles()) {
            val inputStream = f.inputStream()
            if(getSha256(inputStream) == sha256) {
                hashCount++
            }
        }
        return hashCount
    }

    fun buildDraft(amount: BigDecimal, description: String, category: CategoryUi): AddDetailedTransactionDraft {
        return AddDetailedTransactionDraft(items = listOf(AddTransactionItem(0, category, amount, description)))
    }
}