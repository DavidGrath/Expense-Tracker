package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.davidgrath.expensetracker.CategoryStringIdMatcher
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestConstants
import com.davidgrath.expensetracker.test.TestContentProvider
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.assertRecyclerViewItemSpinnerText
import com.davidgrath.expensetracker.assertRecyclerViewItemText
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.clearTextRecyclerViewItem
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TestTimeAndLocaleHandler
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.ui.AddEditDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.inputNumberRecyclerViewItem
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.typeTextRecyclerViewItem
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.squareup.rx3.idler.Rx3Idler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.util.Locale


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
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var transactionItemRepository: TransactionItemRepository
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: TestExpenseTracker
    var scenario: ActivityScenario<*>? = null

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


        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionActivityTest::class.java)
    }

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
        scenario = null
    }

    @After
    fun tearDown() {
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        draftFolder.deleteRecursively()
        //TODO Make equivalent version using delete function
        val contentFolder = File(app.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER)
        contentFolder.deleteRecursively()
        val mainFolder = File(app.filesDir, Constants.FOLDER_NAME_DATA)
        mainFolder.deleteRecursively()
        // https://github.com/robolectric/robolectric/issues/5131#issuecomment-505819342
        // Free up memory so the Bitmap load doesn't OOM
        scenario?.close()
    }

    @Test
    fun givenIntentHasArgsWhenStartActivityThenDetailsArePopulated() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        scenario = mainActivityScenario
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryRepository.findByProfileIdAndStringId(profile.id!!, "fitness").subscribeOn(Schedulers.io()).blockingGet()!!
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
            R.id.edit_text_add_detailed_transaction_item_amount, "300"
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
        val draft = buildDraft(BigDecimal.TEN, "Candy", "food")
        val fileHandler: DraftFileHandler = app
        addDetailedTransactionRepository.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).subscribe()
        //Start on MainActivity
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        scenario = mainActivityScenario
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val dumbbellCategory = categoryRepository.findByProfileIdAndStringId(profile.id!!, "fitness").subscribeOn(Schedulers.io()).blockingGet()!!
        val basicAmount = "300.00"
        val basicDescription = "Dumbbells"
        val categoryId = dumbbellCategory.id!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        mainActivityScenario.onActivity {
            val intent = Intent(it, AddDetailedTransactionActivity::class.java).also {
                it.putExtra(AddDetailedTransactionActivity.ARG_INITIAL_ACCOUNT_ID, accountId)
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
            R.id.edit_text_add_detailed_transaction_item_amount, "300"
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
            R.id.edit_text_add_detailed_transaction_item_amount, "10"
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
        scenario = addDetailedTransactionActivityScenario
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
        scenario = addDetailedTransactionActivityScenario
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

    @Test //TODO I could also possibly test this at the repository level, but we ignore that for now
    fun givenImageThresholdReachedWhenAddMoreThenFail() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        val resources = arrayOf(TestData.Resource.Images.BREAD,
            TestData.Resource.Images.DUMBBELLS_1,
            TestData.Resource.Images.HAMMER,
            TestData.Resource.Images.DUMBBELLS_3,
            TestData.Resource.Images.TOOTHBRUSH,
            TestData.Resource.Images.WALL_CLOCK,
            TestData.Resource.Images.SHIRT,
            TestData.Resource.Images.TRAMPOLINE,
            TestData.Resource.Images.LOTION,
            TestData.Resource.Images.DUMBBELLS_2
        )
        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            TestData.Resource.Images.BREAD,
            TestData.Resource.Images.DUMBBELLS_1,
            TestData.Resource.Images.DUMBBELLS_2,
            TestData.Resource.Images.DUMBBELLS_3,
            TestData.Resource.Images.TOOTHBRUSH,
            TestData.Resource.Images.WALL_CLOCK,
            TestData.Resource.Images.SHIRT,
            TestData.Resource.Images.TRAMPOLINE,
            TestData.Resource.Images.LOTION,
            TestData.Resource.Images.HAMMER
            )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        val draftImages = resources.slice(0..8) .map { r ->
            val f = File(draftImagesFolder, r.fileName)
            copyResourceToFile(classLoader, r.resourceName, f)
            AddEditTransactionFile(null, f.toUri(), "image/jpeg", r.sha256, 0L)
        }
        val draft = addDetailedTransactionRepository.getDraftValue()
        val item = draft.items[0].copy(images = draftImages)
        addDetailedTransactionRepository.changeItemInvalidate(0, item)
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val imageBelow1MiB = TestData.Resource.Images.DUMBBELLS_2
        val returnIntent = Intent().also {
            it.data = imageBelow1MiB.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)


        onView(withId(R.id.image_view_add_detailed_transaction_item_add_image)).check(matches(ViewMatchers.isNotEnabled()))
    }

    @Test
    fun numberFormatTest() {

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "1234.567"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, "1,234.56"
        )
        clearTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )

        //There is a wide variety of locales, so just testing for one that will be different from the current
        timeAndLocaleHandler.changeLocale(Locale("de", "DE"))
        addDetailedTransactionActivityScenario.recreate()


        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "1000,017"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, "1.000,01"
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
        

        //TODO Add Image and Evidence
        //Close App
        addDetailedTransactionActivityScenario.onActivity { it.viewModelStore.clear() }
        addDetailedTransactionActivityScenario.moveToState(Lifecycle.State.DESTROYED)

        //Open App
        addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        //Assert Screen is present with same details

        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, "100"
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
    fun givenDraftExistsWhenExistingTransactionOpenedForEditThenDraftNotLoaded() {
        //Existing draft
        val draft = buildDraft(BigDecimal(20), "Candy", "food")
        val fileHandler: DraftFileHandler = app
        addDetailedTransactionRepository.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).subscribe()
        //Existing item
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val transaction = TestBuilder.defaultTransaction(accountId, BigDecimal.TEN)
        val id = transactionRepository.addTransaction(transaction).blockingGet()
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryRepository.findByProfileIdAndStringId(profile.id!!, "fitness").subscribeOn(Schedulers.io()).blockingGet()!!
        val transactionItem = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal.TEN, category.id!!).description("Dumbbells").build()
        transactionItemRepository.addTransactionItem(transactionItem).blockingGet()

        val intent = Intent(app, AddDetailedTransactionActivity::class.java).also {
            it.putExtra(AddDetailedTransactionActivity.ARG_MODE, "edit")
            it.putExtra(AddDetailedTransactionActivity.ARG_EDIT_TRANSACTION_ID, id)
        }
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch<AddDetailedTransactionActivity>(intent)
        scenario = addDetailedTransactionActivityScenario

        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount, "10"
        )
        assertRecyclerViewItemText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description, transactionItem.description
        )
        assertRecyclerViewItemSpinnerText<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.spinner_add_detailed_transaction_item_category, "Fitness"
        )
    }

    @Test
    fun givenNoDraftExistsWhenExistingTransactionOpenedForEditThenDraftFileNotCreated() {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val transaction = TestBuilder.defaultTransaction(accountId, BigDecimal.TEN)
        val id = transactionRepository.addTransaction(transaction).blockingGet()
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryRepository.findByProfileIdAndStringId(profile.id!!, "fitness").subscribeOn(Schedulers.io()).blockingGet()!!
        val transactionItem = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal.TEN, category.id!!).description("Dumbbells").build()
        transactionItemRepository.addTransactionItem(transactionItem).blockingGet()

        val intent = Intent(app, AddDetailedTransactionActivity::class.java).also {
            it.putExtra(AddDetailedTransactionActivity.ARG_MODE, "edit")
            it.putExtra(AddDetailedTransactionActivity.ARG_EDIT_TRANSACTION_ID, id)
        }
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch<AddDetailedTransactionActivity>(intent)
        scenario = addDetailedTransactionActivityScenario

        val draftFile = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.DRAFT_FILE_NAME)
        assertFalse(draftFile.exists())
    }

    @Test
    fun givenAtLeastOneImageExistsInDbWhenAddImageThenDialogOptionPresent() {
        val addDetailedTransactionActivityScenario = ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        saveImageToDevice(TestData.Resource.Images.BREAD).blockingSubscribe()
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main, 0,
            R.id.text_view_add_detailed_transaction_show_details
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)
        onView(withId(R.id.image_view_add_external_media_local_image)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun givenNoImagesExistInDbWhenAddImageThenDialogOptionPresent() {
        val addDetailedTransactionActivityScenario = ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main, 0,
            R.id.text_view_add_detailed_transaction_show_details
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)
        onView(withId(R.id.image_view_add_external_media_local_image)).inRoot(isDialog())
            .check(matches(not((isDisplayed()))))
    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionExistsWhenEditsMadeAndCancelClickedThenTransactionNotChanged() {

    }

    @Test
    fun givenModeIsAddAndChangesMadeWhenCloseActivityThenDraftDiscarded() {

        //Open Add Detailed Transaction Screen
        var addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        val basicAmount = "100.00"
        val basicDescription = "Description"


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

        addDetailedTransactionActivityScenario.moveToState(Lifecycle.State.DESTROYED)
        addDetailedTransactionActivityScenario.close()
        assertTrue(app.draftExists())
        addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "0"
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description
        )
        clearTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description,
            " " // It seems setting the text to empty doesn't trigger a change. It's either something I did or a subtle bug, either way I'm leaving it like this
        )
        addDetailedTransactionActivityScenario.moveToState(Lifecycle.State.DESTROYED)

        assertFalse(app.draftExists())
    }

    @Test //TODO Implement
    fun givenImageIsNotInDraftAndImageIsNotInDbAndImageDoesNotNeedModificationsWhenAddImageThenDialogDoesNotShow() {
        /*val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        val imageUnder1Mib = TestData.Resource.Images.DUMBBELLS_3

        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            TestData.Resource.Images.DUMBBELLS_3
        )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader

        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.HAMMER.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)


        onView(withId(R.id.image_view_add_detailed_transaction_item_add_image)).check(matches(ViewMatchers.isNotEnabled()))*/
    }


    @Test
    //TODO These tests fail because the dialogs don't open on time consistently. I should rework how Rx works in general
    fun givenImageIsNotInDraftAndImageIsNotInDbAndImageNeedsModificationsWhenAddImageThenDialogShows() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        val imageOver1Mib = TestData.Resource.Images.DUMBBELLS_1

        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            imageOver1Mib
        )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader

        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = imageOver1Mib.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)

        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)

        onView(withId(R.id.linear_layout_add_image)).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
    }


    @Test
    fun givenImageIsNotInDraftAndImageIsInDbAndImageDoesNotNeedModificationsWhenAddImageThenDialogDoesNotShow() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        val imageBelow1Mib = TestData.Resource.Images.DUMBBELLS_1_BELOW_1600

        saveImageToDevice(imageBelow1Mib)
        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            imageBelow1Mib
        )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader

        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = imageBelow1Mib.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)

        val dialog = ShadowDialog.getLatestDialog()
        assertFalse(dialog.isShowing)

    }


    /**
     * https://github.com/robolectric/robolectric/issues/5131#issuecomment-505819342
     */
    @Test
    //TODO These tests fail because the dialogs don't open on time consistently. I should rework how Rx works in general
    fun givenImageIsNotInDraftAndImageInDbAndImageNeedsModificationsWhenAddImageThenDialogShows() {
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario
        val imageAbove1Mib = TestData.Resource.Images.DUMBBELLS_1

        saveImageToDevice(imageAbove1Mib)
        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            imageAbove1Mib
        )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        val runtime = Runtime.getRuntime()
        LOGGER.debug("Memory: Free: {}, Total: {}, Max: {}", runtime.freeMemory().formatBytes(Locale.US), runtime.totalMemory().formatBytes(Locale.US), runtime.maxMemory().formatBytes(Locale.US))
        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = imageAbove1Mib.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION * 3)

        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)

        onView(withId(R.id.linear_layout_add_image)).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
    }


    @Test
    fun givenImageIsInDraftAndImageIsNotInDbAndImageDoesNotNeedModificationsWhenAddImageThenDialogDoesNotShow() {
        val imageBelow1MiB = TestData.Resource.Images.DUMBBELLS_1_BELOW_1600

        val _draft = buildDraft(BigDecimal.TEN, "Candy", "food")
        val item = _draft.items[0]
        val images = item.images + AddEditTransactionFile(null, imageBelow1MiB.uri, "image/jpeg", imageBelow1MiB.sha256, 188_605)
        val modifiedDraft = _draft.copy(items = listOf(item.copy(images = images)))
        val fileHandler: DraftFileHandler = app
        addDetailedTransactionRepository.createDraft().blockingSubscribe()

        fileHandler.saveDraft(modifiedDraft).subscribe()

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario

        saveImageToDevice(imageBelow1MiB)
        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            imageBelow1MiB
        )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader

        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = imageBelow1MiB.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)

        val dialog = ShadowDialog.getLatestDialog()
        assertFalse(dialog.isShowing)

    }


    @Test
    fun givenImageIsInDraftAndImageIsNotInDbAndImageNeedsModificationsWhenAddImageThenDialogDoesNotShow() {
        val imageWithLocation = TestData.Resource.Images.DUMBBELLS_1_GEOTAGGED

        val _draft = buildDraft(BigDecimal.TEN, "Candy", "food")
        val item = _draft.items[0]
        val images = item.images + AddEditTransactionFile(null, imageWithLocation.uri, "image/jpeg", imageWithLocation.sha256, 188_605)

        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val f = File(draftImagesFolder, imageWithLocation.fileName)
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        copyResourceToFile(classLoader, imageWithLocation.resourceName, f)
        //TODO Make a shortcut method "addImageToDraft" or review if I already have an existing one
        val modifiedDraft = _draft.copy(items = listOf(item.copy(images = images)), imageHashes = mapOf(imageWithLocation.sha256 to imageWithLocation.uri))
        val fileHandler: DraftFileHandler = app
        addDetailedTransactionRepository.createDraft().blockingSubscribe()

        fileHandler.saveDraft(modifiedDraft).subscribe()

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario

        saveImageToDevice(imageWithLocation)
        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            imageWithLocation
        )

        draftImagesFolder.mkdirs()

        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = imageWithLocation.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)

        val dialog = ShadowDialog.getLatestDialog()
        assertFalse(dialog.isShowing)

    }

    @Test
    fun givenModifiedImageIsInDraftAndImageIsNotInDbAndImageNeedsModificationsWhenAddImageThenDialogDoesNotShow() {
        val imageWithLocation = TestData.Resource.Images.DUMBBELLS_1_GEOTAGGED
        val imageWithoutLocation = TestData.Resource.Images.DUMBBELLS_1

        val _draft = buildDraft(BigDecimal.TEN, "Candy", "food")
        val item = _draft.items[0]
        val images = item.images + AddEditTransactionFile(null, imageWithoutLocation.uri, "image/jpeg", imageWithoutLocation.sha256, 188_605, false, imageWithLocation.sha256)

        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val f = File(draftImagesFolder, imageWithoutLocation.fileName)
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        copyResourceToFile(classLoader, imageWithoutLocation.resourceName, f)
        //TODO Make a shortcut method "addImageToDraft" or review if I already have an existing one
        val modifiedDraft = _draft.copy(items = listOf(item.copy(images = images)), imageHashes = mapOf(imageWithoutLocation.sha256 to imageWithLocation.uri), sourceImageHashes = mapOf(imageWithLocation.sha256 to imageWithoutLocation.sha256))
        val fileHandler: DraftFileHandler = app
        addDetailedTransactionRepository.createDraft().blockingSubscribe()

        fileHandler.saveDraft(modifiedDraft).subscribe()

        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch(AddDetailedTransactionActivity::class.java)
        scenario = addDetailedTransactionActivityScenario

        saveImageToDevice(imageWithoutLocation)
        addContentProviderResources(app, AddDetailedTransactionActivityTest::class.java.classLoader,
            imageWithLocation
        )

        draftImagesFolder.mkdirs()

        val draft = addDetailedTransactionRepository.getDraftValue()

        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)

        val returnIntent = Intent().also {
            it.data = imageWithLocation.uri
        }

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
        pickFileFromDevice()
        Thread.sleep(SLEEP_DURATION)

        val dialog = ShadowDialog.getLatestDialog()
        assertFalse(dialog.isShowing)

    }

    @Test
    @Ignore("Feels redundant for now")
    fun givenImageIsInDraftAndImageIsInDbAndImageNeedsModificationsWhenAddImageThenDialogDoesNotShow() {
        assertTrue(false)
    }

    @Test
    @Ignore("I'm not sure if a file like this will ever exist, but I do need to make sure I show the proper error message just in case")
    fun givenImageNeedsSizeReductionAndImageDimensionsReducedBelowThresholdAndImageStillTooLargeThenErrorDialog() {
        assertTrue(false)
    }

    fun buildDraft(amount: BigDecimal, description: String, categoryStringId: String, evidence: List<AddEditTransactionFile> = emptyList(), note: String = ""): AddEditDetailedTransactionDraft {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryRepository.findByProfileIdAndStringId(profile.id!!, categoryStringId).subscribeOn(Schedulers.io()).blockingGet()!!
        val evidenceMap = mutableMapOf<String, Uri>()
        for(resource in evidence) {
            evidenceMap[resource.sha256] = resource.uri
        }
        return AddEditDetailedTransactionDraft(items = listOf(AddTransactionItem(0, null, category.id!!, amount, description)), evidence = evidence, evidenceHashes = evidenceMap, accountId = accountId)
    }

    fun pickFileFromDevice() {
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)
        onView(withId(R.id.image_view_add_external_media_device_file)).inRoot(RootMatchers.isDialog()).perform(click())
        onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog()).perform(click())
        ShadowLooper.runUiThreadTasks()
    }

    fun saveImageToDevice(resource: TestData.Resource, filename: String = "45402cd3-2452-4804-981a-7ea5515dec74.jpg"): Single<Long> {
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val mainImage = File(mainImagesFolder, filename)
        if(mainImage.exists()) {
            throw Exception("Image already exists on device")
        }
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        copyResourceToFile(classLoader, resource.resourceName, mainImage)
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val image = ImageDb(null, profile.id!!, 0L, resource.sha256, "image/jpeg", mainImage.toUri().toString(), dateTimeString, offset, zone)
        return imageDao.insertImage(image).subscribeOn(Schedulers.io())
    }
}