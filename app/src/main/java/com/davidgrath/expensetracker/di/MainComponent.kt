package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionViewModelFactory
import com.davidgrath.expensetracker.ui.main.MainViewModelFactory
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsViewModelFactory
import dagger.Component
import org.threeten.bp.Clock
import javax.inject.Singleton

@Singleton
@Component(modules = [MainModule::class])
interface MainComponent {
    fun categoryDao(): CategoryDao
    fun accountDao(): AccountDao
    fun profileDao(): ProfileDao
    fun clock(): Clock
    fun inject(mainViewModelFactory: MainViewModelFactory)
    fun inject(addDetailedTransactionActivity: AddDetailedTransactionActivity)
    fun inject(transactionDetailsActivity: TransactionDetailsActivity)
}