package com.davidgrath.expensetracker.ui.transactiondetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository

class TransactionDetailsViewModelFactory(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val imageRepository: ImageRepository,
    private val categoryRepository: CategoryRepository,
    private val evidenceRepository: EvidenceRepository,
    private val accountRepository: AccountRepository,
    private val timeAndLocaleHandler: TimeAndLocaleHandler
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionDetailsViewModel(transactionId, transactionRepository,
            transactionItemRepository, imageRepository, categoryRepository, evidenceRepository,
            accountRepository, timeAndLocaleHandler
        ) as T
    }
}