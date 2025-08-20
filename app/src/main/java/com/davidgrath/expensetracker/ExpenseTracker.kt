package com.davidgrath.expensetracker

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidgrath.expensetracker.di.DaggerMainComponent
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.MainModule
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File

open class ExpenseTracker : Application(), DraftFileHandler{

    open lateinit var appComponent: MainComponent
    private var draft = AddDetailedTransactionDraft(emptyList())
    private val _draftLiveData = MutableLiveData<AddDetailedTransactionDraft>()
    private val draftLiveData: LiveData<AddDetailedTransactionDraft> = _draftLiveData
    private val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriTypeAdapter()).create()


//    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerMainComponent.builder().mainModule(MainModule(this, this)).build()
        tempInitCategories()
    }


    override fun saveDraft(draft: AddDetailedTransactionDraft) {
        saveFile(draft).subscribeOn(Schedulers.io()).blockingSubscribe()
        this.draft = draft
        _draftLiveData.postValue(draft)
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

    override fun createDraft(): Boolean {
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        root.mkdirs()
        val file = File(root, Constants.DRAFT_FILE_NAME)
        val ret = file.createNewFile()
        if (ret) {
            val emptyDraft = AddDetailedTransactionDraft(emptyList())
            val string = gson.toJson(emptyDraft)
            file.writeText(string)
            this.draft = emptyDraft
            _draftLiveData.postValue(emptyDraft)
        }
        return ret
    }

    override fun deleteDraft(): Boolean {
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        this.draft = AddDetailedTransactionDraft(emptyList())
        _draftLiveData.postValue(this.draft)
        return file.delete()
    }

    override fun getDraft(): LiveData<AddDetailedTransactionDraft> {
        return draftLiveData
    }

    override fun getDraftValue(): AddDetailedTransactionDraft {
        return draft
    }

    override fun moveFileToMain(file: File): Single<File> {
        return Single.fromCallable<File> {
            val mainFolder = File(filesDir, Constants.FOLDER_NAME_DATA)
            val folder = File(mainFolder, Constants.SUBFOLDER_NAME_IMAGES)
            val f = File(folder, file.name)
            file.copyTo(f)
            Log.i("ExpenseTracker", "Created ${f.path}")
            val b = file.delete()
            Log.i("ExpenseTracker", "Deleted ${file.path}: $b")
            f
        }.subscribeOn(Schedulers.io())

    }


    fun tempInitCategories() {
        val clock = appComponent.clock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val offset = date.offset.id
        val zone = date.zone.id
        val categoryDao = appComponent.categoryDao()
        for(category in Utils.CORE_CATEGORIES) {
            val categoryDb = categoryDao.findByStringId(category)
                .subscribeOn(Schedulers.io())
                .blockingGet()
            if(categoryDb == null) {
                categoryDao.insertCategory(CategoryDb(null, 0, category, false, null, dateString, offset, zone))
                    .subscribeOn(Schedulers.io())
                    .blockingGet()
            }
        }
    }

    companion object {
        const val LOG_TAG = "ExpenseTracker"
    }
}

