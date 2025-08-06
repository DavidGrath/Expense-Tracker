package com.davidgrath.expensetracker.ui.addtransaction

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.RecyclerClickItemAction
import com.davidgrath.expensetracker.RecyclerInputNumberItemAction
import com.davidgrath.expensetracker.RecyclerInputTextItemAction
import com.davidgrath.expensetracker.ui.main.MainActivity
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddDetailedTransactionActivityInstrumentedTest {

    @Test
    fun givenItemsAreAllValidWhenDoneThenTransactionAdded() {
        //TODO Swap with Room test to make sure MainActivity is removed from this
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.text_view_transaction_item_amount)).check(doesNotExist())
        onView(withId(R.id.fab_main)).perform(longClick())

        onView(withId(R.id.linear_layout_add_detailed_transaction_main_add_item))
            .perform(click())
        val basicAmount = "300"
        val basicDescription = "Water"
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(0,
                RecyclerClickItemAction(R.id.edit_text_add_detailed_transaction_item_amount)))
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(0,
                RecyclerInputNumberItemAction(R.id.edit_text_add_detailed_transaction_item_amount, "")))
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(0,
                RecyclerInputTextItemAction(R.id.edit_text_add_detailed_transaction_item_amount, basicAmount)))
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(0,
                RecyclerInputTextItemAction(R.id.edit_text_add_detailed_transaction_item_description, basicDescription)
            ))

        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                RecyclerClickItemAction(R.id.edit_text_add_detailed_transaction_item_amount)))
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                RecyclerInputNumberItemAction(R.id.edit_text_add_detailed_transaction_item_amount, "")))
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                RecyclerInputTextItemAction(R.id.edit_text_add_detailed_transaction_item_amount, "400.00")))
        onView(withId(R.id.recyclerview_add_detailed_transaction_main))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>(1,
                RecyclerInputTextItemAction(R.id.edit_text_add_detailed_transaction_item_description, "Bread")))

        onView(withId(R.id.image_button_add_detailed_transaction_done)).perform(click())
        //Be sure that the home screen is displayed
        onView(withId(R.id.frame_main)).check(matches(isDisplayed()))
        onView(withText("Bread")).check(matches(isDisplayed()))
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