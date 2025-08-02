package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PurchaseItemCategoriesDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val purchaseItemId: Long,
    val categoryId: Long,
    val createdAt: String,
    val createdAtTimezone: String
)
