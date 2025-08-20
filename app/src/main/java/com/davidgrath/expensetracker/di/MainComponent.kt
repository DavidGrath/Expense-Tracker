package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionViewModelFactory
import com.davidgrath.expensetracker.ui.main.MainViewModelFactory
import dagger.Component
import org.threeten.bp.Clock

@Component(modules = [MainModule::class])
interface MainComponent {
    fun categoryDao(): CategoryDao
    fun clock(): Clock
    fun inject(mainViewModelFactory: MainViewModelFactory)
    fun inject(mainViewModelFactory: AddDetailedTransactionActivity)
}