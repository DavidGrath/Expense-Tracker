package com.davidgrath.expensetracker.ui.addtransaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import java.math.BigDecimal
import java.math.RoundingMode

class AddDetailedTransactionViewModel: ViewModel() {

    private var incrementId = 0
    var getImageItemId = -1
    private var purchaseItems: Pair<List<AddTransactionPurchaseItem>, Int> = listOf(AddTransactionPurchaseItem(++incrementId)) to EVENT_ALL
    private val _purchaseItemsLiveData = MutableLiveData<Pair<List<AddTransactionPurchaseItem>, Int>>(purchaseItems)
    val purchaseItemsLiveData : LiveData<Pair<List<AddTransactionPurchaseItem>, Int>> = _purchaseItemsLiveData
    val transactionTotalLiveData : LiveData<BigDecimal> = _purchaseItemsLiveData.map { items -> items.first.map { it.amount?: BigDecimal.ZERO }.reduceOrNull {acc,bd -> acc.plus(bd) }?.setScale(2, RoundingMode.HALF_UP)?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) }

    fun addItem(): Boolean {
        if(purchaseItems.first.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            val position = purchaseItems.first.size + 1
            purchaseItems = (purchaseItems.first + AddTransactionPurchaseItem(++incrementId)) to (EVENT_INSERT or position)
            _purchaseItemsLiveData.postValue(purchaseItems)
            return true
        }
        return false
    }

    fun onItemChanged(position: Int, item: AddTransactionPurchaseItem) {
        with(purchaseItems.first.toMutableList()) {
            this[position] = item
            purchaseItems = this to (EVENT_CHANGE or position)
            _purchaseItemsLiveData.postValue(purchaseItems)
        }
    }

    fun getImageOriginalUris(): Set<String> {
        return emptySet()
    }

    fun addItemFile(originalUri: String, localUri: String) {
        val item = purchaseItems.first.find { it.id == getImageItemId }
        if(item == null) {
            return
        }
        if(localUri in item.images) {
            return
        }
        val index = purchaseItems.first.indexOf(item)
        purchaseItems = purchaseItems.first.toMutableList().also {
            it[index] = item.copy(images = item.images + localUri)
        } to (EVENT_CHANGE_INVALIDATE or index)
        _purchaseItemsLiveData.postValue(purchaseItems)
        getImageItemId = -1
    }

    fun onItemDeleted(position: Int) {
        with(purchaseItems.first.toMutableList()) {
            this.removeAt(position)
            purchaseItems = this to (EVENT_DELETE or position)
            _purchaseItemsLiveData.postValue(purchaseItems)
        }
    }

    companion object {
        var EVENT_MASK =                0b111 shl 5
        var EVENT_DELETE =              0b001 shl 5
        var EVENT_INSERT =              0b010 shl 5
        var EVENT_ALL =                 0b011 shl 5
        var EVENT_CHANGE =              0b100 shl 5
        var EVENT_NONE =                0b101 shl 5
        var EVENT_CHANGE_INVALIDATE =   0b110 shl 5
        var POSITION_MASK = 0b000_11111
    }
}