package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.jilt.Builder
import java.math.BigDecimal

@Entity(
    indices = [
        Index("transactionId","ordinal", unique = true),
        Index("primaryCategoryId")
              ],
    foreignKeys = [
        ForeignKey(entity = TransactionDb::class, parentColumns = ["id"], childColumns = ["transactionId"]),
        ForeignKey(entity = CategoryDb::class, parentColumns = ["id"], childColumns = ["primaryCategoryId"]),
    ]
    )
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
    val primaryCategoryId: Long,
    /**
     * Basically for withdrawal discounts and income deductions; amount, description, and primaryCategoryId are the main relevant fields if true
     */
    val isReduction: Boolean,
    val ordinal: Int,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)