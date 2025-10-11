package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jilt.Builder
import java.math.BigDecimal

@Entity
@Builder
data class TransactionItemDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val transactionId: Long,
    val amount: BigDecimal,
    val brand: String?,
    val quantity: Int = 1,
    val description: String,
    val variation: String,
    val referenceNumber: String?,
    val primaryCategoryId: Long, // val isReduction: Boolean //TODO Basically for withdrawal discounts and income deductions; amount, description, and primaryCategoryId are the main relevant fields if true
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)