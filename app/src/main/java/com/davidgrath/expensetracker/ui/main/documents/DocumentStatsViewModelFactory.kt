package com.davidgrath.expensetracker.ui.main.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.MainComponent
import javax.inject.Inject

class DocumentStatsViewModelFactory(
    private val appComponent: MainComponent,
) : ViewModelProvider.Factory {

    @Inject
    lateinit var viewModel: DocumentStatsViewModel

    init {
        appComponent.inject(this)
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel as T
    }
}