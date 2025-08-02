package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity
data class PurchaseItemDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val transactionId: Long,
    val amount: BigDecimal,
    val brand: String?,
    val quantity: Int = 1,
    val description: String,
    val variation: String,
    val referenceNumber: String?,
    val primaryCategoryId: Long,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)