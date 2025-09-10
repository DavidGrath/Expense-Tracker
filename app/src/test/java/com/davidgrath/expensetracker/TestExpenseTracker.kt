package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.di.DaggerMainComponent
import com.davidgrath.expensetracker.di.DaggerTestComponent
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.di.TestModule
import io.reactivex.rxjava3.schedulers.Schedulers

class TestExpenseTracker: ExpenseTracker() {
    override lateinit var appComponent: MainComponent

    override fun onCreate() {
//        super.onCreate()
        appComponent = DaggerTestComponent.builder().testModule(TestModule(this, this)).build()
        preferences = getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, MODE_PRIVATE)
        tempInit().subscribeOn(Schedulers.io()).blockingGet()
    }
}