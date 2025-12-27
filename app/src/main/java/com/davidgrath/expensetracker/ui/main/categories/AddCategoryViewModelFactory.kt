package com.davidgrath.expensetracker.ui.main.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.MaterialMetadata

class AddCategoryViewModelFactory(private val icons: List<MaterialMetadata.MaterialIcon>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddCategoryViewModel(icons) as T
    }
}