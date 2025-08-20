package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.di.DaggerInstrumentedTestComponent
import com.davidgrath.expensetracker.di.InstrumentedTestModule
import com.davidgrath.expensetracker.di.MainComponent

class InstrumentedTestExpenseTracker: ExpenseTracker() {
    override lateinit var appComponent: MainComponent

    override fun onCreate() {
//        super.onCreate()
        appComponent = DaggerInstrumentedTestComponent.builder().instrumentedTestModule(
            InstrumentedTestModule(this, this)
        ).build()
        tempInitCategories()
    }
}