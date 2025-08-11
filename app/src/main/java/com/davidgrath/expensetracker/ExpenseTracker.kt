package com.davidgrath.expensetracker

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidgrath.expensetracker.db.dao.TempCategoryDao
import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File

class ExpenseTracker : Application(), DraftFileHandler, TempAppModule {

    private var draft = AddDetailedTransactionDraft(emptyList())
    private val _draftLiveData = MutableLiveData<AddDetailedTransactionDraft>()
    private val draftLiveData: LiveData<AddDetailedTransactionDraft> = _draftLiveData
    private val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriTypeAdapter()).create()

    private val tempTransactionDao = TempTransactionDao()
    private val tempTransactionItemDao = TempTransactionItemDao()
    private val tempImagesDao = TempImagesDao()
    private val tempCategoryDao = TempCategoryDao()
    private val transactionRepository = TransactionRepository(tempTransactionDao, tempTransactionItemDao)
    private val addDetailedTransactionRepository = AddDetailedTransactionRepository(this, tempImagesDao, transactionRepository, tempCategoryDao)
    private val categoryRepository = CategoryRepository(tempCategoryDao)


    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftFile = File(root, Constants.DRAFT_FILE_NAME)
        Log.d("DraftFile", draftFile.absolutePath)
        _draftLiveData.postValue(draft)
        tempInitCategories()
    }


    override fun saveDraft(draft: AddDetailedTransactionDraft) {
        Thread {
            saveFile(draft)
        }.start()
        this.draft = draft
        _draftLiveData.postValue(draft)
    }

    private fun saveFile(draft: AddDetailedTransactionDraft) {
        val string = gson.toJson(draft)
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        file.writeText(string)
    }

    override fun draftExists(): Boolean {
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        return file.exists()
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
            _draftLiveData.postValue(emptyDraft)
        }
        return ret
    }

    override fun deleteDraft(): Boolean {
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        return file.delete()
    }

    override fun getDraft(): LiveData<AddDetailedTransactionDraft> {
        return draftLiveData
    }

    override fun getDraftValue(): AddDetailedTransactionDraft {
        return draft
    }

    override fun transactionDao(): TempTransactionDao {
        return tempTransactionDao
    }

    override fun transactionItemDao(): TempTransactionItemDao {
        return tempTransactionItemDao
    }

    override fun imagesDao(): TempImagesDao {
        return tempImagesDao
    }

    override fun categoryDao(): TempCategoryDao {
        return tempCategoryDao
    }

    override fun addDetailedTransactionRepository(): AddDetailedTransactionRepository {
        return addDetailedTransactionRepository
    }

    override fun transactionRepository(): TransactionRepository {
        return transactionRepository
    }

    override fun categoryRepository(): CategoryRepository {
        return categoryRepository
    }

    fun tempInitCategories() {
        val date = ZonedDateTime.now()
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val offset = date.offset.id
        val zone = date.zone.id
        for(category in Utils.CORE_CATEGORIES) {
            val categoryDb = tempCategoryDao.findByStringId(category).blockingGet()
            if(categoryDb == null) {
                tempCategoryDao.addCategory(CategoryDb(null, 0, category, false, null, dateString, offset, zone))
            }
        }
    }
}

