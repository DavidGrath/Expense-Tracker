package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.db.dao.TempCategoryDao
import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository

interface TempAppModule {
    fun transactionDao(): TempTransactionDao
    fun transactionItemDao(): TempTransactionItemDao
    fun imagesDao(): TempImagesDao
    fun categoryDao(): TempCategoryDao
    fun addDetailedTransactionRepository(): AddDetailedTransactionRepository
    fun transactionRepository(): TransactionRepository
    fun categoryRepository(): CategoryRepository
}