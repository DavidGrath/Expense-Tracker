package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestConstants
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.addContentProviderResourcesInstrumented
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.test.TestContentProvider
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AddDetailedTransactionOtherDetailsFragmentInstrumentedTest {
    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val addDetailedTransactionActivityScenarioRule = ActivityScenarioRule(AddDetailedTransactionActivity::class.java)

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        (app.appComponent as InstrumentedTestComponent).inject(this)
        Espresso.onView(ViewMatchers.withId(R.id.tab_layout_add_detailed_transaction))
            .perform(TabLayoutItemClick(1))
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        draftFolder.deleteRecursively()
//        val contentFolder = File(context.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER) //TODO Instrumented version of delete
//        contentFolder.deleteRecursively()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        mainFolder.deleteRecursively()
    }

    @Test
    fun givenEvidenceIsPdfAndPdfIsPasswordProtectedWhenSelectThenErrorDialog() {
        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val resource = TestData.Resource.Documents.EVIDENCE_PDF_PASSWORD_PROTECTED

        addContentProviderResourcesInstrumented(app, resource)
        val returnIntent = Intent().also {
            it.data = resource.uri
        }

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )

        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        onView(withId(android.R.id.message)).check(matches(allOf(isDisplayed(), withText(Matchers.matchesRegex(".*password.*"))))) //probably change to resourceId
    }

    @Test
    fun givenEvidenceIsPdfAndPdfHasZeroPagesWhenSelectThenErrorDialog() {
        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val resource = TestData.Resource.Documents.EVIDENCE_PDF_EMPTY

        addContentProviderResourcesInstrumented(app, resource)
        val returnIntent = Intent().also {
            it.data = resource.uri
        }

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )

        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        onView(withId(android.R.id.message)).check(matches(allOf(isDisplayed(), withText(Matchers.matchesRegex(".*zero.*"))))) //probably change to resourceId
    }

    /*@Test
    fun givenEvidenceIsPdfAndPdfOkayWhenSelectThenImageDisplayed() {
    }*/
}