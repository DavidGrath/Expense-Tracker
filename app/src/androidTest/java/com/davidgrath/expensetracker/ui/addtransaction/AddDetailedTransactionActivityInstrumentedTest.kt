package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.davidgrath.expensetracker.CategoryStringIdMatcher
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.RecyclerInputTextItemAction
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.addContentProviderResourcesInstrumented
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.inputNumberRecyclerViewItemInstrumented
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.scrollRecyclerViewItem
import com.davidgrath.expensetracker.typeTextRecyclerViewItem
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.squareup.rx3.idler.Rx3Idler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.CoreMatchers.allOf
import javax.inject.Inject
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ToDo Make a full "use case"/"acceptance" test, including these steps
 *  1. Open draft screen
 *  2. Make all possible changes, including seller, location, custom time, document, additional item, image for item, description, note, etc
 *  3. Recreate activity to test that draft saving and restoration work well
 *  4. Save draft
 *  5. Check details screen. Assert correctness where applicable
 *  6. Open edit screen
 *  7. Change basically everything
 *  7b. Do **NOT** recreate the activity because I haven't designed edits to persist like that
 *  8. Save and verify again
 *
 *  I'm not sure exactly where to place this test since it's so different. Thoughts include:
 *  * Separate build variant called "acceptance"
 *  * I don't think JUnit exclude works well with test<Variant>UnitTest based on past attempts, but I'd like to use that if possible
 *  * Possibly work with Categories, though from what I glance, it uses up the RunWith annotation
 *  * Contemplate Jupiter, I think it has features that make this easier, although I don't know what the learning curve is like
 */
@RunWith(AndroidJUnit4::class)
class AddDetailedTransactionActivityInstrumentedTest {

    @get:Rule
    val intentsRule = IntentsRule()
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var transactionItemImagesDao: TransactionItemImagesDao
    @Inject
    lateinit var imageDao: ImageDao
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Inject
    lateinit var database: ExpenseTrackerDatabase
    lateinit var app: InstrumentedTestExpenseTracker
    lateinit var uiDevice: UiDevice

    //TODO Might remove. Trying to fix an issue with Glide, the AddImageDialog, and loading affecting layout
    /*companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            RxJavaPlugins.setInitIoSchedulerHandler(Rx3Idler.create("Instrumented Rx3 Handler"))
        }
    }*/

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
        app.tempInit().subscribeOn(Schedulers.io()).blockingSubscribe()
        (app.appComponent as InstrumentedTestComponent).inject(this)
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        addDetailedTransactionRepository.deleteDraft()
        Single.fromCallable { database.clearAllTables() }.subscribeOn(Schedulers.io()).blockingSubscribe()
    }

    @Test
    fun givenItemsAreAllValidWhenDoneThenTransactionAdded() {
        //TODO Swap with Room test to make sure MainActivity is removed from this
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.text_view_transaction_item_amount)).check(doesNotExist())
        onView(withId(R.id.fab_transactions)).perform(longClick())

        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item))
            .perform(click())
        val basicAmount = "300"
        val basicDescription = "Water"
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        inputNumberRecyclerViewItemInstrumented<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
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

        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                scrollTo()
            ))
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "400.00"
        )

        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                RecyclerInputTextItemAction(R.id.edit_text_add_detailed_transaction_item_description, "Bread")))

        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        //Be sure that the home screen is displayed
        onView(withId(R.id.viewpager_main)).check(matches(isDisplayed()))
        onView(withText("Bread")).check(matches(isDisplayed()))
    }

    @Test
    fun givenImageWasAddedToItemWhenDoneThenFirstImageVisible() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.text_view_transaction_item_amount)).check(ViewAssertions.doesNotExist())
        onView(withId(R.id.fab_transactions)).perform(ViewActions.longClick())

        val basicAmount = "300"
        val basicDescription = "Water"
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
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
        Espresso.closeSoftKeyboard()

        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResourcesInstrumented(context, TestData.Resource.Images.BREAD)

        //Open Image from system
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.BREAD.uri
        }


        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        onView(withId(R.id.image_view_add_external_media_device_file)).inRoot(isDialog()).perform(click())
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        onView(withId(R.id.image_view_transaction_item_first_image)).check(matches(allOf(isDisplayed(), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
    }

    @Test
    fun givenDoneWhenCreateNewDraftThenDraftEmpty() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.text_view_transaction_item_amount)).check(ViewAssertions.doesNotExist())
        onView(withId(R.id.fab_transactions)).perform(ViewActions.longClick())

        val basicAmount = "300"
        val basicDescription = "Water"
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
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
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        onView(withId(R.id.viewpager_main)).check(matches(isDisplayed()))
        onView(withText(basicDescription)).check(matches(isDisplayed()))

        onView(withId(R.id.fab_transactions)).perform(ViewActions.longClick())
        onView(withId(R.id.edit_text_add_detailed_transaction_item_amount)).check(matches(
            ViewMatchers.withText("")))
        onView(withId(R.id.edit_text_add_detailed_transaction_item_description)).check(matches(
            ViewMatchers.withText("")))
    }

    @Test
    fun givenMoreThanOneItemAndItemsHaveDifferentCategoriesSelectedWhenSaveThenCategoriesSavedProperly() {
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fab_transactions)).perform(longClick())

        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(scrollTo(), click())
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item)).perform(scrollTo(), click())

//        inputNumberRecyclerViewItemInstrumented<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "100.00"
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Bread"
        )
        closeSoftKeyboard()
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.spinner_add_detailed_transaction_item_category
        )
        Espresso.onData(allOf(CategoryStringIdMatcher("food"))).perform(click())

        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                scrollTo()
            ))
//        inputNumberRecyclerViewItemInstrumented<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
        scrollRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount,
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "200.00"
        )
        closeSoftKeyboard()
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Bus Fare"
        )
        closeSoftKeyboard()
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.spinner_add_detailed_transaction_item_category
        )
        Espresso.onData(allOf(CategoryStringIdMatcher("transportation"))).perform(click())

        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(2,
                scrollTo()
            ))
//        inputNumberRecyclerViewItemInstrumented<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
        scrollRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_amount,
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_amount,
            "300.00"
        )
        closeSoftKeyboard()
        scrollRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_description,
        )
        typeTextRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.edit_text_add_detailed_transaction_item_description,
            "Movie Night"
        )
        closeSoftKeyboard()
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            2,
            R.id.spinner_add_detailed_transaction_item_category
        )
        Espresso.onData(allOf(CategoryStringIdMatcher("entertainment"))).perform(click())

        var list = emptyList<TransactionWithItemAndCategory>()

        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val accountId = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().firstOrNull()!!.id
        transactionRepository.getTransactions(profile.id!!, accountId!!, null, null).subscribe {
            list = it
        }
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        onView(withId(R.id.viewpager_main)).check(matches(allOf(isDisplayed(), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))


        assertEquals(3, list.size)
        val categoryOne = list[0].categoryStringId
        val categoryTwo = list[1].categoryStringId
        val categoryThree = list[2].categoryStringId

        assertEquals("food", categoryOne)
        assertEquals("transportation", categoryTwo)
        assertEquals("entertainment", categoryThree)

    }

    val LAUNCH_TIMEOUT = 5_000L
    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun simpleCameraIntentTestApiFileProvider() {

        uiDevice.pressHome()

        val launcherPackage = getLauncherPackageName()
        assertNotNull(launcherPackage)
        uiDevice.wait(Until.hasObject(By.pkg(launcherPackage!!).depth(0)), LAUNCH_TIMEOUT)

        val packageName = "com.davidgrath.expensetracker"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)!!.also {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(packageName).depth(0)), LAUNCH_TIMEOUT)

        val fab = uiDevice.findObject(UiSelector().resourceIdMatches(".*fab_transactions"))
        fab.longClick()

        val showDetails = uiDevice.findObject(UiSelector().resourceIdMatches(".*text_view_add_detailed_transaction_show_details"))
        showDetails.click()

        val addImage = uiDevice.findObject(UiSelector().resourceIdMatches(".*image_view_add_detailed_transaction_item_add_image"))
        addImage.click()

        val useCamera = uiDevice.findObject(UiSelector().resourceIdMatches(".*image_view_add_external_media_camera"))
        useCamera.click()

        val ok = uiDevice.findObject(UiSelector().resourceIdMatches(".*button1"))

        uiDevice.performActionAndWait( {ok.click()}, Until.newWindow(), LAUNCH_TIMEOUT)
        val shutterId = if(Build.MANUFACTURER.contains("samsung", true)) {
            "com.sec.android.app.camera:id/normal_center_button"
        } else {
            "com.android.camera:id/shutter_button"
        }
        val shutter = uiDevice.findObject(UiSelector().resourceId(shutterId))
        shutter.click()
        val doneButtonId = if(Build.MANUFACTURER.contains("samsung", true)) {
            "com.sec.android.app.camera:id/okay"
        } else {
            "com.android.camera:id/btn_done"
        }
        val done = uiDevice.findObject(UiSelector().resourceId(doneButtonId))
        uiDevice.performActionAndWait( {done.click()}, Until.newWindow(), LAUNCH_TIMEOUT)

        val addImageOk = uiDevice.findObject(UiSelector().resourceIdMatches(".*button1"))
        uiDevice.performActionAndWait( {addImageOk.click()}, Until.newWindow(), LAUNCH_TIMEOUT)

        val draft = addDetailedTransactionRepository.getDraftValue()
        val items = draft.items
        val itemImages = items[0].images
        assertEquals(1, itemImages.size)
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 23)
    fun simpleCameraIntentTestNoRuntimePermissions() {
        uiDevice.pressHome()

        val launcherPackage = getLauncherPackageName()
        assertNotNull(launcherPackage)
        uiDevice.wait(Until.hasObject(By.pkg(launcherPackage!!).depth(0)), LAUNCH_TIMEOUT)

        val packageName = "com.davidgrath.expensetracker"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)!!.also {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(packageName).depth(0)), LAUNCH_TIMEOUT)

        val fab = uiDevice.findObject(UiSelector().resourceIdMatches(".*fab_transactions"))
        fab.longClick()

        val showDetails = uiDevice.findObject(UiSelector().resourceIdMatches(".*text_view_add_detailed_transaction_show_details"))
        showDetails.click()

        val addImage = uiDevice.findObject(By.desc("Add image to item"))
        addImage.click()

        val useCamera = uiDevice.findObject(UiSelector().resourceIdMatches(".*image_view_add_external_media_camera"))
        useCamera.click()

        val ok = uiDevice.findObject(UiSelector().resourceIdMatches(".*button1"))
        val action = Runnable {
            ok.click()
        }
        uiDevice.performActionAndWait(action, Until.newWindow(), LAUNCH_TIMEOUT)
        val shutter = uiDevice.findObject(UiSelector().resourceId("com.android.camera:id/shutter_button"))
        shutter.click()

        val done = uiDevice.findObject(UiSelector().resourceId("com.android.camera:id/btn_done"))
        done.click()

        val addImageOk = uiDevice.findObject(UiSelector().resourceIdMatches(".*button1"))
        uiDevice.performActionAndWait( {addImageOk.click()}, Until.newWindow(), LAUNCH_TIMEOUT)

        val draft = addDetailedTransactionRepository.getDraftValue()
        val items = draft.items
        val itemImages = items[0].images
        assertEquals(1, itemImages.size)
    }

    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 22)
    fun simpleCameraIntentTest() {
        uiDevice.pressHome()

        val launcherPackage = getLauncherPackageName()
        assertNotNull(launcherPackage)
        uiDevice.wait(Until.hasObject(By.pkg(launcherPackage!!).depth(0)), LAUNCH_TIMEOUT)

        val packageName = "com.davidgrath.expensetracker"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)!!.also {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(packageName).depth(0)), LAUNCH_TIMEOUT)

        val fab = uiDevice.findObject(UiSelector().resourceIdMatches(".*fab_transactions"))
        fab.longClick()

        val showDetails = uiDevice.findObject(UiSelector().resourceIdMatches(".*text_view_add_detailed_transaction_show_details"))
        showDetails.click()

        val addImage = uiDevice.findObject(By.desc("Add image to item"))
        addImage.click()

        val useCamera = uiDevice.findObject(UiSelector().resourceIdMatches(".*image_view_add_external_media_camera"))
        useCamera.click()

        val ok = uiDevice.findObject(UiSelector().resourceIdMatches(".*button1"))
        val action = Runnable {
            ok.click()
        }
        uiDevice.performActionAndWait(action, Until.newWindow(), LAUNCH_TIMEOUT)
        val shutter = uiDevice.findObject(UiSelector().resourceId("com.android.camera:id/shutter_button"))
        shutter.click()

        val done = uiDevice.findObject(UiSelector().resourceId("com.android.camera:id/btn_done"))
        done.click()

        val addImageOk = uiDevice.findObject(UiSelector().resourceIdMatches(".*button1"))
        uiDevice.performActionAndWait( {addImageOk.click()}, Until.newWindow(), LAUNCH_TIMEOUT)

        val draft = addDetailedTransactionRepository.getDraftValue()
        val items = draft.items
        val itemImages = items[0].images
        assertEquals(1, itemImages.size)
    }

    @Test
    @Ignore("This test mostly works, but unfortunately, the timing for when the second checkbox becomes visible is inconsistent. Maybe I can fix it later") //TODO
    fun addImageWithModificationsTest() {

        //Add image to item
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResourcesInstrumented(context, TestData.Resource.Images.DUMBBELLS_1_GEOTAGGED)

        val addDetailedTransactionActivityScenario = ActivityScenario.launch(AddDetailedTransactionActivity::class.java)

        //Open Image from system
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(R.id.recyclerview_add_detailed_transaction_main, 0, R.id.text_view_add_detailed_transaction_show_details)
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Images.DUMBBELLS_1_GEOTAGGED.uri
        }


        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            0,
            R.id.image_view_add_detailed_transaction_item_add_image
        )

        onView(withId(R.id.image_view_add_external_media_device_file)).inRoot(isDialog()).perform(click())
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

        //Select remove location, select compress
        onView(withId(R.id.check_box_add_image_remove_location)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.check_box_add_image_reduce_size)).inRoot(isDialog()).perform(click())
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        //Assert that an image view is present in first item
        var draft = addDetailedTransactionRepository.getDraftValue()
        var items = draft.items
        var itemImages = items[0].images
        assertEquals(1, itemImages.size)
        //Add second item
        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item))
            .perform(scrollTo(),  click())
        //Add image
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )
        clickRecyclerViewItem<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(
            R.id.recyclerview_add_detailed_transaction_main,
            1,
            R.id.image_view_add_detailed_transaction_item_add_image
        )
        //Assert no dialog

        //Assert image is added to second item
        draft = addDetailedTransactionRepository.getDraftValue()
        items = draft.items
        itemImages = items[1].images
        assertEquals(1, itemImages.size)
    }

    fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).also {
            it.addCategory(Intent.CATEGORY_HOME)
        }
        val packageManager = app.packageManager
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * TODO This class was made early on when I didn't know exactly how to work with Espresso. Marked for deletion
     */
    class TagMatcher(private val tag: Long): TypeSafeMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("Has tag $tag")
        }

        override fun matchesSafely(item: View?): Boolean {
            return item != null && item.tag is Long && item.tag == tag
        }
    }

}