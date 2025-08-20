package com.davidgrath.expensetracker

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.squareup.rx3.idler.Rx3Idler
import io.reactivex.rxjava3.plugins.RxJavaPlugins

class ExpenseTrackerTestRunner: AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        RxJavaPlugins.setInitIoSchedulerHandler(Rx3Idler.create("Instrumented Rx3 Handler"))
        super.onCreate(arguments)
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, InstrumentedTestExpenseTracker::class.java.name, context)
    }
}