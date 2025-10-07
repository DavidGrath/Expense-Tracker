package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import org.threeten.bp.Clock
import java.math.BigDecimal

class AddDetailedTransactionViewModelFactory(
    private val application: Application,
    private val mode: String,
    private val addDetailedTransactionRepository: AddDetailedTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val profileDao: ProfileDao,
    private val profileStringId: String,
    private val transactionId: Long?,
    private val initialAccountId: Long?,
    private val initialAmount: BigDecimal?,
    private val initialDescription: String?,
    private val initialCategoryId: Long?
    ): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddDetailedTransactionViewModel(application, mode, addDetailedTransactionRepository, categoryRepository, accountRepository, profileDao, profileStringId, transactionId, initialAccountId, initialAmount, initialDescription, initialCategoryId) as T
    }
}