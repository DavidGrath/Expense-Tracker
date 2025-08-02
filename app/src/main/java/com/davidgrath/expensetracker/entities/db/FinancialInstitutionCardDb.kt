package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Primarily for deriving the Account when using OCR
 */
@Entity
data class FinancialInstitutionCardDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val accountID: Long,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val createdAt: String,
    val createdAtTimezone: String
)
