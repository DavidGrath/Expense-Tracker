package com.davidgrath.expensetracker

import android.content.ContentValues
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ReplaceTextAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.action.TypeTextAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.google.android.material.tabs.TabLayout
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.CustomTypeSafeMatcher
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.io.File
import java.math.BigDecimal

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

class RecyclerClearTextItemAction(private val id: Int): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    }

    override fun getDescription(): String {
        return "Inputting description"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val editText = view!!.findViewById<EditText>(id)
        ReplaceTextAction("").perform(uiController, editText)
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

class RecyclerLongClickItemAction(private val id: Int): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    }

    override fun getDescription(): String {
        return "Long clicking item"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val subView = view!!.findViewById<View>(id)
        subView.performLongClick()
//        ViewActions.click().perform(uiController, subView) //Doesn't work with local tests for some reason
    }
}

class CategoryStringIdMatcher(val stringId: String): CustomTypeSafeMatcher<CategoryUi>("A category") {
    override fun matchesSafely(item: CategoryUi?): Boolean {
        if(item == null) {
            return false
        }
        if(item.stringId == null) {
            return false
        }
        return item.stringId == stringId
    }
}

class AccountUiIdMatcher(val id: Long): CustomTypeSafeMatcher<AccountUi>("An account") {
    override fun matchesSafely(item: AccountUi?): Boolean {
        if(item == null) {
            return false
        }
        return item.id == id
    }
}

class TabLayoutItemClick(val position: Int): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isDisplayed()
        )
    }

    override fun getDescription(): String {
        return "Click TabLayout at item $position"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val tabLayout = view as TabLayout
        tabLayout.getTabAt(position)!!.view.performClick()
    }
}

class RecyclerScrollItemAction(private val id: Int): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    }

    override fun getDescription(): String {
        return "Clicking item"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val subView = view!!.findViewById<View>(id)
        ScrollToAction().perform(uiController, subView)
//        ViewActions.click().perform(uiController, subView) //Doesn't work with local tests for some reason
    }
}

val cursorEndViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isDisplayed())
    }

    override fun getDescription(): String {
        return "Placing cursor at end"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val editText = view as EditText
        editText.setSelection(editText.length())
    }
}

fun <VH : RecyclerView.ViewHolder> typeTextRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes editTextId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(
        RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerClickItemAction(editTextId)),
        RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerInputTextItemAction(editTextId, text))
    )
}
fun <VH : RecyclerView.ViewHolder> inputNumberRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes editTextId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerInputNumberItemAction(editTextId, text)))
}

fun <VH : RecyclerView.ViewHolder> inputNumberRecyclerViewItemInstrumented(@IdRes recyclerViewId: Int, position: Int, @IdRes editTextId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(
        RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerClickItemAction(editTextId)),
        RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerInputTextItemAction(editTextId, text))
    )
}

fun <VH : RecyclerView.ViewHolder> scrollRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes itemId: Int) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(
        RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerScrollItemAction(itemId))
    )
}

fun <VH : RecyclerView.ViewHolder> clearTextRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes editTextId: Int) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId)).perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerClearTextItemAction(editTextId)))
}

fun <VH : RecyclerView.ViewHolder> clickRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes viewId: Int) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId))
        .check { view, noViewFoundException ->
            val subView = view!!.findViewById<View>(viewId)
            ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).check(subView, null)
        }
        .perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerClickItemAction(viewId)))
}

fun <VH : RecyclerView.ViewHolder> longClickRecyclerViewItem(@IdRes recyclerViewId: Int, position: Int, @IdRes viewId: Int) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId))
        .check { view, noViewFoundException ->
            val subView = view!!.findViewById<View>(viewId)
            ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).check(subView, null)
        }
        .perform(RecyclerViewActions.actionOnItemAtPosition<VH>(position, RecyclerLongClickItemAction(viewId)))
}

fun <VH : RecyclerView.ViewHolder> assertRecyclerViewItemText(@IdRes recyclerViewId: Int, position: Int, @IdRes viewId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId))
        .check { view, noViewFoundException ->
            val v = view as RecyclerView
            val subRecyclerViewHolder =
                v.findViewHolderForAdapterPosition(position)
            assertNotNull("ViewHolder at $position does not exist", subRecyclerViewHolder)
            val textView =
                (subRecyclerViewHolder!!.itemView).findViewById<TextView>(viewId)
            val matcher = ViewMatchers.withText(text)
            ViewAssertions.matches(matcher).check(textView,
                null)
        }
}

fun <VH : RecyclerView.ViewHolder> assertRecyclerViewItemSpinnerText(@IdRes recyclerViewId: Int, position: Int, @IdRes viewId: Int, text: String) {
    Espresso.onView(ViewMatchers.withId(recyclerViewId))
        .check { view, noViewFoundException ->
            val v = view as RecyclerView
            val subRecyclerViewHolder =
                v.findViewHolderForAdapterPosition(position)
            assertNotNull("ViewHolder at $position does not exist", subRecyclerViewHolder)
            val spinner =
                (subRecyclerViewHolder!!.itemView).findViewById<Spinner>(viewId)
            val matcher = ViewMatchers.withSpinnerText(Matchers.containsString(text))
            ViewAssertions.matches(matcher).check(spinner,
                null)
        }
}

fun addContentProviderResources(context: Context, classLoader: ClassLoader, vararg images: TestData.Resource) {
    val contentDir = File(context.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER)
    contentDir.mkdir()
    for (image in images) {
        val resourceInputStream = classLoader.getResourceAsStream(image.resourceName)
        val file = File(contentDir, image.fileName)
        val outputStream = file.outputStream()
        val bytes = resourceInputStream.copyTo(outputStream)
        println("addContentProviderResources: Bytes copied: $bytes")
        resourceInputStream.close()
        outputStream.close()
    }
}

fun addContentProviderResourcesInstrumented(context: Context, vararg images: TestData.Resource) {
    for(image in images) {
        val contentValues = ContentValues()
        contentValues.put("resourceName", image.resourceName)
        contentValues.put("fileName", image.fileName)
        context.contentResolver.insert(image.uri, contentValues)
    }
}

fun assertEqualsBD(expected: BigDecimal, actual: BigDecimal) {
    assertEquals("Expected $expected but was $actual",0, expected.compareTo(actual))
}

fun getDefaultAccountId(profileRepository: ProfileRepository, accountRepository: AccountRepository): Long {
    val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
    val accountId = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().firstOrNull()!!.id
    return accountId!!
}