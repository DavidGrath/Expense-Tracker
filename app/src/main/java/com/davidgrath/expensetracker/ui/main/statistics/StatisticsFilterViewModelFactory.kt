package com.davidgrath.expensetracker.ui.main.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.MainComponent
import javax.inject.Inject

class StatisticsFilterViewModelFactory(
    private val appComponent: MainComponent,
) : ViewModelProvider.Factory {

    @Inject
    lateinit var viewModel: StatisticsFilterViewModel

    init {
        appComponent.inject(this)
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel as T
    }
}