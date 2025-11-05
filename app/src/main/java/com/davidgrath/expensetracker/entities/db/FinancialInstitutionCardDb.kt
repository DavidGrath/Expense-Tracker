package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Primarily for deriving the Account when using OCR
 */
@Entity(
    indices = [Index(value = ["accountId"])],
    foreignKeys = [ForeignKey(AccountDb::class, parentColumns = ["id"], childColumns = ["accountId"])]
)
data class FinancialInstitutionCardDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val accountId: Long,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
