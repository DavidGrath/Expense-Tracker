package com.davidgrath.expensetracker.di

import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivityInstrumentedTest
import com.davidgrath.expensetracker.ui.main.MainActivityInstrumentedTest
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [InstrumentedTestModule::class])
interface InstrumentedTestComponent: MainComponent {
    fun inject(addDetailedTransactionActivityInstrumentedTest: AddDetailedTransactionActivityInstrumentedTest)
    fun inject(mainActivityInstrumentedTest: MainActivityInstrumentedTest)
}