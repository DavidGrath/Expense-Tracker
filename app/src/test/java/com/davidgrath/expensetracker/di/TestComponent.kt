package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.ExpenseTrackerTest
import com.davidgrath.expensetracker.db.dao.TransactionDaoTest
import com.davidgrath.expensetracker.db.dao.TransactionItemDaoTest
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepositoryTest
import com.davidgrath.expensetracker.repositories.TransactionRepositoryTest
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivityTest
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionOtherDetailsFragmentTest
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [TestModule::class])
interface TestComponent: MainComponent {

    fun inject(expenseTrackerTest: ExpenseTrackerTest)

    fun inject(addDetailedTransactionActivityTest: AddDetailedTransactionActivityTest)
    fun inject(addDetailedTransactionOtherDetailsFragmentTest: AddDetailedTransactionOtherDetailsFragmentTest)

    fun inject(transactionRepositoryTest: TransactionRepositoryTest)
    fun inject(addDetailedTransactionRepositoryTest: AddDetailedTransactionRepositoryTest)

    fun inject(transactionItemDaoTest: TransactionItemDaoTest)
    fun inject(transactionDaoTest: TransactionDaoTest)
}