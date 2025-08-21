package com.davidgrath.expensetracker.repositories

import androidx.core.net.toUri
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.getHashCount
import com.squareup.rx3.idler.Rx3Idler
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionRepositoryTest {


    @Inject
    lateinit var repository: AddDetailedTransactionRepository
    @Inject
    lateinit var categoryDao: CategoryDao
    @Inject
    lateinit var fileHandler: DraftFileHandler

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            RxJavaPlugins.setInitIoSchedulerHandler(Rx3Idler.create("Robolectric Rx3 Handler"))
        }
    }

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    fun givenDetailedTransactionInDraftWhenDoneThenDraftFileNoLongerExists() {
        val context = RuntimeEnvironment.getApplication()
        val folder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(folder, Constants.DRAFT_FILE_NAME)
        fileHandler.createDraft()
        //Enter valid input

        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val draft = AddDetailedTransactionDraft(items = listOf(AddTransactionItem(0, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")))

        fileHandler.saveDraft(draft)
        repository.finishTransaction().blockingSubscribe()
        assertFalse(file.exists())
    }

    @Test
    fun givenImagesInDraftWhenDoneThenImagesNotInDraftAndImagesInMainFolder() {
        val context = RuntimeEnvironment.getApplication()
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftFolder = File(context.filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftImagesFolder = File(draftFolder, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainFolder = File(context.filesDir, Constants.FOLDER_NAME_DATA)
        val mainImagesFolder = File(mainFolder, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val draftImage = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, TestData.Resource.Images.BREAD.resourceName, draftImage)
        val uri = draftImage.toUri()
        val map = mapOf(uri to TestData.Resource.Images.BREAD.sha256)
        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val draft = AddDetailedTransactionDraft(items =
        listOf(AddTransactionItem(0, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")), imageHashes = map)
        fileHandler.createDraft()
        fileHandler.saveDraft(draft)
        addContentProviderResources(context, classLoader, TestData.Resource.Images.BREAD)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(TestData.Resource.Images.BREAD.sha256, draftImagesFolder).blockingGet())
        assertEquals(1, getHashCount(TestData.Resource.Images.BREAD.sha256, mainImagesFolder).blockingGet())
    }

    @Test
    @Ignore("I need to consider how deleting works eventually")
    fun givenImageInDraftNotUsedWhenDoneThenImageNotInMainFolder() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenDocumentInDraftWhenDoneThenDocumentNotInDraftAndDocumentInMainFolder() {

    }
}