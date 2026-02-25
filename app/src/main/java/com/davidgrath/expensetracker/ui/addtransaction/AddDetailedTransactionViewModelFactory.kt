package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.SellerRepository
import com.davidgrath.expensetracker.utils.ImageHelper
import java.math.BigDecimal
import javax.inject.Inject

class AddDetailedTransactionViewModelFactory(
    private val application: Application,
    private val mode: String,
    private val profileStringId: String,
    private val transactionId: Long?,
    private val initialAccountId: Long?,
    private val initialAmount: BigDecimal?,
    private val initialDescription: String?,
    private val initialCategoryId: Long?
    ): ViewModelProvider.Factory {


        @Inject
        lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
        @Inject
        lateinit var categoryRepository: CategoryRepository
        @Inject
        lateinit var accountRepository: AccountRepository
        @Inject
        lateinit var profileRepository: ProfileRepository
        @Inject
        lateinit var imageRepository: ImageRepository
        @Inject
        lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
        @Inject
        lateinit var sellerRepository: SellerRepository
        @Inject
        lateinit var imageHelper: ImageHelper
    init {
        (application as ExpenseTracker).appComponent.inject(this)
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddDetailedTransactionViewModel(application, mode, addDetailedTransactionRepository, categoryRepository, accountRepository, profileRepository,
            imageRepository, timeAndLocaleHandler, sellerRepository, imageHelper, profileStringId, transactionId, initialAccountId, initialAmount, initialDescription, initialCategoryId) as T
    }
}