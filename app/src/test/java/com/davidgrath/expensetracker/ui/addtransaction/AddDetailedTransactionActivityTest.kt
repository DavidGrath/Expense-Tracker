package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.davidgrath.expensetracker.CategoryStringIdMatcher
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TestConstants
import com.davidgrath.expensetracker.test.TestContentProvider
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.assertRecyclerViewItemSpinnerText
import com.davidgrath.expensetracker.assertRecyclerViewItemText
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.getHashCount
import com.davidgrath.expensetracker.inputNumberRecyclerViewItem
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.typeTextRecyclerViewItem
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.squareup.rx3.idler.Rx3Idler
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.CoreMatchers.allOf
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.math.BigDecimal


@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionActivityTest {

    @get:Rule
    val intentsRule = IntentsRule()
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var imageDao: ImageDao

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            RxJavaPlugins.setInitIoSchedulerHandler(Rx3Idler.create("Robolectric Rx3 Handler"))
        }

        /**
         * Rx Idler doesn't seem to work as expected, so I'll fall back to this when necessary
         */
        const val SLEEP_DURATION = 1_100L
    }

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        draftFolder.deleteRecursively()
        //TODO Make equivalent version using delete function
        val contentFolder = File(context.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER)
        contentFolder.deleteRecursively()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        mainFolder.deleteRecursively()
    }

    @Test
    fun givenIntentHasArgsWhenStartActivityThenDetailsArePopulated() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        val category = categoryRepository.findByStringId("fitness").subscribeOn(Schedulers.io()).blockingGet()!!
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

        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, basicAmount
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description, basicDescription
        )
        assertRecyclerViewItemSpinnerText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.spinner_add_detailed_transaction_item_category, "Fitness"
        )
    }

    @Test
    fun givenIntentHasArgsAndDraftExistsWhenStartActivityThenArgsMovedToTopOfList() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val category = categoryRepository.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val draft = buildDraft(BigDecimal.TEN, "Candy", categoryDbToCategoryUi(category))
        val fileHandler: DraftFileHandler = context
        addDetailedTransactionRepository.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft)
        //Start on MainActivity
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        val dumbbellCategory = categoryRepository.findByStringId("fitness").subscribeOn(Schedulers.io()).blockingGet()!!
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

    /**
     * This test is flaky for some reason. Ignoring it
     */
    @Test
    @Ignore("Flaky, likely because of the Snackbar")
    fun givenAnyAmountNonPositiveOrEmptyWhenDoneThenFail() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        //More than one item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(scrollTo(), click())
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(scrollTo(), click())

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "100.00"
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Description"
        )

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "0.00"
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Description"
        )

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_amount,
            ""
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Description"
        )
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        onView(withText("Invalid input")).check(matches(isDisplayed()))
    }

    @Test
    fun givenAnyDescriptionEmptyWhenDoneThenFail() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        //More than one item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(scrollTo(), click())

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "100.00"
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Description"
        )

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "200.00"
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_description,
            ""
        )
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        onView(withText("Invalid input")).check(matches(isDisplayed()))
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
    @Ignore("Next commit")
    fun givenEvidenceThresholdLimitReachedWhenAddMoreThenFail() {
    }

    @Test
    @Ignore("Not ready yet")
    fun givenImageThresholdReachedWhenAddMoreThenFail() {
        //TODO get at least 10 free stock images for this
    }

    @Test
    @Ignore("When I get to general number formatting")
    fun twoDecimalPlacesTest() {
        //TODO Locale,DecimalFormat/NumberFormat, probably inject
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "100.006"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, "100.01"
        )
    }

    @Test
    fun givenDetailedTransactionInDraftWhenStartActivityThenTransactionEditRestored() {

        val basicAmount = "100.00"
        val basicDescription = "Description"

        //Open Add Detailed Transaction Screen
        var addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        //Add an Item with all details
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            basicAmount
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description,
            basicDescription
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.spinner_add_detailed_transaction_item_category
        )
        onData(allOf(CategoryStringIdMatcher("fitness"))).perform(click())
        //Close App
        //Open App

        addDetailedTransactionActivityScenario.onActivity { it.viewModelStore.clear() }
        addDetailedTransactionActivityScenario.moveToState(Lifecycle.State.DESTROYED)
        addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        //Assert Screen is present with same details

        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, basicAmount
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description, basicDescription
        )
        assertRecyclerViewItemSpinnerText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.spinner_add_detailed_transaction_item_category, "Fitness"
        )
        //This might end up becoming a toggleable preference like: "restoreDraftTransactionWhenReopenApplication"
    }

    @Test
    fun givenUserSelectsSameImageMultipleTimesForSameItemThenImageOnlyAddedOnce() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.BREAD.uri
        }
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResources(context, AddDetailedTransactionActivityTest::class.java.classLoader, TestData.Resource.Images.BREAD)

        //Open Image from system

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

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
        Thread.sleep(SLEEP_DURATION)

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
        Thread.sleep(SLEEP_DURATION)
        val draft = addDetailedTransactionRepository.getDraftValue()
        val images = draft.items[0].images
        assertEquals(1, images.size)
    }

    @Test
    fun givenUserSelectsSameImageThatSomehowHasDifferentUrisThenImageOnlyAddedOnce() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val duplicateBread = TestData.Resource.Images.BREAD.fileName("bread_duplicate.jpg")
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResources(context, AddDetailedTransactionActivityTest::class.java.classLoader, TestData.Resource.Images.BREAD, duplicateBread)
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.BREAD.uri
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
            R.id.text_view_add_detailed_transaction_show_details
        )

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        Thread.sleep(SLEEP_DURATION)
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
        Thread.sleep(SLEEP_DURATION)
        val draft = addDetailedTransactionRepository.getDraftValue()
        val images = draft.items[0].images
        assertEquals(1, images.size)
    }


    @Test
    fun givenUserSelectsSameImageMultipleTimesAcrossMultipleItemsThenImageOnlyCopiedOnceToDraftStorage() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResources(context, AddDetailedTransactionActivityTest::class.java.classLoader, TestData.Resource.Images.BREAD)
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.BREAD.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)
        //Open Image from system
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        Thread.sleep(SLEEP_DURATION)
        //Add new item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(scrollTo(), click())
        //Open same at different position
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        Thread.sleep(SLEEP_DURATION)
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val imagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)

        assertEquals(1, getHashCount(TestData.Resource.Images.BREAD.sha256, imagesFolder).blockingGet())
    }


    @Test
    fun givenUserSelectsImageThatHasBeenSavedToMainStorageWhenSelectImageThenImageNotCopiedToDraftStorage() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        addContentProviderResources(context, classLoader, TestData.Resource.Images.BREAD)
        //Copy Image to DB
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        val mainImagesFolder = File(mainFolder, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val existingImage = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, TestData.Resource.Images.BREAD.resourceName, existingImage)
        imageDao.insertImage(ImageDb(null, 0, TestData.Resource.Images.BREAD.sha256, "image/jpeg", existingImage.toUri().toString(), "2025-08-09T12:08:00", "-04:00", "America/New_York"))
            .subscribeOn(Schedulers.io())
            .blockingGet()

        //Add same item
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.BREAD.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
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
        )
        Thread.sleep(SLEEP_DURATION)
        //Assert no new files, or assert no existing files match same hash

        val draft = addDetailedTransactionRepository.getDraftValue()
        assertEquals(0, getHashCount(TestData.Resource.Images.BREAD.sha256, draftImagesFolder).blockingGet())
        assertEquals(1, draft.items[0].images.size)
    }

    @Test
    fun givenExternalImageWasModifiedAndOriginalImageAddedToDraftWhenAddImageThenNewImageExistsInDraftStorage() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        val bread = TestData.Resource.Images.BREAD
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        addContentProviderResources(context, classLoader, bread)
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.BREAD.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
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
        )
        Thread.sleep(SLEEP_DURATION)

        val modifiedBread = bread.copy(resourceName = TestData.Resource.Images.TOOTHBRUSH.resourceName)
        addContentProviderResources(context, classLoader, modifiedBread)
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
        Thread.sleep(SLEEP_DURATION)
        val draft = addDetailedTransactionRepository.getDraftValue()
        val images = draft.items[0].images
        assertEquals(2, images.size)
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val imagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        assertEquals(1, getHashCount(bread.sha256, imagesFolder).blockingGet())
        assertEquals(1, getHashCount(TestData.Resource.Images.TOOTHBRUSH.sha256, imagesFolder).blockingGet())
    }

    @Test
    @Ignore("Not ready yet")
    fun givenCustomCategoryModifiedWhenRestoreDraftThenDraftCategoryCorrect() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenCustomCategoryDeletedWhenRestoreDraftThenCategoryBecomesMisc() {

    }

    fun buildDraft(amount: BigDecimal, description: String, category: CategoryUi): AddDetailedTransactionDraft {
        return AddDetailedTransactionDraft(items = listOf(AddTransactionItem(0, category, amount, description)))
    }
}