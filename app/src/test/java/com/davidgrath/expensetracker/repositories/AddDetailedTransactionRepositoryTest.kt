package com.davidgrath.expensetracker.repositories

import android.net.Uri
import androidx.core.net.toUri
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.addContentProviderResources
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.copyResourceToFile
import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import com.davidgrath.expensetracker.entities.ui.AddEditDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.getHashCount
import com.davidgrath.expensetracker.test.TestContentProvider
import com.squareup.rx3.idler.Rx3Idler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
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
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var imageDao: ImageDao
    @Inject
    lateinit var itemImagesDao: TransactionItemImagesDao
    @Inject
    lateinit var evidenceDao: EvidenceDao
    @Inject
    lateinit var database: ExpenseTrackerDatabase
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var transactionItemRepository: TransactionItemRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var profileRepository: ProfileRepository

    lateinit var app: ExpenseTracker

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            RxJavaPlugins.setInitIoSchedulerHandler(Rx3Idler.create("Robolectric Rx3 Handler"))
        }

        val MOCKED_DATE = LocalDate.of(2025, 6, 30)
        val MOCKED_TIME = LocalTime.of(8, 0)
    }

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
    }

    @After
    fun tearDown() {
        app.filesDir.deleteRecursively()
        Single.fromCallable { database.clearAllTables() }.subscribeOn(Schedulers.io()).blockingSubscribe()
    }

    @Test
    fun givenDetailedTransactionInDraftWhenDoneThenDraftFileNoLongerExists() {
        val folder = File(app.filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(folder, Constants.DRAFT_FILE_NAME)
        fileHandler.createDraft()
        //Enter valid input

        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val draft = AddEditDetailedTransactionDraft(items = listOf(AddTransactionItem(0, null, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")), accountId = accountId)

        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertFalse(file.exists())
    }

    @Test
    fun givenImagesInDraftWhenDoneThenImagesNotInDraftAndImagesInMainFolder() {
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val draftImage = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, TestData.Resource.Images.BREAD.resourceName, draftImage)
        val uri = draftImage.toUri()
        val resource = TestData.Resource.Images.BREAD
        val map = mapOf(resource.sha256 to uri)
        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val draft = AddEditDetailedTransactionDraft(items =
        listOf(AddTransactionItem(0, null, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description", images = listOf(
            AddEditTransactionFile(null, uri, "image/jpeg", resource.sha256, 0L)
        ))), imageHashes = map, accountId = accountId)
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder).blockingGet())
        assertEquals(1, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
    }

    @Test
    fun givenImageInDraftNotUsedWhenDoneThenImageNotInMainFolder() {
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()

        val draftEvidence = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val bread = TestData.Resource.Images.BREAD
        copyResourceToFile(classLoader, bread.resourceName, draftEvidence)
        getHashCount(bread.sha256, draftImagesFolder).blockingGet()
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val map = mapOf(bread.sha256 to draftEvidence.toUri())
        val draft = AddEditDetailedTransactionDraft(listOf(basicItem(0)), imageHashes = map, accountId = accountId)
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()

        assertEquals(0, getHashCount(bread.sha256, draftImagesFolder).blockingGet())
        assertEquals(0, getHashCount(bread.sha256, mainImagesFolder).blockingGet())
    }

    @Test
    fun basicEditTest() {
        //Save Basic Transaction
        val (id, itemId, item2Id) = saveBasicTwoItemTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        val firstEvidence = TestData.Resource.Documents.SIMPLE_EVIDENCE
        val evidenceDir = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        saveEvidenceToDevice(id, firstEvidence).blockingSubscribe()
        var transaction = transactionRepository.getTransactionByIdSingle(id).blockingGet()
        var items = transactionItemRepository.getTransactionItemsSingle(id).blockingGet()
        assertEqualsBD(BigDecimal(20), transaction.amount)
        assertEquals(null, transaction.note)
        assertEquals("Description", items[0].description)
        assertEquals(1, getHashCount(firstEvidence.sha256, evidenceDir).blockingGet())
        assertFalse(transaction.debitOrCredit)
        assertEquals("USD", transaction.currencyCode)
        //Load Draft

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        //Edit existing item
        val firstItem = repository.getDraftValue().items[0]
        val food = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val foodUi = categoryDbToCategoryUi(food)
        repository.changeItem(0,
            firstItem.copy(description = "Edited", amount = BigDecimal(20), category = foodUi,
                brand = "Generic")
        )

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val bread = TestData.Resource.Images.BREAD
        val secondEvidence = TestData.Resource.Documents.EVIDENCE_IMAGE
        addContentProviderResources(app, classLoader, bread, secondEvidence)
        repository.addImageToItem(firstItem.id, bread.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        //Remove item
        repository.deleteItem(1)

        //Add Valid Item

        repository.addItem()
        val secondItem = repository.getDraftValue().items[1]
        repository.changeItem(1, secondItem.copy(description = "Dumbbells", amount = BigDecimal(50), category = foodUi))

        val note = "This is a test"
        repository.setNote(note) //TODO Oh yeah, I haven't enforced the codepoint constraint at this level, only the UI level
        repository.removeEvidence(0)
        repository.addEvidence(secondEvidence.uri, "image/jpeg").blockingSubscribe()

        //Edit Account
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val accountId = accountRepository.createAccount(profile.id!!, "British", "GBP").blockingGet()
        repository.setAccount(accountId)

        //Change default debit to credit
        repository.toggleDebitOrCredit()

        //TODO Date/Time
        repository.finishTransaction().blockingSubscribe()

        val total = BigDecimal(70)

        transaction = transactionRepository.getTransactionByIdSingle(id).blockingGet()
        items = transactionItemRepository.getTransactionItemsSingle(id).blockingGet()
        val images = imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet()
        val evidenceList = evidenceDao.getAllByTransactionIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()

        assertEquals(2, items.size)
        assertEqualsBD(total, transaction.amount)
        assertEquals(note, transaction.note)
        assertEquals("Edited", items[0].description)
        assertEqualsBD(BigDecimal(20), items[0].amount)
        assertEquals("Generic", items[0].brand)
        assertEquals(food.id, items[0].primaryCategoryId)
//        assertEquals(null, items[0].referenceNumber) //TODO Reference number
//        assertEquals(null, items[0].variation) //TODO Variation
        assertEquals(bread.sha256, images[0].sha256)
        assertEquals(1, evidenceList.size)
        assertEquals(0, getHashCount(firstEvidence.sha256, evidenceDir).blockingGet())
        assertEquals(1, getHashCount(secondEvidence.sha256, evidenceDir).blockingGet())
        assertEquals("GBP", transaction.currencyCode)
        assertEquals(true , transaction.debitOrCredit)
    }
    @Test // A //TODO These letters are from a spreadsheet I drafted. Transform that to an AsciiDoc table in this repo
    fun givenImageInEditRemovedAndImageNotLinkedToAnyOtherItemsWhenSaveThenImageRemovedFromDevice() {

        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val resource = TestData.Resource.Images.BREAD
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val imageId = saveImageToDevice(resource).blockingGet()
        val itemImage = TransactionItemImagesDb(null, itemId, imageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.deleteItemImage(0, 0)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
        assertEquals(null, imageDao.findBySha256(resource.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(0, imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }


    @Test // C
    fun givenModeIsEditAndItemImageDeletedWhenSameImageAddedThenItemImageNoLongerInDeletedList() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val bread = TestData.Resource.Images.BREAD
        addContentProviderResources(app, classLoader, bread)

        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val breadImageId = saveImageToDevice(bread).blockingGet()

        val itemImage = TransactionItemImagesDb(null, itemId, breadImageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.deleteItemImage(0, 0)
        val breadFile = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")

        repository.addImageToItem(0, bread.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        val deletedImages = repository.getDraftValue().items.find { it.dbId == itemId }!!.deletedDbImages
        val images = repository.getDraftValue().items.find { it.dbId == itemId }!!.images
        assertFalse(deletedImages.map { it.sha256 }.contains(bread.sha256))
        assertTrue(images.first { it.uri == breadFile.toUri() }.dbId != null)
        assertTrue(images.first { it.uri == breadFile.toUri() }.dbIsLinked)
    }





    @Test
    fun givenModeIsEditAndItemImageRemovedAndItemImageWasOnlyUsageOnDeviceWhenAddSameImageToDifferentItemThenImageStaysOnDevice() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val bread = TestData.Resource.Images.BREAD
        addContentProviderResources(app, classLoader, bread)

        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val draftImage = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, bread.resourceName, draftImage)

        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val breadImageId = saveImageToDevice(bread).blockingGet()

        val itemImage = TransactionItemImagesDb(null, itemId, breadImageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        repository.deleteItemImage(0, 0)

        repository.addItem()
        repository.changeItem(1, repository.getDraftValue().items[1].copy(amount = BigDecimal.TEN, description = "Desc"))
        repository.addImageToItem(1, bread.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertEquals(1, getHashCount(bread.sha256, mainImagesFolder).blockingGet())
        assertNotNull(imageDao.findBySha256(bread.sha256).subscribeOn(Schedulers.io()).blockingGet())
        val otherItemId = transactionItemDao.getAllByTransactionIdSingle(id).subscribeOn(Schedulers.io()).blockingGet().find { it.description == "Desc" }!!.id!!
        assertEquals(1, imageDao.getAllByItemSingle(otherItemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test //D
    fun givenModeIsEditAndItemImageNotInDbWhenSaveEditThenImageExistsOnDeviceAndLinkExists() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val bread = TestData.Resource.Images.BREAD
        addContentProviderResources(app, classLoader, bread)

        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val draftEvidence = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(AddDetailedTransactionRepositoryTest::class.java.classLoader, bread.resourceName, draftEvidence)
        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        repository.addImageToItem(0, bread.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.finishTransaction().blockingSubscribe()
        assertEquals(1, getHashCount(bread.sha256, mainImagesFolder).blockingGet())
        assertNotNull(imageDao.findBySha256(bread.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(1, imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test //E
    fun givenModeIsEditAndItemImageInDbAndImageNotLinkedWhenSaveEditThenLinkExists() {
        val total = BigDecimal(35)
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val transaction = TestBuilder.defaultTransaction(accountId, total)
        val id = transactionDao.insertTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryDao.findByStringId("miscellaneous").subscribeOn(Schedulers.io()).blockingGet()!!
        val item = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal(20), category.id!!).build()
        val otherItem = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal(15), category.id!!).build()
        val itemId = transactionItemDao.insertTransactionItem(item).subscribeOn(Schedulers.io()).blockingGet()
        val otherItemId = transactionItemDao.insertTransactionItem(otherItem).subscribeOn(Schedulers.io()).blockingGet()


        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val bread = TestData.Resource.Images.BREAD
        addContentProviderResources(app, classLoader, bread)

        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val mainImage = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val breadImageId = saveImageToDevice(bread).blockingGet()

        val itemImage = TransactionItemImagesDb(null, itemId, breadImageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()


        repository.addImageToItem(1, bread.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertEquals(1, getHashCount(bread.sha256, mainImagesFolder).blockingGet())
        assertNotNull(imageDao.findBySha256(bread.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(1, imageDao.getAllByItemSingle(otherItemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }


    @Test //G
    fun givenModeIsEditAndImageInDbAndItemImageNotLinkedAndImageMarkedDeletedAndImageNotLinkedToOtherTransactionWhenSaveEditThenImageRemovedFromDevice() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val resource = TestData.Resource.Images.BREAD
        addContentProviderResources(app, classLoader, resource)

        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val resourceFile = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val imageId = saveImageToDevice(resource).blockingGet()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.deleteItemImage(0, 0)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
        assertEquals(null, imageDao.findBySha256(resource.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(0, imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test // H
    fun givenModeIsEditAndItemImageNotLinkedAndImageMarkedDeletedAndImageLinkedToOtherTransactionWhenSaveEditThenImageStaysOnDevice() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        val otherTransaction = saveBasicTransaction(BigDecimal(20), "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val resource = TestData.Resource.Images.BREAD
        addContentProviderResources(app, classLoader, resource)
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val resourceFile = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val imageId = saveImageToDevice(resource).blockingGet()

        val otherItemImage = TransactionItemImagesDb(null, otherTransaction.second, imageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(otherItemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.changeItem(0, repository.getDraftValue().items[0].copy(amount = BigDecimal.TEN, description = "Description"))

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.deleteItemImage(0, 0)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(1, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
        assertNotNull(imageDao.findBySha256(resource.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(0, imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test // I
    fun givenModeIsEditAndItemImageLinkedAndImageMarkedDeletedImageNotLinkedToOtherTransactionWhenSaveEditThenImageRemovedFromDevice() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val resource = TestData.Resource.Images.BREAD
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
//        val resourceFile = File(mainImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val imageId = saveImageToDevice(resource).blockingGet()
        val itemImage = TransactionItemImagesDb(null, itemId, imageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        repository.deleteItemImage(0, 0)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
        assertEquals(null, imageDao.findBySha256(resource.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(0, imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test // J
    fun givenModeIsEditAndItemImageLinkedAndImageMarkedDeletedAndImageLinkedToOtherTransactionWhenSaveEditThenImageStaysOnDevice() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        val otherTransaction = saveBasicTransaction(BigDecimal(20), "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val resource = TestData.Resource.Images.BREAD
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val imageId = saveImageToDevice(resource).blockingGet()

        val itemImage = TransactionItemImagesDb(null, itemId, imageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()
        val otherItemImage = TransactionItemImagesDb(null, otherTransaction.second, imageId, dateTimeString, offset, zone)
        itemImagesDao.insertItemImage(otherItemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.changeItem(0, repository.getDraftValue().items[0].copy(amount = BigDecimal.TEN, description = "Description"))

        repository.deleteItemImage(0, 0)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(1, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
        assertNotNull(imageDao.findBySha256(resource.sha256).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(0, imageDao.getAllByItemSingle(itemId).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test
    fun givenModeIsEditWhenEvidenceDeletedAndSameItemAddedThenEvidenceNoLongerInDeletedList() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        addContentProviderResources(app, classLoader, resource)

        val mainDocumentsFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainDocumentsFolder.mkdirs()
        val mainEvidence = File(mainDocumentsFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val evidenceId = saveEvidenceToDevice(id, resource).blockingGet()

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.removeEvidence(0)
        repository.addEvidence(resource.uri, "image/jpeg").blockingSubscribe()
        val draft = repository.getDraftValue()
        val deleted = draft.deletedDbEvidence
        val evidence = draft.evidence
        assertNull(deleted.find { it.sha256 == resource.sha256 })
        assertNotNull(evidence.find { it.sha256 == resource.sha256 })
        assertEquals(evidenceId, evidence.find { it.sha256 == resource.sha256 }!!.dbId)
    }

    @Test
    fun givenDocumentInDraftNotUsedWhenDoneThenDocumentNotInMainFolder() {
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        draftEvidenceFolder.mkdirs()
        val mainEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainEvidenceFolder.mkdirs()
        val draftEvidence = File(draftEvidenceFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, TestData.Resource.Documents.EVIDENCE_IMAGE.resourceName, draftEvidence)
        val uri = draftEvidence.toUri()
        val map = mapOf(TestData.Resource.Documents.EVIDENCE_IMAGE.sha256 to uri)
        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val draft = AddEditDetailedTransactionDraft(items =
        listOf(AddTransactionItem(0, null, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")), evidenceHashes = map, evidence = emptyList(), accountId = accountId)
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(TestData.Resource.Documents.EVIDENCE_IMAGE.sha256, draftEvidenceFolder).blockingGet())
        assertEquals(0, getHashCount(TestData.Resource.Documents.EVIDENCE_IMAGE.sha256, mainEvidenceFolder).blockingGet())
    }

    @Test
    fun givenModeIsEditAndEvidenceInDbWhenEvidenceMarkedDeletedWhenFinishTransactionThenEvidenceRemovedFromDevice() {

        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN).subscribeOn(Schedulers.io()).blockingGet()
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val mainEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainEvidenceFolder.mkdirs()
        saveEvidenceToDevice(id, resource).subscribeOn(Schedulers.io()).blockingSubscribe()
        assertEquals(1, evidenceDao.getAllByTransactionIdSingle(id).subscribeOn(Schedulers.io()).blockingGet().size)

        repository.setMode("edit")
        repository.initializeEdit(id).blockingSubscribe()
        repository.removeEvidence(0)
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(resource.sha256, mainEvidenceFolder).blockingGet())
        assertEquals(0, evidenceDao.getAllByTransactionIdSingle(id).subscribeOn(Schedulers.io()).blockingGet().size)
    }

    @Test
    fun givenDocumentExistsInOtherTransactionWhenAddSameDocumentThenDuplicateDocumentFilesExist() {

        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN).subscribeOn(Schedulers.io()).blockingGet()
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        draftEvidenceFolder.mkdirs()
        val mainEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainEvidenceFolder.mkdirs()
        val draftEvidence = File(draftEvidenceFolder, "evidence.jpg")
        copyResourceToFile(classLoader, resource.resourceName, draftEvidence)
        saveEvidenceToDevice(id, resource).subscribeOn(Schedulers.io()).blockingGet()

        val uri = draftEvidence.toUri()
        val map = mapOf(resource.sha256 to uri)
        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val draft = AddEditDetailedTransactionDraft(items =
        listOf(AddTransactionItem(0, null, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")), evidenceHashes = map, evidence = listOf(
            AddEditTransactionFile(null, uri, "image/jpeg", resource.sha256, 0L)
        ), accountId = accountId
        )
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertEquals(2, getHashCount(resource.sha256, mainEvidenceFolder).blockingGet())
    }

    @Test
    fun givenModeIsAddWhenAddSameEvidenceToTransactionThenEvidenceOnlyExistsOnce() {
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val draftEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        draftEvidenceFolder.mkdirs()

        val draftEvidence = File(draftEvidenceFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        copyResourceToFile(classLoader, resource.resourceName, draftEvidence)
        addContentProviderResources(app, classLoader, resource)
        val uri = draftEvidence.toUri()
        val map = mapOf(resource.sha256 to uri)
        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val draft = AddEditDetailedTransactionDraft(items =
        listOf(AddTransactionItem(0, null, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")), evidenceHashes = map, evidence = listOf(
            AddEditTransactionFile(null, uri, "image/jpeg", resource.sha256, 0L)),
            accountId = accountId
        )
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()
        repository.addEvidence(resource.uri, "image/jpeg").blockingSubscribe()
        val evidence = repository.getDraftValue().evidence
        assertEquals(1, evidence.size)
    }

    @Test
    fun givenModeIsEditWhenAddSameEvidenceToTransactionThenEvidenceOnlyExistsOnce() {


        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN).subscribeOn(Schedulers.io()).blockingGet()
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        draftEvidenceFolder.mkdirs()
        val mainEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainEvidenceFolder.mkdirs()
        val draftEvidence = File(draftEvidenceFolder, "evidence.jpg")
        copyResourceToFile(classLoader, resource.resourceName, draftEvidence)
        addContentProviderResources(app, classLoader, resource)
        saveEvidenceToDevice(id, resource).subscribeOn(Schedulers.io()).blockingGet()
        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()
        repository.addEvidence(resource.uri, "image/jpeg").blockingSubscribe()
        val evidence = repository.getDraftValue().evidence
        
        assertEquals(1, evidence.filter { it.sha256 == resource.sha256 }.size)
        assertEquals(1, getHashCount(resource.sha256, draftEvidenceFolder).blockingGet())
    }

    @Test
    fun givenModeIsDraftAndImagesSavedThroughPreviousEditWhenRestoreDraftThenReferencesCorrected() {

        val resource = TestData.Resource.Images.BREAD
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()


        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN).subscribeOn(Schedulers.io()).blockingGet()
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        addContentProviderResources(app, classLoader, resource)

        //Add image to draft transaction
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        //Add image to existing transaction
        repository.setMode("edit")
        repository.initializeEdit(id).blockingSubscribe()
        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder).blockingGet())

        repository.setMode("add")
        repository.restoreDraft().blockingSubscribe()
        val repoDraft = repository.getDraftValue()
        val image = imageDao.findBySha256(resource.sha256).subscribeOn(Schedulers.io()).blockingGet()
        //Assert hashmap updated
        assertEquals(Uri.parse(image!!.uri), repoDraft.imageHashes[resource.sha256])
        assertEquals(Uri.parse(image!!.uri), repoDraft.items[0].images[0].uri)
        //Assert item images have proper ids instead of null
        assertNotNull(repoDraft.items[0].images[0].dbId)
        assertNotNull(image)

    }


    @Test //TODO This test could possibly be broken down, but for now I'm not sure how
    fun givenModeIsDraftAndDraftImagesSavedThroughPreviousEditWhenFinishTransactionThenImageExistsOnlyOnce() {
        val resource = TestData.Resource.Images.BREAD
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()


        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN).subscribeOn(Schedulers.io()).blockingGet()
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        addContentProviderResources(app, classLoader, resource)

        //Add image to draft transaction
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        //Add image to existing transaction
        repository.setMode("edit")
        repository.initializeEdit(id).blockingSubscribe()
        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder).blockingGet())

        repository.setMode("add")
        repository.restoreDraft().blockingSubscribe()
        val repoDraft = repository.getDraftValue()
        assertNotNull(repoDraft.items[0].images[0].dbId)
        repository.finishTransaction().blockingSubscribe()

        assertEquals(1, getHashCount(resource.sha256, mainImagesFolder).blockingGet())
        assertEquals(1, imageDao.getAllSingle().subscribeOn(Schedulers.io()).blockingGet().filter { it.sha256 == resource.sha256 }.size)

    }

    @Test
    fun givenModeIsEditAndDraftExistsAndUnusedImagesPresentWhenFinishTransactionThenUnusedImagesLeftIntact() {
        val resource = TestData.Resource.Images.BREAD
        val otherResource = TestData.Resource.Images.TOOTHBRUSH
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()

        //Add image to draft transaction
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        addContentProviderResources(app, classLoader, resource, otherResource)

        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        //Add other image to existing transaction
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN).subscribeOn(Schedulers.io()).blockingGet()
        repository.setMode("edit")
        repository.initializeEdit(id).blockingSubscribe()
        repository.addImageToItem(0, otherResource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)

        repository.setMode("add")
        repository.restoreDraft().blockingSubscribe()
        assertEquals(0, getHashCount(otherResource.sha256, draftImagesFolder).blockingGet())
        assertEquals(1, getHashCount(resource.sha256, draftImagesFolder).blockingGet())
    }

    @Test
    fun givenDocumentInDraftWhenDoneThenDocumentNotInDraftAndDocumentInMainFolder() {

        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE

        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        val draftEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        draftEvidenceFolder.mkdirs()
        val mainEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainEvidenceFolder.mkdirs()
        val draftEvidence = File(draftEvidenceFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        copyResourceToFile(classLoader, resource.resourceName, draftEvidence)
        val uri = draftEvidence.toUri()
        val map = mapOf(resource.sha256 to uri)
        val category = categoryDao.findByStringId("food").subscribeOn(Schedulers.io()).blockingGet()!!
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val draft = AddEditDetailedTransactionDraft(items =
        listOf(AddTransactionItem(0, null, categoryDbToCategoryUi(category), BigDecimal(300).setScale(2, RoundingMode.HALF_UP), "Description")), evidenceHashes = map, evidence = listOf(
            AddEditTransactionFile(null, uri, "image/jpeg", resource.sha256, 0L)
        ), accountId = accountId
        )
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()
        repository.finishTransaction().blockingSubscribe()
        assertEquals(0, getHashCount(resource.sha256, draftEvidenceFolder).blockingGet())
        assertEquals(1, getHashCount(resource.sha256, mainEvidenceFolder).blockingGet())
    }

    @Test
    fun givenUserSelectsSameImageMultipleTimesForSameItemThenImageOnlyAddedOnce() {
        val resource = TestData.Resource.Images.BREAD
        addContentProviderResources(app, AddDetailedTransactionRepositoryTest::class.java.classLoader, resource)
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        val repoDraft = repository.getDraftValue()
        val itemImages = repoDraft.items[0].images
        assertEquals(1, itemImages.size)
    }

    @Test
    fun givenUserSelectsSameImageThatSomehowHasDifferentUrisThenImageOnlyAddedOnce() {
        val resource = TestData.Resource.Images.BREAD
        val duplicateResource = resource.fileName("bread_duplicate.jpg")

        addContentProviderResources(app, AddDetailedTransactionRepositoryTest::class.java.classLoader, resource, duplicateResource)
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.addImageToItem(0, duplicateResource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        val repoDraft = repository.getDraftValue()
        val itemImages = repoDraft.items[0].images
        assertEquals(1, itemImages.size)
    }

    @Test
    fun givenUserSelectsSameImageMultipleTimesAcrossMultipleItemsThenImageOnlyCopiedOnceToDraftStorage() {

        val resource = TestData.Resource.Images.BREAD
        addContentProviderResources(app, AddDetailedTransactionRepositoryTest::class.java.classLoader, resource)
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        repository.addItem()
        repository.addImageToItem(1, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        val items = repository.getDraftValue().items
        assertEquals(1, items[0].images.size)
        assertEquals(1, items[1].images.size)
        assertEquals(1, getHashCount(resource.sha256, draftImagesFolder).blockingGet())
    }

    @Test
    fun givenUserSelectsImageThatHasBeenSavedToMainStorageWhenSelectImageThenImageNotCopiedToDraftStorage() {
        val resource = TestData.Resource.Images.BREAD
        addContentProviderResources(app, AddDetailedTransactionRepositoryTest::class.java.classLoader, resource)

        saveImageToDevice(resource).blockingSubscribe()
        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)

        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        val repoDraft = repository.getDraftValue()
        val items = repoDraft.items
        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder).blockingGet())
        assertEquals(1, items[0].images.size)
        assertNotNull(items[0].images[0].dbId)
    }

    @Test
    fun givenExternalImageWasModifiedAndOriginalImageAddedToDraftWhenAddImageThenNewImageExistsInDraftStorage() {
        val resource = TestData.Resource.Images.BREAD
        val modifiedResource = resource.copy(resourceName = TestData.Resource.Images.TOOTHBRUSH.resourceName)
        addContentProviderResources(app, AddDetailedTransactionRepositoryTest::class.java.classLoader, resource)

        val draftImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)

        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        repository.addImageToItem(0, resource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()
        addContentProviderResources(app, AddDetailedTransactionRepositoryTest::class.java.classLoader, modifiedResource)
        repository.addImageToItem(0, modifiedResource.uri, "image/jpeg").subscribeOn(Schedulers.io()).blockingSubscribe()

        assertEquals(1, getHashCount(resource.sha256, draftImagesFolder).blockingGet())
        assertEquals(1, getHashCount(modifiedResource.sha256, draftImagesFolder).blockingGet())
    }

    //region Date and Time

    @Test
    fun givenModeIsAddAndDoNotUseCustomTimeAndTransactionHasCustomTimeWhenSaveThenTransactionHasCurrentDateTime() {
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setDate(localDate)
        repository.setTime(localTime)

        repository.finishTransaction().blockingSubscribe()
        val transaction = transactionDao.getByIdSingle(1).subscribeOn(Schedulers.io()).blockingGet()
        val now = LocalDateTime.now(timeAndLocaleHandler.getClock()).truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals(0, now.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsAddAndUseCustomTimeAndDateNotNullAndTimeNotNullWhenSaveThenTransactionHasSpecifiedDateTime() {
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setUseCustomDateTime(true)
        repository.setDate(localDate)
        repository.setTime(localTime)
        repository.finishTransaction().blockingSubscribe()
        val transaction = transactionDao.getByIdSingle(1).subscribeOn(Schedulers.io()).blockingGet()
        val custom = LocalDateTime.parse("2025-01-01T01:00:30.123").truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals("Expected $custom but was $transactionTime",0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsAddAndUseCustomTimeAndDateNotNullAndTimeNullWhenSaveThenTransactionHasSpecifiedDate() {
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setUseCustomDateTime(true)
        repository.setDate(localDate)
        repository.setTime(null)
        repository.finishTransaction().blockingSubscribe()
        val transaction = transactionDao.getByIdSingle(1).subscribeOn(Schedulers.io()).blockingGet()
        val custom = localDate.atTime(MOCKED_TIME).truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals(0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsAddAndUseCustomTimeAndDateNullAndTimeNotNullWhenSaveThenTransactionHasSpecifiedTime() {
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setUseCustomDateTime(true)
        repository.setDate(null)
        repository.setTime(localTime)
        repository.finishTransaction().blockingSubscribe()
        val transaction = transactionDao.getByIdSingle(1).subscribeOn(Schedulers.io()).blockingGet()
        val custom = MOCKED_DATE.atTime(localTime).truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals(0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsAddAndUseCustomTimeAndDateNullAndTimeNullWhenSaveThenTransactionHasDefaultDateTime() {
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setUseCustomDateTime(true)
        repository.setDate(null)
        repository.setTime(null)
        repository.finishTransaction().blockingSubscribe()
        val transaction = transactionDao.getByIdSingle(1).subscribeOn(Schedulers.io()).blockingGet()
        val custom = MOCKED_DATE.atTime(MOCKED_TIME).truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals("Expected $custom but got $transactionTime",0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsEditAndDateNotNullAndTimeNotNullWhenSaveThenTransactionHasNewDateTime() {

        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        var transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()
        val originalDateTime = LocalDate.parse(transaction.datedAt).atTime(LocalTime.parse(transaction.datedAtTime!!))

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setDate(localDate)
        repository.setTime(localTime)
        repository.finishTransaction().blockingSubscribe()

        transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()

        val custom = localDate.atTime(localTime).truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals("Expected $custom but was $transactionTime",0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsEditAndDateNotNullAndTimeNullWhenSaveThenTransactionHasNewDate() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        var transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()
        val originalDateTime = LocalDate.parse(transaction.datedAt).atTime(LocalTime.parse(transaction.datedAtTime!!))

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setDate(localDate)
        repository.setTime(null)
        repository.finishTransaction().blockingSubscribe()

        transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()

        val custom = localDate.atTime(originalDateTime.toLocalTime()).truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals("Expected $custom but was $transactionTime",0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsEditAndDateNullAndTimeNotNullWhenSaveThenTransactionHasNewTime() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        var transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()
        val originalDateTime = LocalDate.parse(transaction.datedAt).atTime(LocalTime.parse(transaction.datedAtTime!!))

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setDate(null)
        repository.setTime(localTime)
        repository.finishTransaction().blockingSubscribe()

        transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()

        val custom = originalDateTime.toLocalDate().atTime(localTime).truncatedTo(ChronoUnit.MINUTES)

        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals("Expected $custom but was $transactionTime",0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsEditAndDateNullAndTimeNullWhenSaveThenTransactionHasOriginalDateTime() {
        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()
        var transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()
        val originalDateTime = LocalDate.parse(transaction.datedAt).atTime(LocalTime.parse(transaction.datedAtTime!!))

        repository.setMode("edit")
        repository.initializeEdit(id).blockingGet()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setDate(null)
        repository.setTime(null)
        repository.finishTransaction().blockingSubscribe()

        transaction = transactionDao.getByIdSingle(id).subscribeOn(Schedulers.io()).blockingGet()

        val custom = originalDateTime.truncatedTo(ChronoUnit.MINUTES)
        val transactionTime = transaction.getDatedLocalDateTime(timeAndLocaleHandler)!!.truncatedTo(ChronoUnit.MINUTES)
        assertEquals("Expected $custom but was $transactionTime",0, custom.compareTo(transactionTime))
    }

    //TODO Continue future-prevention code later

    /*@Test
    fun givenModeIsAnyAndUseCustomDateTimeAndDateNotNullAndTimeNotNullAndDateTimeInFutureWhenSaveThenCurrentTimeUsedInstead() {
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous")
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()
        repository.restoreDraft().blockingSubscribe()

        val localDate = LocalDate.of(2025, 7, 1)
        val localTime = LocalTime.of(1, 0, 30, 123_000_000)
        repository.setUseCustomDateTime(true)
        repository.setDate(localDate)
        repository.setTime(localTime)

        val dateTime = repository.getDraftValue().customDate!!.atTime(repository.getDraftValue().customTime!!).truncatedTo(ChronoUnit.MINUTES)
        val now = MOCKED_DATE.atTime(MOCKED_TIME)
        assertEquals("Expected $now but was $dateTime",0, custom.compareTo(transactionTime))
    }

    @Test
    fun givenModeIsAnyAndUseCustomDateTimeAndTimeNotNullWhenSetDateAndDateTodayThenDateTimeNotInFuture() {

    }*/

    // endregion

    @Test
    @Ignore("Not ready yet")
    fun givenCustomCategoryModifiedWhenRestoreDraftThenDraftCategoryCorrect() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenCustomCategoryDeletedWhenRestoreDraftThenCategoryBecomesMisc() {

    }

    @Test
    fun givenAccountDeletedWhenRestoreDraftThenDefaultAccountUsed() {
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val defaultAccountId = getDefaultAccountId(profileRepository, accountRepository)
        val accountId = accountRepository.createAccount(profile.id!!, "British", "GBP").blockingGet()
        val draft = buildDraft(BigDecimal(100), "Desc", "miscellaneous").copy(accountId = accountId)
        fileHandler.createDraft().blockingSubscribe()
        fileHandler.saveDraft(draft).blockingSubscribe()

        accountRepository.deleteAccount(profile.id!!, accountId).blockingSubscribe()

        repository.restoreDraft().blockingSubscribe()
        val repoDraft = repository.getDraftValue()
        assertEquals(defaultAccountId, repoDraft.accountId)
    }

    @Test
    fun givenOneAccountLeftForProfileWhenDeleteThenFail() { //TODO Care about cascading delete later
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val defaultAccountId = getDefaultAccountId(profileRepository, accountRepository)

        val result = accountRepository.deleteAccount(profile.id!!, defaultAccountId).onErrorReturn { -1 } .blockingGet()

        assertEquals(-1, result)
        assertEquals(1, accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().size)
    }


    @Test
    @Ignore("Save for later")
    fun undoTest() {

    }
    @Test
    @Ignore("Save for later")

    fun undoDeleteItemImageTest() {

    }

    @Test
    @Ignore("Save for later")
    fun undoDeleteEvidenceTest() {

    }

    /**
     * A basic item with sensible defaults
     */
    fun basicItem(id: Int): AddTransactionItem {
        val category = categoryDao.findByStringId("miscellaneous").subscribeOn(Schedulers.io()).blockingGet()!!
        return AddTransactionItem(id, null, categoryDbToCategoryUi(category), BigDecimal.TEN, "Description")
    }

    fun saveImageToDevice(resource: TestData.Resource, filename: String = "45402cd3-2452-4804-981a-7ea5515dec74.jpg"): Single<Long> {
        val mainImagesFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val mainImage = File(mainImagesFolder, filename)
        if(mainImage.exists()) {
            throw Exception("Image already exists on device")
        }
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        copyResourceToFile(classLoader, resource.resourceName, mainImage)
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val image = ImageDb(null, 0L, resource.sha256, "image/jpeg", mainImage.toUri().toString(), dateTimeString, offset, zone)
        return imageDao.insertImage(image).subscribeOn(Schedulers.io())
    }

    fun saveEvidenceToDevice(transactionId: Long, resource: TestData.Resource, filename: String = "45402cd3-2452-4804-981a-7ea5515dec74.jpg"): Single<Long> {
        val mainEvidenceFolder = file(app.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, "2025", "06", "30")
        mainEvidenceFolder.mkdirs()
        val mainEvidence = File(mainEvidenceFolder, filename)
        if(mainEvidence.exists()) {
            throw Exception("Evidence already exists on device")
        }
        val classLoader = AddDetailedTransactionRepositoryTest::class.java.classLoader
        copyResourceToFile(classLoader, resource.resourceName, mainEvidence)
        val (dateTimeString, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val evidence = EvidenceDb(null, transactionId, 0L, resource.sha256, "image/jpeg", mainEvidence.toUri().toString(), dateTimeString, offset, zone)
        return evidenceDao.insertEvidence(evidence).subscribeOn(Schedulers.io())
    }

    fun saveBasicTransaction(amount: BigDecimal, categoryStringId: String = "miscellaneous"): Single<Pair<Long, Long>> {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val transaction = TestBuilder.defaultTransaction(accountId, amount)
        val id = transactionDao.insertTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryDao.findByStringId(categoryStringId).subscribeOn(Schedulers.io()).blockingGet()!!
        val item = TestBuilder.defaultTransactionItemBuilder(id, amount, category.id!!).build()
        return transactionItemDao.insertTransactionItem(item).subscribeOn(Schedulers.io()).map { itemId ->
            id to itemId
        }
    }

    /**
     * Total amount is `amount` * 2
     */
    fun saveBasicTwoItemTransaction(amount: BigDecimal, categoryStringId: String = "miscellaneous"): Single<Triple<Long, Long, Long>> {
        val transaction = TestBuilder.defaultTransactionBuilder(getDefaultAccountId(profileRepository, accountRepository), amount.times(BigDecimal(2))).debitOrCredit(false).build()
        val id = transactionDao.insertTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryDao.findByStringId(categoryStringId).subscribeOn(Schedulers.io()).blockingGet()!!
        val item = TestBuilder.defaultTransactionItemBuilder(id, amount, category.id!!).build()
        val item2 = TestBuilder.defaultTransactionItemBuilder(id, amount, category.id!!).build()
        return transactionItemDao.insertTransactionItem(item).subscribeOn(Schedulers.io()).flatMap { itemId ->
            transactionItemDao.insertTransactionItem(item2)
                .map { item2Id ->
                    Triple(id, itemId, item2Id)
                }
        }
    }

    fun saveDraftImage() {

    }

    fun saveMainImage() {

    }

    fun buildDraft(amount: BigDecimal, description: String, categoryStringId: String, evidence: List<AddEditTransactionFile> = emptyList(), note: String = ""): AddEditDetailedTransactionDraft {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val category = categoryDao.findByStringId(categoryStringId).subscribeOn(Schedulers.io()).blockingGet()!!
        val categoryUi = categoryDbToCategoryUi(category)
        val evidenceMap = mutableMapOf<String, Uri>()
        for(resource in evidence) {
            evidenceMap[resource.sha256] = resource.uri
        }
        return AddEditDetailedTransactionDraft(items = listOf(AddTransactionItem(0, null, categoryUi, amount, description)), accountId = accountId, evidence = evidence, evidenceHashes = evidenceMap)
    }

    class TransactionBuilder private constructor(){
        private var items = emptyList<TransactionItemDb>()
    }

    class TransactionItemBuilder private constructor(private var transactionItem: TransactionItemDb, private var clock: Clock, private var dao: TransactionItemDao, private var itemImagesDao: TransactionItemImagesDao) {
        private var images = emptyList<ImageDb>()
        companion object {
            fun builder(transactionItem: TransactionItemDb, clock: Clock, dao: TransactionItemDao, itemImagesDao: TransactionItemImagesDao): TransactionItemBuilder {
                val builder = TransactionItemBuilder(transactionItem, clock, dao, itemImagesDao)
                return builder
            }
        }

        fun build(): TransactionItemDb {
            val id = dao.insertTransactionItem(transactionItem).blockingGet()
            val item = transactionItem.copy(id)
            val date = ZonedDateTime.now(clock)
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            val offset = date.offset.id
            val zone = date.zone.id
            for(image in images) {
                itemImagesDao.insertItemImage(TransactionItemImagesDb(null, id, image.id!!, dateTimeString, offset, zone)).blockingSubscribe()
            }
            return item
        }
    }
}