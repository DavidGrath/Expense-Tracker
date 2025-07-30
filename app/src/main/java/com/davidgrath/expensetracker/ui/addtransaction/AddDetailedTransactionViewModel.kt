package com.davidgrath.expensetracker.ui.addtransaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import java.math.BigDecimal

class AddDetailedTransactionViewModel: ViewModel() {

    /*private */var purchaseItems: MutableList<AddTransactionPurchaseItem> = mutableListOf()
    private val _purchaseItemsLiveData = MutableLiveData<List<AddTransactionPurchaseItem>>(purchaseItems)
    val purchaseItemsLiveData : LiveData<List<AddTransactionPurchaseItem>> = _purchaseItemsLiveData
    val transactionTotalLiveData : LiveData<BigDecimal> = _purchaseItemsLiveData.map { items -> items.map { it.amount?: BigDecimal.ZERO }.reduceOrNull {acc,bd -> acc.plus(bd) }?: BigDecimal.ZERO }

    fun addItem(): Boolean {
        if(purchaseItems.size + 1 <= 20) {
            purchaseItems.add(AddTransactionPurchaseItem())
            _purchaseItemsLiveData.postValue(purchaseItems)
            return true
        }
        return false
    }

    fun onItemChanged(position: Int, item: AddTransactionPurchaseItem) {
        purchaseItems[position] = item
        _purchaseItemsLiveData.postValue(purchaseItems)
    }

    fun onItemDeleted(position: Int) {
        purchaseItems.removeAt(position)
        _purchaseItemsLiveData.postValue(purchaseItems)
    }
}