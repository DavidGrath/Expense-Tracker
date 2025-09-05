package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestConstants
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getHashCount
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.test.TestContentProvider
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
import java.io.File
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
    lateinit var evidenceDao: EvidenceDao

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)

        onView(withId(R.id.tab_layout_add_detailed_transaction)).perform(TabLayoutItemClick(1))
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
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
        val folder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
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
        evidenceDao.insertEvidence(EvidenceDb(null, 0, EvidenceDb.Type.DOCUMENT, 0L, resource.sha256, "application/pdf", existingDocument.toUri().toString(), "2025-06-30T08:00:00", "-04:00", "America/New_York"))
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
        val evidenceFolder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")

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
        val evidenceFolder = file(context.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        assertEquals(1, evidenceList.size)
        assertEquals(1, getHashCount(evidence.sha256, evidenceFolder).blockingGet())
    }
}