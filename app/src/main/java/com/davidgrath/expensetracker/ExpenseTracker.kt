package com.davidgrath.expensetracker

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.davidgrath.expensetracker.di.DaggerMainComponent
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.MainModule
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.util.Currency
import java.util.Locale

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


    override fun saveDraft(draft: AddDetailedTransactionDraft) {
        //TODO Debounce
        saveFile(draft).subscribeOn(Schedulers.io()).subscribe()
    }

    private fun saveFile(draft: AddDetailedTransactionDraft): Single<Unit> {
        return Single.fromCallable {
            val string = gson.toJson(draft)
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            file.writeText(string)
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
                val emptyDraft = AddDetailedTransactionDraft(emptyList())
                val string = gson.toJson(emptyDraft)
                file.writeText(string)
            }
            ret
        }
    }

    override fun deleteDraft(): Single<Boolean> {
        return Single.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            file.delete()
        }
    }

    override fun getDraft(): Single<AddDetailedTransactionDraft> {
        return Single.fromCallable {
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            val reader = file.bufferedReader()
            val draft = gson.fromJson(reader, AddDetailedTransactionDraft::class.java)
            reader.close()
            draft
        }.subscribeOn(Schedulers.io())
    }

    override fun moveFileToMain(file: File, subfolder: String): Single<File> {
        return Single.fromCallable<File> {
            val mainFolder = File(filesDir, Constants.FOLDER_NAME_DATA)
            val folder = File(mainFolder, subfolder)
            val f = File(folder, file.name)
            if(f.exists()) {
                return@fromCallable f
            }
            file.copyTo(f)
            Log.i("ExpenseTracker", "Created ${f.path}")
            val b = file.delete()
            Log.i("ExpenseTracker", "Deleted ${file.path}: $b")
            f
        }.subscribeOn(Schedulers.io())

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

