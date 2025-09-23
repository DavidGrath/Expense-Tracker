package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import org.threeten.bp.Clock
import java.math.BigDecimal

class AddDetailedTransactionViewModelFactory(
    private val application: Application,
    private val mode: String,
    private val addDetailedTransactionRepository: AddDetailedTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
    private val transactionId: Long?,
    private val initialAmount: BigDecimal?,
    private val initialDescription: String?,
    private val initialCategoryId: Long?
    ): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddDetailedTransactionViewModel(application, mode, addDetailedTransactionRepository, categoryRepository, clock, transactionId, initialAmount, initialDescription, initialCategoryId) as T
    }
}