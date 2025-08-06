package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TransactionItemCategoriesDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val transactionItemId: Long,
    val categoryId: Long,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
