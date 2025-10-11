package com.davidgrath.expensetracker.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.repositories.ImageRepository
import javax.inject.Inject

class AddDetailedTransactionGetImageViewModelFactory(private val appComponent: MainComponent): ViewModelProvider.Factory {

    @Inject
    lateinit var imageRepository: ImageRepository

    init {
        appComponent.inject(this)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddDetailedTransactionGetImageViewModel(imageRepository) as T
    }
}