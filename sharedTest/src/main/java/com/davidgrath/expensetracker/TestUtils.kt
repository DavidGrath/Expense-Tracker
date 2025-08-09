package com.davidgrath.expensetracker

import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ReplaceTextAction
import androidx.test.espresso.action.TypeTextAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

class RecyclerInputTextItemAction(private val id: Int, private val input: String): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    }

    override fun getDescription(): String {
        return "Inputting description"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val editText = view!!.findViewById<EditText>(id)
        TypeTextAction(input).perform(uiController, editText)
    }
}

/**
 * "TypeText" doesn't work for inputType number so this should be used instead
 */
class RecyclerInputNumberItemAction(private val id: Int, private val input: String): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    }

    override fun getDescription(): String {
        return "Inputting description"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val editText = view!!.findViewById<EditText>(id)
        ReplaceTextAction(input).perform(uiController, editText)
    }
}

class RecyclerClickItemAction(private val id: Int): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    }

    override fun getDescription(): String {
        return "Clicking item"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val subView = view!!.findViewById<View>(id)
        subView.performClick()
//        ViewActions.click().perform(uiController, subView) //Doesn't work with local tests for some reason
    }
}

fun <VH : RecyclerView.ViewHolder> typeTextRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes editTextId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerInputTextItemAction(editTextId, text)))
}
fun <VH : RecyclerView.ViewHolder> inputNumberRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes editTextId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerInputNumberItemAction(editTextId, text)))
}

fun <VH : RecyclerView.ViewHolder> clickRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes viewId: Int) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId))
        .perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerClickItemAction(viewId)))
}