package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.di.DaggerInstrumentedTestComponent
import com.davidgrath.expensetracker.di.InstrumentedTestModule
import com.davidgrath.expensetracker.di.MainComponent
import io.reactivex.rxjava3.schedulers.Schedulers

class InstrumentedTestExpenseTracker: ExpenseTracker() {
    override lateinit var appComponent: MainComponent

    override fun onCreate() {
//        super.onCreate()
        appComponent = DaggerInstrumentedTestComponent.builder().instrumentedTestModule(
            InstrumentedTestModule(this, this)
        ).build()
        preferences = getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, MODE_PRIVATE)
        tempInit().subscribeOn(Schedulers.io()).blockingGet()
    }
}