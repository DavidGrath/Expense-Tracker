package com.davidgrath.expensetracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.davidgrath.expensetracker.entities.ui.Transaction

class MainViewModel: ViewModel() {
    private var list = mutableListOf<Transaction>()
    private val _listLiveData = MutableLiveData<List<Transaction>>()
    val listLiveData : LiveData<List<Transaction>> = _listLiveData

    public fun addToList(transaction: Transaction) {
        list.add(transaction)
        _listLiveData.postValue(this.list)
    }
}