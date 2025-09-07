package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
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
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.CategoryStringIdMatcher
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.RecyclerInputTextItemAction
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.addContentProviderResourcesInstrumented
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.inputNumberRecyclerViewItemInstrumented
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.scrollRecyclerViewItem
import com.davidgrath.expensetracker.typeTextRecyclerViewItem
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.hamcrest.CoreMatchers.allOf
import javax.inject.Inject
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

    @Before
    fun setUp() {
        (ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>().appComponent as InstrumentedTestComponent).inject(this)
    }

    @After
    fun tearDown() {
        transactionItemImagesDao.deleteAll().blockingSubscribe()
        imageDao.deleteAll().blockingSubscribe()
        transactionItemDao.deleteAll().blockingSubscribe()
        transactionDao.deleteAll().blockingSubscribe()
        addDetailedTransactionRepository.deleteDraft()
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
        transactionRepository.getTransactions().subscribe {
            list = it
        }
        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        onView(withId(R.id.viewpager_main)).check(matches(allOf(isDisplayed(), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))


        assertEquals(3, list.size)
        val categoryOne = list[0].categoryStringID
        val categoryTwo = list[1].categoryStringID
        val categoryThree = list[2].categoryStringID

        assertEquals("food", categoryOne)
        assertEquals("transportation", categoryTwo)
        assertEquals("entertainment", categoryThree)

    }

    class TagMatcher(private val tag: Long): TypeSafeMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("Has tag $tag")
        }

        override fun matchesSafely(item: View?): Boolean {
            return item != null && item.tag is Long && item.tag == tag
        }
    }


}