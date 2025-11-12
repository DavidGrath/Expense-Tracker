package com.davidgrath.expensetracker.db.dao

import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.TransactionBuilder
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivityTest
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class ImageDaoTest {

    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var imageDao: ImageDao
    @Inject
    lateinit var itemImagesDao: TransactionItemImagesDao
    @Inject
    lateinit var expenseTrackerDatabase: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: TestExpenseTracker
    lateinit var dataBuilder: DataBuilder

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<TestExpenseTracker>()
        (app.appComponent as TestComponent).inject(this)
        dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
    }

    @Test
    fun getImageWithStatsTest() {
        val totalTransactions = 2L
        val totalItems = 3L

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val imageId = saveImageToDevice(TestData.Resource.Images.BREAD).subscribeOn(Schedulers.io()).blockingGet()
        val imageId2 = saveImageToDevice(TestData.Resource.Images.TOOTHBRUSH, "toothbrush.jpg").subscribeOn(Schedulers.io()).blockingGet()
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())

        val id1 = dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .commit().first()

        var items = transactionItemDao.getAllByTransactionIdSingle(id1).subscribeOn(Schedulers.io()).blockingGet()
        itemImagesDao.insertItemImage(TransactionItemImagesDb(null, items[0].id!!, imageId, dateTimeString, offset, zone)).subscribeOn(Schedulers.io()).blockingSubscribe()
        itemImagesDao.insertItemImage(TransactionItemImagesDb(null, items[0].id!!, imageId2, dateTimeString, offset, zone)).subscribeOn(Schedulers.io()).blockingSubscribe()

        val id2 = dataBuilder.createTransaction()
            .debitOrCredit(false)
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withItem("Description 2", "miscellaneous", BigDecimal(100))
            .commit().first()

        items = transactionItemDao.getAllByTransactionIdSingle(id2).subscribeOn(Schedulers.io()).blockingGet()
        itemImagesDao.insertItemImage(TransactionItemImagesDb(null, items[0].id!!, imageId, dateTimeString, offset, zone)).subscribeOn(Schedulers.io()).blockingSubscribe()
        itemImagesDao.insertItemImage(TransactionItemImagesDb(null, items[1].id!!, imageId, dateTimeString, offset, zone)).subscribeOn(Schedulers.io()).blockingSubscribe()

//        val expectedTotalSize = 686_112L // Should I bother adding size to the TestData.Resource class?
        val size = imageDao.storageSumSingle(profile.id!!).subscribeOn(Schedulers.io()).blockingGet()
        val count = imageDao.countAllSingle(profile.id!!).subscribeOn(Schedulers.io()).blockingGet()
        val stats = imageDao.getImageStatsSingle(imageId).subscribeOn(Schedulers.io()).blockingGet()
        val stats2 = imageDao.getImageStatsSingle(imageId2).subscribeOn(Schedulers.io()).blockingGet()
        LOGGER.debug("stats: {}", stats)
        assertEquals(2L, count)
//        assertEquals(expectedTotalSize, size)
        assertEquals(totalItems, stats.itemCount)
        assertEquals(totalTransactions, stats.transactionCount)
        assertEquals(1L, stats2.itemCount)
        assertEquals(1L, stats2.transactionCount)

    }

    fun saveImageToDevice(resource: TestData.Resource, filename: String = "45402cd3-2452-4804-981a-7ea5515dec74.jpg"): Single<Long> {
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val mainImage = File(mainImagesFolder, filename)
        if(mainImage.exists()) {
            throw Exception("Image already exists on device")
        }
        val classLoader = AddDetailedTransactionActivityTest::class.java.classLoader
        copyResourceToFile(classLoader, resource.resourceName, mainImage)
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val image = ImageDb(null, profile.id!!, 0L, resource.sha256, "image/jpeg", mainImage.toUri().toString(), dateTimeString, offset, zone)
        return imageDao.insertImage(image).subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageDaoTest::class.java)
    }
}