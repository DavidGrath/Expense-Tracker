package com.davidgrath.expensetracker

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.google.gson.Gson
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class ExpenseTracker : Application(), DraftFileHandler, TempAppModule {

    private var draft = AddDetailedTransactionDraft(emptyList())
    private val _draftLiveData = MutableLiveData<AddDetailedTransactionDraft>()
    private val draftLiveData: LiveData<AddDetailedTransactionDraft> = _draftLiveData
    private val gson = Gson()

    private val tempTransactionDao = TempTransactionDao()
    private val tempTransactionItemDao = TempTransactionItemDao()

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftFile = File(root, Constants.DRAFT_FILE_NAME)
        Log.d("DraftFile", draftFile.absolutePath)
        _draftLiveData.postValue(draft)
    }


    override fun saveDraft(draft: AddDetailedTransactionDraft) {
        saveFile(draft)
        this.draft = draft
        _draftLiveData.postValue(draft)
    }

    private fun saveFile(draft: AddDetailedTransactionDraft): Single<Unit> {
        return Single.just(Unit).subscribeOn(Schedulers.io()).map {
            val string = gson.toJson(draft)
            val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
            val file = File(root, Constants.DRAFT_FILE_NAME)
            file.writeText(string)
        }
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
            val emptyDraft = AddDetailedTransactionDraft(listOf(AddTransactionItem(0)))
            val string = gson.toJson(emptyDraft)
            file.writeText(string)
            //TODO LiveData post
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

    override fun transactionDao(): TempTransactionDao {
        return tempTransactionDao
    }

    override fun transactionItemDao(): TempTransactionItemDao {
        return tempTransactionItemDao
    }

}

