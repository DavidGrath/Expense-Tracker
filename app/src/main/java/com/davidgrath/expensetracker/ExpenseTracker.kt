package com.davidgrath.expensetracker

import android.app.Application
import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import com.davidgrath.expensetracker.di.DaggerMainComponent
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.MainModule
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.ui.AddEditDetailedTransactionDraft
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionMainFragment
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.nio.file.Paths
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.io.path.toPath

open class ExpenseTracker : Application(), DraftFileHandler {

    open lateinit var appComponent: MainComponent
    private val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriTypeAdapter()).create()
    open lateinit var preferences: SharedPreferences
    open lateinit var LOGGER: Logger

//    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        LOGGER = LoggerFactory.getLogger(ExpenseTracker::class.java)
        appComponent = DaggerMainComponent.builder().mainModule(MainModule(this, this)).build()
        preferences = getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, MODE_PRIVATE)
        tempInit().subscribeOn(Schedulers.io()).blockingGet()
    }


    override fun saveDraft(draft: AddEditDetailedTransactionDraft): Single<Unit> {
        //TODO Debounce

        return Single.fromCallable {
            val string = gson.toJson(draft)
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            if(file.exists()) {
                file.writeText(string)
                LOGGER.info("saveDraft: Saved draft file")
            } else {
                LOGGER.info("saveDraft: File does not exist")
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun draftExists(): Boolean {
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        val exists = file.exists()
        LOGGER.info("Draft Exists: {}", exists)
        return exists
    }

    override fun createDraft(): Single<Boolean> {
        return Single.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            root.mkdirs()
            val file = File(root, Constants.DRAFT_FILE_NAME)
            val ret = file.createNewFile()
            if (ret) {
                LOGGER.info("Created new empty draft file")
                val emptyDraft = AddEditDetailedTransactionDraft(emptyList(), -1)
                val string = gson.toJson(emptyDraft)
                file.writeText(string)
                LOGGER.info("Wrote default draft to draft file")
            }
            ret
        }
    }

    override fun deleteDraftFiles(): Single<Boolean> {
        return Single.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            val d = file.delete()
            LOGGER.info("Deleted draft file: {}", d)
            val documents = File(root, Constants.SUBFOLDER_NAME_DOCUMENTS)
            if(documents.exists()) {
                val children = documents.listFiles()
                val size = children.size
                if(size > 0) {
                    documents.deleteRecursively()
                    LOGGER.info("Deleted {} unused files in {}", size, documents)
                } else {
                    LOGGER.info("Deleted empty directory {}", documents)
                }
            }
            val images = File(root, Constants.SUBFOLDER_NAME_IMAGES)
            if(images.exists()) {
                val children = images.listFiles()
                val size = children.size
                if(size > 0) {
                    images.deleteRecursively()
                    LOGGER.info("Deleted {} unused files in {}", size, images)
                } else {
                    images.delete()
                    LOGGER.info("Deleted empty directory {}", images)
                }
            }

            true
        }
    }

    override fun getDraft(): Maybe<AddEditDetailedTransactionDraft> {
        return Maybe.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            if(!file.exists()) {
                LOGGER.info("getDraft: File does not exist")
                return@fromCallable null
            }
            val size = file.length() //TODO UOM/UCOM
            val reader = file.bufferedReader()
            val draft = gson.fromJson(reader, AddEditDetailedTransactionDraft::class.java)
            reader.close()
            LOGGER.info("getDraft: Read {} bytes from draft file", size)
            draft
        }.subscribeOn(Schedulers.io())
    }

    override fun moveFileToMain(sourceFile: File, subfolder: String): Single<File> {
        
        return Single.fromCallable<File> {
            val mainFolder = File(filesDir, Constants.FOLDER_NAME_DATA)
            val folder = File(mainFolder, subfolder)
            val f = File(folder, sourceFile.name)
            if(f.exists()) {
                return@fromCallable f
            }
            sourceFile.copyTo(f)
            LOGGER.info("Created {}", f.path)
            
            val b = sourceFile.delete()
            LOGGER.info("Deleted {}: {}", sourceFile.path, b)
            
            f
        }.subscribeOn(Schedulers.io())

    }

    override fun getFileHash(uri: Uri): Single<String> {
        val inputStream = contentResolver.openInputStream(uri)!!
        return getSha256(inputStream).doOnSuccess { inputStream.close() }
    }

    /**
     * Deletes the original if it was the Camera intent file
     */
    override fun copyUriToDraft(uri: Uri, mimeType: String, subfolder: String): Single<Uri> {
        val subPath = Constants.FOLDER_NAME_DRAFT + File.separator + subfolder
        val folder = file(filesDir.absolutePath,
            Constants.FOLDER_NAME_DRAFT, subfolder
        )
        folder.mkdirs()
        val filename = UUID.randomUUID().toString()
        val extension = when (mimeType) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "application/pdf" -> ".pdf"
            else -> ""
        }
        return Single.fromCallable {
            val inputStream = contentResolver.openInputStream(uri)!!
            val file = File(folder, "$filename$extension")
            val outputStream = file.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            LOGGER.info("copyUriToDraft: Created file {}", subPath) //TODO Make a makeshift relativize
            if(uri.scheme == ContentResolver.SCHEME_FILE) {
                LOGGER.info("copyUriToDraft: File was copied to app from camera. Deleting original")
                val cameraDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
                if(cameraFile.exists()) {
                    cameraFile.delete()
                }
            }
            file.toUri()
        }
    }

    fun tempInit(): Single<Unit> {
        val profileDao = appComponent.profileDao()
        return profileDao.findByStringId(Constants.DEFAULT_PROFILE_ID).switchIfEmpty(tempCreateDefaultProfile()).flatMap { defaultProfile ->
            val currentProfile = preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null)
            if (currentProfile == null) {
                preferences.edit()
                    .putString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, Constants.DEFAULT_PROFILE_ID)
                    .commit()
                LOGGER.info("Initialized current profile to default {}", Constants.DEFAULT_PROFILE_ID)
            }
            tempInitProfile(defaultProfile)
                .flatMap {
                    tempInitDb()
                }
        }
    }
    fun tempInitDb(): Single<Unit> {
        return tempInitDefaultCategories()
    }

    fun tempCreateDefaultProfile(): Single<ProfileDb> {
        val clock = appComponent.timeHandler().getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val offset = date.offset.id
        val zone = date.zone.id
        val profile = ProfileDb(null, "Default", Constants.DEFAULT_PROFILE_ID, dateString, offset, zone)
        val profileDao = appComponent.profileDao()
        return profileDao.insertProfile(profile).flatMap {
            profileDao.getByStringId(Constants.DEFAULT_PROFILE_ID)
        }

    }
    fun tempInitProfile(profileDb: ProfileDb): Single<Unit> {
        return Single.fromCallable {
            val accountDao = appComponent.accountDao()
            val accounts = accountDao.getAllByProfileIdSingle(profileDb.id!!).blockingGet()
            var defaultAccount = accounts.firstOrNull()
            val locale = appComponent.timeHandler().getLocale()
            val currency = Currency.getInstance(locale)?.currencyCode ?: "USD"

            LOGGER.info("Picked default locale and currency: {}, {}", locale, currency)
            val clock = appComponent.timeHandler().getClock()
            val date = ZonedDateTime.now(clock)
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val offset = date.offset.id
            val zone = date.zone.id

            if (defaultAccount == null) {
                //Create Default Account
                val account = AccountDb(
                    null,
                    profileDb.id!!,
                    currency,
                    null,
                    "",
                    "Default Account",
                    dateString,
                    offset,
                    zone
                )
                val id = accountDao.insertAccount(account).blockingGet()
                defaultAccount = accountDao.findByIdSingle(id).blockingGet()!!
                LOGGER.info("Created default account for profile ${profileDb.stringId}")
            }

            val profilePreferences = getSharedPreferences(profileDb.stringId, MODE_PRIVATE)
            profilePreferences.edit()
                .putLong(Constants.PreferenceKeys.Profile.DEFAULT_ACCOUNT_ID, defaultAccount.id!!).commit()
            LOGGER.info("Set default account for profile ${profileDb.stringId}")
        }
    }
    fun tempInitDefaultCategories(): Single<Unit> {
        return Single.fromCallable {
            val clock = appComponent.timeHandler().getClock()
            val date = ZonedDateTime.now(clock)
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val offset = date.offset.id
            val zone = date.zone.id
            val categoryDao = appComponent.categoryDao()
            var anyCategoryNotExist = false
            for (category in Utils.CORE_CATEGORIES) {
                val categoryDb = categoryDao.findByStringId(category)
                    .subscribeOn(Schedulers.io())
                    .blockingGet()
                if (categoryDb == null) {
                    anyCategoryNotExist = true
                    categoryDao.insertCategory(
                        CategoryDb(
                            null,
                            null,
                            category,
                            false,
                            null,
                            dateString,
                            offset,
                            zone
                        )
                    )
                        .subscribeOn(Schedulers.io())
                        .blockingGet()
                }
            }
            if(anyCategoryNotExist) {
                LOGGER.info("Created default categories")
            }
        }
    }

}

