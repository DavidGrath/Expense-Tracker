package com.davidgrath.expensetracker

import android.app.Application
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.PurchaseItemDb
import com.davidgrath.expensetracker.entities.db.TransactionDb

class ExpenseTracker: Application() {
    override fun onCreate() {
        super.onCreate()
    }
}