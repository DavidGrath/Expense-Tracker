package com.davidgrath.expensetracker.ui.addtransaction

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode

class AddDetailedTransactionViewModel(private val repository: AddDetailedTransactionRepository): ViewModel() {


    var getImageItemId = -1
    init {
        if(!repository.draftExists()) {
            repository.createDraft()
            repository.addItem()
        }
    }
    val purchaseItemsLiveData = repository.getDraft()
    val transactionTotalLiveData : LiveData<BigDecimal> = purchaseItemsLiveData.map { items -> items.first.items.map { it.amount?: BigDecimal.ZERO }.reduceOrNull {acc,bd -> acc.plus(bd) }?.setScale(2, RoundingMode.HALF_UP)?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) }

    fun addItem(): Boolean {
        return repository.addItem()
    }

    fun onItemChanged(position: Int, item: AddTransactionPurchaseItem) {
        repository.changeItem(position, item)
    }

    fun doesHashExist(sha256: String): Boolean {
        return repository.doesHashExist(sha256)
    }

    fun getDraftImageUri(sha256: String): Uri {
        return repository.getDraftImageUri(sha256)
    }

    fun addItemFile(uri: Uri, sha256: String) {
        repository.addImageToItem(getImageItemId, uri, sha256)
        getImageItemId = -1
    }

    fun onItemDeleted(position: Int) {
        repository.deleteItem(position)
    }

    fun finishDraft() {
        repository.finishTransaction()
    }

}