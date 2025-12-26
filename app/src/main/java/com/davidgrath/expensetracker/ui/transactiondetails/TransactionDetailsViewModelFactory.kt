package com.davidgrath.expensetracker.ui.transactiondetails

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.SellerRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import javax.inject.Inject

class TransactionDetailsViewModelFactory(
    private val transactionId: Long,
    private val appComponent: MainComponent
): ViewModelProvider.Factory {

    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var transactionItemRepository: TransactionItemRepository
    @Inject
    lateinit var imageRepository: ImageRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var evidenceRepository: EvidenceRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Inject
    lateinit var sellerRepository: SellerRepository
    @Inject
    lateinit var application: Application

    init {
        appComponent.inject(this)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionDetailsViewModel(transactionId, transactionRepository,
            transactionItemRepository, imageRepository, categoryRepository, evidenceRepository,
            accountRepository, timeAndLocaleHandler, sellerRepository, application
        ) as T
    }
}