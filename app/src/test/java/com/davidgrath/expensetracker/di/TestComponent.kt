package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.ExpenseTrackerTest
import com.davidgrath.expensetracker.db.dao.AccountDaoTest
import com.davidgrath.expensetracker.db.dao.TransactionDaoTest
import com.davidgrath.expensetracker.db.dao.TransactionItemDaoTest
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepositoryTest
import com.davidgrath.expensetracker.repositories.TransactionRepositoryTest
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivityTest
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionOtherDetailsFragmentTest
import com.davidgrath.expensetracker.ui.main.MainViewModelTest
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragmentTest
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFilterActivityTest
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragmentTest
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [TestModule::class])
interface TestComponent: MainComponent {

    fun inject(expenseTrackerTest: ExpenseTrackerTest)

    //region UI

    fun inject(addDetailedTransactionActivityTest: AddDetailedTransactionActivityTest)
    fun inject(addDetailedTransactionOtherDetailsFragmentTest: AddDetailedTransactionOtherDetailsFragmentTest)
    fun inject(accountsFragmentTest: AccountsFragmentTest)
    fun inject(mainViewModelTest: MainViewModelTest)
    fun inject(statisticsFragmentTest: StatisticsFragmentTest)
    fun inject(statisticsFilterActivityTest: StatisticsFilterActivityTest)
    //endregion

    //region Repository
    fun inject(transactionRepositoryTest: TransactionRepositoryTest)
    fun inject(addDetailedTransactionRepositoryTest: AddDetailedTransactionRepositoryTest)

    //endregion


    //region DAO
    fun inject(transactionItemDaoTest: TransactionItemDaoTest)
    fun inject(transactionDaoTest: TransactionDaoTest)
    fun inject(accountDaoTest: AccountDaoTest)
    //endregion
}