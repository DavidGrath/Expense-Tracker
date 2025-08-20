package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.di.DaggerMainComponent
import com.davidgrath.expensetracker.di.DaggerTestComponent
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.TestModule

class TestExpenseTracker: ExpenseTracker() {
    override lateinit var appComponent: MainComponent

    override fun onCreate() {
        appComponent = DaggerTestComponent.builder().testModule(TestModule(this, this)).build()
        tempInitCategories()
//        super.onCreate()
    }
}