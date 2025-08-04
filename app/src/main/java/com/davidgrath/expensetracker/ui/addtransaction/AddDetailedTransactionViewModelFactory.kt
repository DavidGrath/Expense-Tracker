package com.davidgrath.expensetracker.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository

class AddDetailedTransactionViewModelFactory(private val repository: AddDetailedTransactionRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddDetailedTransactionViewModel(repository) as T
    }
}