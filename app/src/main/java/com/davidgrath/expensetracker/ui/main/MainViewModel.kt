package com.davidgrath.expensetracker.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.davidgrath.expensetracker.entities.ui.Transaction
import com.davidgrath.expensetracker.entities.ui.TransactionItem
import com.davidgrath.expensetracker.transactionsToTransactionItems

class MainViewModel: ViewModel() {
    private var transactions = mutableListOf<Transaction>()
    private var list = listOf<TransactionItem>()
    private val _listLiveData = MutableLiveData<List<TransactionItem>>()
    val listLiveData : LiveData<List<TransactionItem>> = _listLiveData

    public fun addToList(transaction: Transaction) {
        transactions.add(transaction)
        this.list = transactionsToTransactionItems(transactions)
        _listLiveData.postValue(this.list)
    }


}