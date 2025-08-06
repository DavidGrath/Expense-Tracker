package com.davidgrath.expensetracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.repositories.TransactionRepository

class MainViewModelFactory(private val transactionRepository: TransactionRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(transactionRepository) as T
    }
}