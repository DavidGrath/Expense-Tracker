package com.davidgrath.expensetracker

import android.content.Context
import android.os.Environment
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.test.TestContentProvider
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.threeten.bp.LocalDate
import java.io.File
import java.util.Locale
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class ExpenseTrackerTest {

    @Inject
    lateinit var profileDao: ProfileDao
    @Inject
    lateinit var accountDao: AccountDao
    @Inject
    lateinit var imageDao: ImageDao
    @Inject
    lateinit var documentDao: EvidenceDao
    @Inject
    lateinit var transactionDao: TransactionDao
    lateinit var app: TestExpenseTracker

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun initializeTest() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        val preferences = app.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        //Assert default profile exists
        assertEquals(Constants.DEFAULT_PROFILE_ID, preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null))
        val profile = profileDao.findByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        assertNotNull(profile)
        //Assert default account exists
        val accounts = accountDao.getAllByProfileIdSingle(profile!!.id!!).subscribeOn(Schedulers.io()).blockingGet()
        val account = accounts.firstOrNull()
        assertNotNull(account)
        assertEquals("USD", account!!.currencyCode)
    }

    @Test
    fun getFileHashTest() {
        val cameraDirectory = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
        val resource = TestData.Resource.Images.BREAD
        val otherResource = TestData.Resource.Images.TOOTHBRUSH
        val classLoader = ExpenseTrackerTest::class.java.classLoader!!
        copyResourceToFile(classLoader, resource.resourceName, cameraFile)
        addContentProviderResources(app, classLoader, otherResource)
        val fileUri = cameraFile.toUri()
        val contentUri = otherResource.uri

        val fileHash = app.getFileHash(fileUri).subscribeOn(Schedulers.io()).blockingGet()
        val contentHash = app.getFileHash(contentUri).subscribeOn(Schedulers.io()).blockingGet()

        assertEquals(resource.sha256, fileHash)
        assertEquals(otherResource.sha256, contentHash)
    }

    @Test
    fun copyUriToDraftTest() {
        val cameraDirectory = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
        val resource = TestData.Resource.Images.BREAD
        val otherResource = TestData.Resource.Images.TOOTHBRUSH
        val classLoader = ExpenseTrackerTest::class.java.classLoader!!
        copyResourceToFile(classLoader, resource.resourceName, cameraFile)
        addContentProviderResources(app, classLoader, otherResource)
        val fileUri = cameraFile.toUri()
        val contentUri = otherResource.uri

        app.copyUriToDraft(fileUri, "image/jpeg", Constants.SUBFOLDER_NAME_IMAGES).subscribeOn(Schedulers.io()).blockingSubscribe()
        app.copyUriToDraft(contentUri, "image/jpeg", Constants.SUBFOLDER_NAME_IMAGES).subscribeOn(Schedulers.io()).blockingSubscribe()
        val imagesDir = file(app.filesDir, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)

        assertEquals(1, getHashCount(resource.sha256, imagesDir).subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(1, getHashCount(otherResource.sha256, imagesDir).subscribeOn(Schedulers.io()).blockingGet())
    }

    /**
     * I've started using the app on my main phone, and I decided to change things up since then, so this is needed
     */
    @Test
    @Ignore("Work in progress")
    fun tempMigrationTest() {
        val folder = app.filesDir
        val files = folder.listFiles().filter { it.isFile }.map { it.name }.toSet()
        val folders = folder.listFiles().filter { !it.isFile }.map { it.name }.toSet()
        val appFiles = setOf(Constants.DRAFT_FILE_NAME, Constants.FILE_NAME_INTENT_PICTURE, Constants.FILE_NAME_STATS_FILTER_DATA)
        val appFolders = setOf(Constants.FOLDER_NAME_DATA, Constants.FOLDER_NAME_DRAFT)
        val createdFiles = appFiles.intersect(files)
        val createdFolders = appFolders.intersect(folders)
        val migrationNeeded = createdFiles.isNotEmpty() || createdFolders.isNotEmpty()
        if(migrationNeeded) {
            val profileFolder = file(folder, Constants.FOLDER_NAME_PROFILES, Constants.DEFAULT_PROFILE_ID)
            profileFolder.mkdirs()
            for (f in createdFiles) {
                val fromFile = File(folder, f)
                val toFile = File(profileFolder, f)
                fromFile.copyTo(toFile)
                fromFile.delete()
                println("Moved $f")
            }
            val profile = profileDao.findByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
            val images = imageDao.getAllByProfileIdSingle(profile!!.id!!).subscribeOn(Schedulers.io()).blockingGet()
            for(image in images) {
                val file = image.uri.toUri().toFile()
                val newFile = file(profileFolder, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES, file.name)
                imageDao.updateUri(image.id!!, newFile.toUri().toString()).subscribeOn(Schedulers.io()).blockingSubscribe()
            }
            val documents = documentDao.getAllByProfileIdSingle(profile!!.id!!).subscribeOn(Schedulers.io()).blockingGet()

            for(doc in documents) {
                val file = doc.uri.toUri().toFile()
//                val dayFolder = file.parentFile
//                val monthFolder = dayFolder.parentFile
//                val yearFolder = monthFolder.parentFile

                val transaction = transactionDao.getByIdSingle(doc.transactionId).subscribeOn(Schedulers.io()).blockingGet()
                val localDate = LocalDate.parse(transaction.datedAt)
                val year = String.format("%04d", localDate.year)
                val month = String.format("%02d", localDate.monthValue)
                val day = String.format("%02d", localDate.dayOfMonth)
                val newFile = file(profileFolder, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, year, month, day, file.name)
                documentDao.updateEvidenceUri(doc.id!!, newFile.toUri().toString()).subscribeOn(Schedulers.io()).blockingSubscribe()
            }


            for(f in createdFolders) {
                val fromFile = File(folder, f)
                val toFile = File(profileFolder, f)
                fromFile.copyRecursively(toFile)
                fromFile.deleteRecursively()
                println("Moved $f")
            }
        }
    }
}