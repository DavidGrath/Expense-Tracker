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
import com.davidgrath.expensetracker.ui.dialogs.AddImageDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.AddTransactionDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.WeekDayDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.YearDayDialogFragment
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.davidgrath.expensetracker.ui.main.MainViewModelFactory
import com.davidgrath.expensetracker.ui.main.TransactionsFragment
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragment
import com.davidgrath.expensetracker.ui.main.categories.CategoriesViewModel
import com.davidgrath.expensetracker.ui.main.categories.CategoriesViewModelFactory
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsFragment
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsViewModelFactory
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsActivity
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsViewModelFactory
import com.davidgrath.expensetracker.ui.main.images.ImageStatsFragment
import com.davidgrath.expensetracker.ui.main.images.ImageStatsViewModelFactory
import com.davidgrath.expensetracker.ui.main.pdfdetails.PdfDetailsActivity
import com.davidgrath.expensetracker.ui.main.pdfdetails.PdfDetailsViewModelFactory
import com.davidgrath.expensetracker.ui.main.statistics.FilteredTransactionsActivity
import com.davidgrath.expensetracker.ui.main.statistics.FilteredTransactionsViewModel
import com.davidgrath.expensetracker.ui.main.statistics.FilteredTransactionsViewModelFactory
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFilterActivity
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFilterViewModelFactory
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFilterWeekdaysFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsItemsFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsViewModelFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MainModule::class])
interface MainComponent {
    //region Daos
    fun categoryDao(): CategoryDao
    fun accountDao(): AccountDao
    fun profileDao(): ProfileDao

    //endregion

    //region Activities


    fun inject(addDetailedTransactionActivity: AddDetailedTransactionActivity)
    fun inject(transactionDetailsActivity: TransactionDetailsActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(addDetailedTransactionGetImageActivity: AddDetailedTransactionGetImageActivity)
    fun inject(filteredTransactionsActivity: FilteredTransactionsActivity)
    fun inject(imageDetailsActivity: ImageDetailsActivity)
    fun inject(pdfDetailsActivity: PdfDetailsActivity)


    //endregion

    //region ViewModel  Factories

    fun inject(mainViewModelFactory: MainViewModelFactory)
    fun inject(imageStatsViewModelFactory: ImageStatsViewModelFactory)
    fun inject(statisticsFilterViewModelFactory: StatisticsFilterViewModelFactory)
    fun inject(addDetailedTransactionGetImageViewModelFactory: AddDetailedTransactionGetImageViewModelFactory)
    fun inject(addDetailedTransactionViewModelFactory: AddDetailedTransactionViewModelFactory)
    fun inject(filteredTransactionsViewModelFactory: FilteredTransactionsViewModelFactory)

    fun inject(transactionDetailsViewModelFactory: TransactionDetailsViewModelFactory)
    fun inject(imageDetailsViewModelFactory: ImageDetailsViewModelFactory)
    fun inject(documentStatsViewModelFactory: DocumentStatsViewModelFactory)
    fun inject(categoriesViewModelFactory: CategoriesViewModelFactory)
    fun inject(pdfDetailsViewModelFactory: PdfDetailsViewModelFactory)


    //endregion

    //region Dialog Fragment


    fun inject(addAccountDialogFragment: AddAccountDialogFragment)
    fun inject(weekDayDialogFragment: WeekDayDialogFragment)
    fun inject(yearDayDialogFragment: YearDayDialogFragment)
    fun inject(addTransactionDialogFragment: AddTransactionDialogFragment)

    //endregion

    fun timeHandler(): TimeAndLocaleHandler
    fun inject(transactionsFragment: TransactionsFragment)
    fun inject(addDetailedTransactionMainFragment: AddDetailedTransactionMainFragment)
    fun inject(addImageDialogFragment: AddImageDialogFragment)
    fun inject(accountsFragment: AccountsFragment)
    fun inject(statisticsFilterWeekdaysFragment: StatisticsFilterWeekdaysFragment)
    fun inject(statisticsFragment: StatisticsFragment)

    fun inject(imageStatsFragment: ImageStatsFragment)
    fun inject(documentStatsFragment: DocumentStatsFragment)


    fun inject(transactionDetailsItemsFragment: TransactionDetailsItemsFragment)
    fun inject(addDetailedTransactionOtherDetailsFragment: AddDetailedTransactionOtherDetailsFragment)
}