package com.davidgrath.expensetracker

import android.app.Application
import android.os.FileObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import com.google.gson.Gson
import java.io.File

class ExpenseTracker: Application(), DraftFileHandler {

    private lateinit var draftFileObserver: FileObserver
    private val _draftLiveData = MutableLiveData<AddDetailedTransactionDraft>()
    private val draftLiveData : LiveData<AddDetailedTransactionDraft> = _draftLiveData
    private val gson = Gson()

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val draftFile = File(root, Constants.DRAFT_FILE_NAME)
        draftFileObserver = object: FileObserver(draftFile.absolutePath, FileObserver.ALL_EVENTS) {
            override fun onEvent(event: Int, path: String?) {
                if(event and FileObserver.MODIFY > 0) {
                    val file = File(root, path!!)
                    if(file == draftFile) {
                        val reader = file.bufferedReader()
                        val draft = gson.fromJson(reader, AddDetailedTransactionDraft::class.java)
                        _draftLiveData.postValue(draft)
                        reader.close()
                    }
                }
            }
        }
        if(draftFile.exists()) {
            draftFileObserver.startWatching()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        draftFileObserver.stopWatching()
    }

    override fun saveDraft(draft: AddDetailedTransactionDraft) {
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
        draftFileObserver.startWatching()
        if(ret) {
            val emptyDraft = AddDetailedTransactionDraft(listOf(AddTransactionPurchaseItem(0)))
            val string = gson.toJson(emptyDraft)
            file.writeText(string)
        }
        return ret
    }

    override fun deleteDraft(): Boolean {
        draftFileObserver.stopWatching()
        val root = File(filesDir, Constants.FOLDER_NAME_DRAFT)
        val file = File(root, Constants.DRAFT_FILE_NAME)
        return file.delete()
    }

    override fun getDraft(): LiveData<AddDetailedTransactionDraft> {
        return draftLiveData
    }

}