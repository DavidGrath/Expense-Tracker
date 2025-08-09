package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository

interface TempAppModule {
    fun transactionDao(): TempTransactionDao
    fun transactionItemDao(): TempTransactionItemDao
    fun imagesDao(): TempImagesDao
    fun addDetailedTransactionRepository(): AddDetailedTransactionRepository
}