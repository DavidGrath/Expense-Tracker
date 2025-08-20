package com.davidgrath.expensetracker.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import javax.inject.Inject

class MainViewModelFactory(
    private val appComponent: MainComponent,
) : ViewModelProvider.Factory {

    @Inject
    lateinit var viewModel: MainViewModel

    init {
        appComponent.inject(this)
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel as T
    }
}