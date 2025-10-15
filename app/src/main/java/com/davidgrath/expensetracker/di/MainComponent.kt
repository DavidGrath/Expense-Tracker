package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionGetImageViewModelFactory
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionMainFragment
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionOtherDetailsFragment
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionViewModelFactory
import com.davidgrath.expensetracker.ui.dialogs.AddAccountDialogFragment
import com.davidgrath.expensetracker.ui.main.MainViewModelFactory
import com.davidgrath.expensetracker.ui.main.TransactionsFragment
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MainModule::class])
interface MainComponent {
    fun categoryDao(): CategoryDao
    fun accountDao(): AccountDao
    fun profileDao(): ProfileDao
    fun timeHandler(): TimeAndLocaleHandler
    fun inject(mainViewModelFactory: MainViewModelFactory)
    fun inject(addDetailedTransactionActivity: AddDetailedTransactionActivity)
    fun inject(transactionDetailsActivity: TransactionDetailsActivity)
    fun inject(addDetailedTransactionOtherDetailsFragment: AddDetailedTransactionOtherDetailsFragment)
    fun inject(addDetailedTransactionGetImageViewModelFactory: AddDetailedTransactionGetImageViewModelFactory)
    fun inject(addDetailedTransactionViewModelFactory: AddDetailedTransactionViewModelFactory)
    fun inject(transactionsFragment: TransactionsFragment)
    fun inject(addDetailedTransactionMainFragment: AddDetailedTransactionMainFragment)
    fun inject(addAccountDialogFragment: AddAccountDialogFragment)
    fun inject(accountsFragment: AccountsFragment)
}