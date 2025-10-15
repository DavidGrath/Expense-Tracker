package com.davidgrath.expensetracker

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
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
import java.io.File
import java.util.Locale
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class ExpenseTrackerTest {

    @Inject
    lateinit var profileDao: ProfileDao
    @Inject
    lateinit var accountDao: AccountDao
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
}