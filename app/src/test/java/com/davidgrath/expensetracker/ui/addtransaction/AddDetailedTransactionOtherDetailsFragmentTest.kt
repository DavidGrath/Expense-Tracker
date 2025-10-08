package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.widget.EditText
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.AccountUiIdMatcher
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestConstants
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.cursorEndViewAction
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getHashCount
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.test.TestContentProvider
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionOtherDetailsFragmentTest {
    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val addDetailedTransactionActivityScenarioRule = ActivityScenarioRule(AddDetailedTransactionActivity::class.java)

    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var transactionItemRepository: TransactionItemRepository
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var evidenceDao: EvidenceDao
    @Inject
    lateinit var timeHandler: TimeHandler

    lateinit var app: TestExpenseTracker

    companion object {
        private val MOCKED_DATE = LocalDate.of(2025, 6, 30)
        private val MOCKED_TIME = LocalTime.of(8, 0)
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionOtherDetailsFragmentTest::class.java)
    }

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)

        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        timeHandler.changeZone(ZoneId.of("UTC"))
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        draftFolder.deleteRecursively()
        val contentFolder = File(context.filesDir, TestConstants.FOLDER_NAME_CONTENT_PROVIDER)
        contentFolder.deleteRecursively()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        mainFolder.deleteRecursively()
    }


    @Test
    fun givenEvidenceAlreadyInDraftWhenSelectSameEvidenceThenEvidenceOnlyAddedOnce() {
        val evidenceResource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val returnIntent = Intent().also {
            it.data = TestData.Resource.Documents.EVIDENCE_IMAGE.uri
        }
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResources(context, AddDetailedTransactionOtherDetailsFragment::class.java.classLoader, TestData.Resource.Documents.EVIDENCE_IMAGE)

        //Open Document from system

        intending(allOf(hasAction(Intent.ACTION_OPEN_DOCUMENT))).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )

        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)
        //Open same document
        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)
        val draft = addDetailedTransactionRepository.getDraftValue()
        val evidence = draft.evidence
        val folder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        assertEquals(1, evidence.size)
        assertEquals(1, getHashCount(evidenceResource.sha256, folder).blockingGet())
    }

    @Test
    fun givenEvidenceAlreadyInMainStorageWhenSelectSameEvidenceThenEvidenceNotCopiedToDraftStorage() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val classLoader = AddDetailedTransactionOtherDetailsFragment::class.java.classLoader
        val resource = TestData.Resource.Documents.SIMPLE_EVIDENCE
        addContentProviderResources(context, classLoader, resource)
        //Copy Document to DB
        val draftDocumentFolder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        draftDocumentFolder.mkdirs()
        val mainDocumentFolder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainDocumentFolder.mkdirs()
        val existingDocument = File(mainDocumentFolder, "45402cd3-2452-4804-981a-7ea5515dec74.pdf")
        copyResourceToFile(classLoader, resource.resourceName, existingDocument)
        evidenceDao.insertEvidence(EvidenceDb(null, 0, 0L, resource.sha256, "application/pdf", existingDocument.toUri().toString(), "2025-06-30T08:00:00", "-04:00", "America/New_York"))
            .subscribeOn(Schedulers.io())
            .blockingGet()

        //Add same item
        val returnIntent = Intent().also {
            it.data = resource.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)
        //Assert no new files, or assert no existing files match same hash

        val draft = addDetailedTransactionRepository.getDraftValue()
        assertEquals(0, getHashCount(resource.sha256, draftDocumentFolder).blockingGet())
        assertEquals(1, draft.evidence.size)
    }

    @Test
    fun givenExternalEvidenceWasModifiedAndOriginalEvidenceAddedToDraftWhenAddEvidenceThenNewEvidenceExistsInDraftStorage() {
        val resource = TestData.Resource.Documents.SIMPLE_EVIDENCE
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val classLoader = AddDetailedTransactionOtherDetailsFragment::class.java.classLoader
        addContentProviderResources(context, classLoader, resource)
        val returnIntent = Intent().also {
            it.data = resource.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)

        val modifiedResource = resource.copy(resourceName = TestData.Resource.Documents.EVIDENCE_IMAGE.resourceName)
        addContentProviderResources(context, classLoader, modifiedResource)
        val modifiedResourceIntent = Intent().also {
            it.data = modifiedResource.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                modifiedResourceIntent
            )
        )
        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)
        val draft = addDetailedTransactionRepository.getDraftValue()
        val evidence = draft.evidence
        assertEquals(2, evidence.size)
        val evidenceFolder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)

        assertEquals(1, getHashCount(resource.sha256, evidenceFolder).blockingGet())
        assertEquals(1, getHashCount(modifiedResource.sha256, evidenceFolder).blockingGet())
    }

    @Test
    fun givenUserSelectsSameDocumentThatSomehowHasDifferentUrisThenEvidenceOnlyAddedOnce() {
        val evidence = TestData.Resource.Documents.EVIDENCE_IMAGE
        val duplicateEvidence = TestData.Resource.Documents.EVIDENCE_IMAGE.fileName("simple_evidence_duplicate.jpg")
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        addContentProviderResources(context, AddDetailedTransactionOtherDetailsFragment::class.java.classLoader, evidence, duplicateEvidence)
        val returnIntent = Intent().also {
            it.data = evidence.uri
        }
        val duplicateEvidenceIntent = Intent().also {
            it.data = duplicateEvidence.uri
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                returnIntent
            )
        )
        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)

        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                duplicateEvidenceIntent
            )
        )
        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)

        val draft = addDetailedTransactionRepository.getDraftValue()
        val evidenceList = draft.evidence
        val evidenceFolder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        assertEquals(1, evidenceList.size)
        assertEquals(1, getHashCount(evidence.sha256, evidenceFolder).blockingGet())
    }

    /**
     * Might delete in favour of TextInputLayout and TextInputEditText maxLength
     */
    @Test
    fun whenUserTypesTextAndTotalLengthOverCharacterLimitThenTextTruncated() {
        val text = String(CharArray(Constants.MAX_NOTE_CODEPOINT_LENGTH - 9) {
            'a'
        }) + "bcdefghij"
        val additionalText = "bb"
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).perform(
            click(), replaceText(text), cursorEndViewAction, typeTextIntoFocusedView(additionalText)
        )
        onView(withId(R.id.edit_text_add_detailed_transaction_note)).check { view, noViewFoundException ->
            val v = view as EditText
            val text = v.text.toString()
            val count = text.codePointCount(0, text.length)
            assertEquals("abcdefghij", text.substring(Constants.MAX_NOTE_CODEPOINT_LENGTH - 10))
            assertEquals(Constants.MAX_NOTE_CODEPOINT_LENGTH, count)
        }
    }

    @Test //TODO Disable button here and enforce max constraint at repository with test
    fun givenEvidenceThresholdLimitReachedWhenAddMoreThenFail() {
        val resources = arrayOf(TestData.Resource.Images.BREAD,
            TestData.Resource.Images.DUMBBELLS_1,
            TestData.Resource.Images.DUMBBELLS_2,
            TestData.Resource.Images.DUMBBELLS_3,
            TestData.Resource.Images.TOOTHBRUSH,
            TestData.Resource.Images.WALL_CLOCK,
            TestData.Resource.Images.SHIRT,
            TestData.Resource.Images.TRAMPOLINE,
            TestData.Resource.Images.LOTION,
            TestData.Resource.Images.HAMMER)
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
            TestData.Resource.Images.HAMMER,
            TestData.Resource.Documents.EVIDENCE_IMAGE,
        )
        val draftFolder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        val draftImages = resources.map { r ->
            val f = File(draftFolder, r.fileName)
            copyResourceToFile(classLoader, r.resourceName, f)
            AddEditTransactionFile(null, f.toUri(), "image/jpeg", r.sha256, 0L)
        }
        var draft = addDetailedTransactionRepository.getDraftValue()
        draft = draft.copy(evidence = draftImages)
        app.saveDraft(draft).subscribeOn(Schedulers.io()).blockingSubscribe()
        addDetailedTransactionRepository.restoreDraft().subscribeOn(Schedulers.io()).blockingSubscribe()


        val returnIntent = Intent().also {
            it.data = TestData.Resource.Documents.EVIDENCE_IMAGE.uri
        }

        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, returnIntent
            )
        )

        onView(withId(R.id.text_view_add_detailed_transaction_add_evidence)).perform(click())
        Thread.sleep(AddDetailedTransactionActivityTest.SLEEP_DURATION)
        val newDraft = addDetailedTransactionRepository.getDraftValue()
        assertEquals(Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_EVIDENCE, newDraft.evidence.size)

    }

    @Test
    fun givenModeIsEditThenUseCustomCheckBoxNotVisible() {

        startInEditMode()
        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))
        onView(withId(R.id.check_box_add_detailed_transaction_use_custom_date_time)).check(matches(
            withEffectiveVisibility(ViewMatchers.Visibility.GONE)
        ))
        onView(withId(R.id.linear_layout_add_detailed_transaction_date_time)).check(matches(
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        ))

    }

    @Test
    fun givenModeIsEditWhenRemoveDateClickedThenRemoveDateButtonGoneAndOriginalDateDisplayed() {
        startInEditMode()
        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))

        val customDate = LocalDate.of(2025, 1, 1)
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
//        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        val dateString = dateFormat.format(MOCKED_DATE)
        // Using Repository setTime method to avoid having to open the dialog
        addDetailedTransactionRepository.setDate(customDate)
        onView(withId(R.id.image_view_add_detailed_transaction_custom_date_remove)).perform(click())
        onView(withId(R.id.image_view_add_detailed_transaction_custom_date_remove)).check(matches(
            withEffectiveVisibility(ViewMatchers.Visibility.GONE)
        ))
        onView(withId(R.id.text_view_add_detailed_transaction_custom_date)).check(matches(withText(dateString)))
    }

    @Test
    fun givenModeIsEditWhenRemoveTimeClickedThenRemoveTimeButtonGoneAndOriginalTimeDisplayed() {
        startInEditMode()
        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))

        val customTime = LocalTime.of( 1, 1, 30)
//        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        val timeString = timeFormat.format(MOCKED_TIME)
        // Using Repository setTime method to avoid having to open the dialog
        addDetailedTransactionRepository.setTime(customTime)
        onView(withId(R.id.image_view_add_detailed_transaction_custom_time_remove)).perform(click())
        onView(withId(R.id.image_view_add_detailed_transaction_custom_time_remove)).check(matches(
            withEffectiveVisibility(ViewMatchers.Visibility.GONE)
        ))
        onView(withId(R.id.text_view_add_detailed_transaction_custom_time)).check(matches(withText(timeString)))
    }


    @Test
    fun givenModeIsEditWhenSetCustomDateNotNullThenRemoveDateButtonVisibleAndCustomDateDisplayed() {
        startInEditMode()
        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))

        val customDate = LocalDate.of(2025, 1, 1)
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val dateString = dateFormat.format(customDate)
//        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        // Using Repository setTime method to avoid having to open the dialog
        addDetailedTransactionRepository.setDate(customDate)
        onView(withId(R.id.image_view_add_detailed_transaction_custom_date_remove)).check(matches(
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        ))
        onView(withId(R.id.text_view_add_detailed_transaction_custom_date)).check(matches(withText(dateString)))
    }

    @Test
    fun givenModeIsEditWhenSetCustomTimeNotNullThenRemoveTimeButtonVisibleAndCustomTimeDisplayed() { //TODO Deal with setting time to the future

        startInEditMode()
        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))

        val customTime = LocalTime.of( 1, 1, 30)
//        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        val timeString = timeFormat.format(customTime)
        // Using Repository setTime method to avoid having to open the dialog
        addDetailedTransactionRepository.setTime(customTime)

        onView(withId(R.id.image_view_add_detailed_transaction_custom_time_remove)).check(matches(
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        ))
        onView(withId(R.id.text_view_add_detailed_transaction_custom_time)).check(matches(withText(timeString)))
    }

    @Test
    fun givenTransactionZoneIsNotSameAsSystemZoneWhenLoadEditThenOriginalZoneDisplayedAndNoticeDisplayed() {
        timeHandler.changeZone(ZoneId.of("Pacific/Honolulu"))
        startInEditMode()
        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))
        onView(withId(R.id.text_view_add_detailed_transaction_zone_difference_notice)).check(matches(isDisplayed()))
    }

    @Test
    fun basicAccountChangeTest() {
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val accountId = accountRepository.createAccount(profile.id!!, "British", "GBP").blockingGet()
        LOGGER.debug("Account ID: {}", accountId)
        onView(withId(R.id.spinner_add_detailed_transaction_account)).perform(click())
        onData(allOf(AccountUiIdMatcher(accountId))).perform(click())
        val draft = addDetailedTransactionRepository.getDraftValue()
        assertEquals(accountId, draft.accountId)
    }

    /**
     * Relying on edit mode, so can't use ScenarioRule
     */
    fun startInEditMode() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()


        val intent = Intent(app, AddDetailedTransactionActivity::class.java).also {
            it.putExtra(AddDetailedTransactionActivity.ARG_MODE, "edit")
            it.putExtra(AddDetailedTransactionActivity.ARG_EDIT_TRANSACTION_ID, id)
        }
        val addDetailedTransactionActivityScenario =
            ActivityScenario.launch<AddDetailedTransactionActivity>(intent)
    }

    fun saveBasicTransaction(amount: BigDecimal, categoryStringId: String = "miscellaneous"): Single<Pair<Long, Long>> {
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).blockingGet()
        val accountId = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().firstOrNull()!!.id!!

        val transaction = TestBuilder.defaultTransaction(accountId, amount)
        val id = transactionRepository.addTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryRepository.findByStringId(categoryStringId).subscribeOn(Schedulers.io()).blockingGet()!!
        val item = TestBuilder.defaultTransactionItemBuilder(id, amount, category.id!!).build()
        return transactionItemRepository.addTransactionItem(item).subscribeOn(Schedulers.io()).map { itemId ->
            id to itemId
        }
    }
}