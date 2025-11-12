package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionGetImageActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionGetImageViewModelFactory
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionMainFragment
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionOtherDetailsFragment
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionViewModelFactory
import com.davidgrath.expensetracker.ui.dialogs.AddAccountDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.WeekDayDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.YearDayDialogFragment
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.davidgrath.expensetracker.ui.main.MainViewModelFactory
import com.davidgrath.expensetracker.ui.main.TransactionsFragment
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragment
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsFragment
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsViewModelFactory
import com.davidgrath.expensetracker.ui.main.images.ImageStatsFragment
import com.davidgrath.expensetracker.ui.main.images.ImageStatsViewModelFactory
import com.davidgrath.expensetracker.ui.main.statistics.FilteredTransactionsActivity
import com.davidgrath.expensetracker.ui.main.statistics.FilteredTransactionsViewModel
import com.davidgrath.expensetracker.ui.main.statistics.FilteredTransactionsViewModelFactory
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFilterActivity
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFilterViewModelFactory
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsItemsFragment
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
    fun inject(statisticsFilterViewModelFactory: StatisticsFilterViewModelFactory)
    fun inject(statisticsFilterActivity: StatisticsFilterActivity)
    fun inject(statisticsFragment: StatisticsFragment)
    fun inject(weekDayDialogFragment: WeekDayDialogFragment)
    fun inject(yearDayDialogFragment: YearDayDialogFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(imageStatsViewModelFactory: ImageStatsViewModelFactory)
    fun inject(imageStatsFragment: ImageStatsFragment)
    fun inject(addDetailedTransactionGetImageActivity: AddDetailedTransactionGetImageActivity)
    fun inject(documentStatsFragment: DocumentStatsFragment)
    fun inject(documentStatsViewModelFactory: DocumentStatsViewModelFactory)
    fun inject(transactionDetailsItemsFragment: TransactionDetailsItemsFragment)
    fun inject(filteredTransactionsViewModelFactory: FilteredTransactionsViewModelFactory)
    fun inject(filteredTransactionsActivity: FilteredTransactionsActivity)
}