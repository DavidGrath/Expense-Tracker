package com.davidgrath.expensetracker

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.davidgrath.expensetracker.di.DaggerMainComponent
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.MainModule
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.ui.AddEditDetailedTransactionDraft
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.util.Currency
import java.util.Locale
import java.util.UUID

open class ExpenseTracker : Application(), DraftFileHandler {

    open lateinit var appComponent: MainComponent
    private val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriTypeAdapter()).create()
    open lateinit var preferences: SharedPreferences

//    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
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
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun draftExists(): Boolean {
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        val exists = file.exists()
        Log.i(LOG_TAG, "Draft Exists: $exists")
        return exists
    }

    override fun createDraft(): Single<Boolean> {
        return Single.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            root.mkdirs()
            val file = File(root, Constants.DRAFT_FILE_NAME)
            val ret = file.createNewFile()
            if (ret) {
                val emptyDraft = AddEditDetailedTransactionDraft(emptyList())
                val string = gson.toJson(emptyDraft)
                file.writeText(string)
            }
            ret
        }
    }

    override fun deleteDraftFiles(): Single<Boolean> {
        return Single.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            val d = file.delete()
            Log.i("ExpenseTracker", "Deleted draft file: $d")
            val documents = File(root, Constants.SUBFOLDER_NAME_DOCUMENTS)
            if(documents.exists()) {
                val children = documents.listFiles()
                val size = children.size
                if(size > 0) {
                    documents.deleteRecursively()
                    Log.i("ExpenseTracker", "Deleted $size unused files in ${documents}")
                } else {
                    Log.i("ExpenseTracker", "Deleted empty directory ${documents}")
                }
            }
            val images = File(root, Constants.SUBFOLDER_NAME_IMAGES)
            if(images.exists()) {
                val children = images.listFiles()
                val size = children.size
                if(size > 0) {
                    images.deleteRecursively()
                    Log.i("ExpenseTracker", "Deleted $size unused files in ${images}")
                } else {
                    images.delete()
                    Log.i("ExpenseTracker", "Deleted empty directory ${images}")
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
                return@fromCallable null
            }
            val reader = file.bufferedReader()
            val draft = gson.fromJson(reader, AddEditDetailedTransactionDraft::class.java)
            reader.close()
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
            Log.i("ExpenseTracker", "Created ${f.path}")
            
            val b = sourceFile.delete()
            Log.i("ExpenseTracker", "Deleted ${sourceFile.path}: $b")
            
            f
        }.subscribeOn(Schedulers.io())

    }

    override fun getFileHash(uri: Uri): Single<String> {
        val inputStream = contentResolver.openInputStream(uri)!!
        return getSha256(inputStream).doOnSuccess { inputStream.close() }
    }

    override fun copyUriToDraft(uri: Uri, mimeType: String, subfolder: String): Single<Uri> {
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
        val clock = appComponent.clock()
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
            var cashAccount = accounts.find { !it.isCashless }
            var cashlessAccount = accounts.find { it.isCashless }
            val locale = Locale.getDefault()
            val currency = Currency.getInstance(locale)?.currencyCode ?: "USD"

            val clock = appComponent.clock()
            val date = ZonedDateTime.now(clock)
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val offset = date.offset.id
            val zone = date.zone.id

            if (cashlessAccount == null) {
                //Create Cashless Account
                val account = AccountDb(
                    null,
                    profileDb.id!!,
                    currency,
                    null,
                    "",
                    true,
                    "Default Cashless",
                    dateString,
                    offset,
                    zone
                )
                val id = accountDao.insertAccount(account).blockingGet()
                cashlessAccount = accountDao.findByIdSingle(id).blockingGet()!!
                Log.i(LOG_TAG, "Created default cashless account for profile ${profileDb.stringId}")
            }
            if (cashAccount == null) {
                val account = AccountDb(
                    null,
                    profileDb.id!!,
                    currency,
                    null,
                    "",
                    false,
                    "Default Cash",
                    dateString,
                    offset,
                    zone
                )
                accountDao.insertAccount(account).blockingGet()
            }

            val profilePreferences = getSharedPreferences(profileDb.stringId, MODE_PRIVATE)
            profilePreferences.edit()
                .putLong(Constants.PreferenceKeys.Profile.DEFAULT_ACCOUNT_ID, cashlessAccount.id!!).commit()
            Log.i(LOG_TAG, "Set default cashless account for profile ${profileDb.stringId}")
        }
    }
    fun tempInitDefaultCategories(): Single<Unit> {
        return Single.fromCallable {
            val clock = appComponent.clock()
            val date = ZonedDateTime.now(clock)
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val offset = date.offset.id
            val zone = date.zone.id
            val categoryDao = appComponent.categoryDao()
            for (category in Utils.CORE_CATEGORIES) {
                val categoryDb = categoryDao.findByStringId(category)
                    .subscribeOn(Schedulers.io())
                    .blockingGet()
                if (categoryDb == null) {
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
        }
    }

    companion object {
        const val LOG_TAG = "ExpenseTracker"
    }
}

