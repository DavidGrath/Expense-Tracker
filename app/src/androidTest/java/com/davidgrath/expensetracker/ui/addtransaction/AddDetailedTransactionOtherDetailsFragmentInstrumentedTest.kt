package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.EspressoKey
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.addContentProviderResourcesInstrumented
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class AddDetailedTransactionOtherDetailsFragmentInstrumentedTest {
    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val addDetailedTransactionActivityScenarioRule = ActivityScenarioRule(AddDetailedTransactionActivity::class.java)

    @Inject
    lateinit var repository: AddDetailedTransactionRepository
    @Inject
    lateinit var database: ExpenseTrackerDatabase
    lateinit var app: ExpenseTracker

//    @Before
    fun setUp() {
//        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        LOGGER.debug("setUp OtherDetails")
        app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
        LOGGER.debug("setUp OtherDetails 2")
        app.tempInit().subscribeOn(Schedulers.io()).blockingSubscribe()
        LOGGER.debug("setUp OtherDetails 3")
        (app.appComponent as InstrumentedTestComponent).inject(this)
        LOGGER.debug("setUp OtherDetails 4")
        Espresso.onView(ViewMatchers.withId(R.id.tab_layout_add_detailed_transaction))
            .perform(TabLayoutItemClick(1))
        LOGGER.debug("setUp OtherDetails 5")
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        Single.fromCallable { database.clearAllTables() }.subscribeOn(Schedulers.io()).blockingSubscribe()
        draftFolder.deleteRecursively()
//        val contentFolder = File(context.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER) //TODO Instrumented version of delete
//        contentFolder.deleteRecursively()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        mainFolder.deleteRecursively()
    }

    @Test
    fun givenEvidenceIsPdfAndPdfIsPasswordProtectedWhenSelectThenErrorDialog() {
        setUp()
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

        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(scrollTo(),  click())
        onView(withId(R.id.image_view_add_external_media_device_file)).perform(click())
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(android.R.id.message)).inRoot(isDialog()).check(matches(allOf(isDisplayed(), withText(Matchers.matchesRegex(".*password.*"))))) //probably change to resourceId

        //"Clean up"
        onView(withId(android.R.id.button1)).perform(click())
    }

    @Test
    fun givenEvidenceIsPdfAndPdfHasZeroPagesWhenSelectThenErrorDialog() {
        setUp()
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
        onView(withId(R.id.image_view_add_external_media_device_file)).perform(click())
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(android.R.id.message)).check(matches(allOf(isDisplayed(), withText(Matchers.matchesRegex(".*zero.*"))))) //probably change to resourceId

        //"Clean up"
        onView(withId(android.R.id.button1)).perform(click())
    }

    /**
     * If the user somehow finds a way to stack 501+ combining characters into 1, then I think this test fails, but ignore that
     */
    @Test
    fun whenUserPastesTextAndTotalLengthOverCharacterLimitThenTextTruncatedByGrapheme() {
        setUp()
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val text = String(CharArray(Constants.MAX_NOTE_CODEPOINT_LENGTH - 1) {
            'a'
        })
        val manRunning = "\uD83C\uDFC3\u200D\u2642\uFE0F" //4 codepoints - 1 surrogate pair + 3 regular
        val manRunningCount = 4
        addDetailedTransactionActivityScenarioRule.scenario.onActivity {
            it.runOnUiThread {
                val clipBoardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                var clipData = ClipData.newPlainText("simple text", text + manRunning)
                clipBoardManager.setPrimaryClip(clipData)
            }
        }

        onView(withId(R.id.edit_text_add_detailed_transaction_note)).perform(
            click(), ViewActions.pressKey(
            EspressoKey.Builder().withCtrlPressed(true).withKeyCode(KeyEvent.KEYCODE_V).build()))
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).check { view, noViewFoundException ->
            val v = view as EditText
            val text = v.text.toString()
            val count = text.codePointCount(0, text.length)
            assertEquals(Constants.MAX_NOTE_CODEPOINT_LENGTH - 1, count)
        }
        onView(withId(R.id.text_view_add_detailed_transaction_note_length_indicator)).check(matches(withText("${Constants.MAX_NOTE_CODEPOINT_LENGTH - 1}/${Constants.MAX_NOTE_CODEPOINT_LENGTH}")))

        val menRunning = manRunning.repeat(Math.floorDiv(Constants.MAX_NOTE_CODEPOINT_LENGTH, manRunningCount)+1) //4 x 126 = 504
        addDetailedTransactionActivityScenarioRule.scenario.onActivity {
            it.runOnUiThread {
                val clipBoardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                var clipData = ClipData.newPlainText("simple text", menRunning)
                clipBoardManager.setPrimaryClip(clipData)
            }
        }
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).perform(
            clearText(), ViewActions.pressKey(
                EspressoKey.Builder().withCtrlPressed(true).withKeyCode(KeyEvent.KEYCODE_V).build()))
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).check { view, noViewFoundException ->
            val v = view as EditText
            val text = v.text.toString()
            val count = text.codePointCount(0, text.length)
            assertEquals(Constants.MAX_NOTE_CODEPOINT_LENGTH, count)
        }
        onView(withId(R.id.text_view_add_detailed_transaction_note_length_indicator)).check(matches(withText("${Constants.MAX_NOTE_CODEPOINT_LENGTH}/${Constants.MAX_NOTE_CODEPOINT_LENGTH}")))
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).perform(clearText())
    }

    @Test
    fun whenUserPastesTextAndTotalLengthOverCharacterLimitThenTextTruncated() {
        setUp()
        val text = String(CharArray(Constants.MAX_NOTE_CODEPOINT_LENGTH - 9) {
            'a'
        }) + "bcdefghij" + "klmnopqrst"
        addDetailedTransactionActivityScenarioRule.scenario.onActivity {
            it.runOnUiThread {
                val clipBoardManager =
                    app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("simple text", text)
                clipBoardManager.setPrimaryClip(clipData)
            }
        }
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).perform(
            click(), ViewActions.pressKey(
            EspressoKey.Builder().withCtrlPressed(true).withKeyCode(KeyEvent.KEYCODE_V).build()))
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).check { view, noViewFoundException ->
            val v = view as EditText
            val text = v.text.toString()
            val count = text.codePointCount(0, text.length)
            assertEquals(Constants.MAX_NOTE_CODEPOINT_LENGTH, count)
            assertEquals("abcdefghij", text.substring(Constants.MAX_NOTE_CODEPOINT_LENGTH - 10))
        }
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).perform(clearText())
    }


    /*@Test
    fun givenEvidenceIsPdfAndPdfOkayWhenSelectThenImageDisplayed() {
    }*/

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionActivityInstrumentedTest::class.java)

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            LOGGER.info("setUpClass")
        }
    }
}