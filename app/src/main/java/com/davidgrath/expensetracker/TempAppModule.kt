package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao

interface TempAppModule {
    fun transactionDao(): TempTransactionDao
    fun transactionItemDao(): TempTransactionItemDao
}