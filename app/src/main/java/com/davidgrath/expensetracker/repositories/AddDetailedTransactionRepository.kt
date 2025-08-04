package com.davidgrath.expensetracker.repositories

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem

class AddDetailedTransactionRepository(private val fileHandler: DraftFileHandler, val tempImagesDao: TempImagesDao) {

    //TODO Relying on this variable is probably a terrible way to work with a FileObserver
    private var currentEvent = TransactionDetailEvent.All
    private var currentPosition = -1
    private var incrementId = 0
    private val liveData = fileHandler.getDraft()

    fun addItem(): Boolean {
        val draft = liveData.value?: AddDetailedTransactionDraft(emptyList())
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            currentEvent = TransactionDetailEvent.Change
            currentPosition = currentList.size + 1
            val newItems = currentList + AddTransactionPurchaseItem(++incrementId)
            fileHandler.saveDraft(draft.copy(items = newItems))
            return true
        }
        return false
    }

    fun deleteItem(position: Int) {
        val draft = liveData.value?: AddDetailedTransactionDraft(emptyList())
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            currentEvent = TransactionDetailEvent.Delete
            currentPosition = currentList.size + 1
            val newItems = currentList.toMutableList().apply {
                removeAt(position)
            }
            fileHandler.saveDraft(draft.copy(items = newItems))
        }
    }

    fun doesHashExist(sha256: String): Boolean {
        val draft = liveData.value?: AddDetailedTransactionDraft(emptyList())
        val draftImageHashes = draft.imageHashes.values
        return tempImagesDao.doesHashExist(sha256) or (sha256 in draftImageHashes)
    }

    fun getDraftImageUri(sha256: String): Uri {
        val draft = liveData.value?: AddDetailedTransactionDraft(emptyList())
        val draftImageMap = draft.imageHashes
        val key = draftImageMap.keys.find { draftImageMap[it] == sha256 }!!
        return key
    }

    fun changeItem(position: Int, item: AddTransactionPurchaseItem) {
        val draft = liveData.value?: AddDetailedTransactionDraft(emptyList())
        val currentList = draft.items
        currentEvent = TransactionDetailEvent.Change
        currentPosition = position
        val newItems = currentList.toMutableList().also {
            it[position] = item
        }
        fileHandler.saveDraft(draft.copy(items = newItems))
    }

    fun addImageToItem(itemId: Int, localUri: Uri, sha256: String) { //TODO Handle hashing flow properly - is it in onActivityResult or here that I actually compute the hash, and then also skip existing values?
        val draft = liveData.value?: AddDetailedTransactionDraft(emptyList())
        val currentList = draft.items
        val item = currentList.find { it.id == itemId }
        if(item == null) {
            return
        }
        val uriString = localUri.toString()
        if(uriString in item.images) {
            return
        }
        if(item.images.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM) {
            return
        }
        val index = currentList.indexOf(item)
        val newItem = item.copy(images = item.images + uriString)
        val newItems = currentList.toMutableList().also {
            it[index] = newItem
        }
        currentEvent = TransactionDetailEvent.ChangeInvalidate
        currentPosition = index
        val newHashes =  if(draft.imageHashes[localUri] == null) draft.imageHashes + (localUri to sha256) else draft.imageHashes
        fileHandler.saveDraft(AddDetailedTransactionDraft(newItems, newHashes))
    }

    fun getDraft(): LiveData<Triple<AddDetailedTransactionDraft, TransactionDetailEvent, Int>> {
        return fileHandler.getDraft().map {
            Triple(it, currentEvent, currentPosition)
        }
    }

    fun draftExists(): Boolean {
        return fileHandler.draftExists()
    }
    fun createDraft(): Boolean {
        return fileHandler.createDraft()
    }

    fun finishTransaction() {
        //Save to DB
        fileHandler.deleteDraft()
    }

    enum class TransactionDetailEvent {
        Delete,
        Insert,
        All,
        Change,
        ChangeInvalidate,
        None
    }
}