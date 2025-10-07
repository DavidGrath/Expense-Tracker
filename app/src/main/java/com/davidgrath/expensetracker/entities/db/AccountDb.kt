package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AccountDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    /**
     * A java.util.Currency code, which is ISO 4217
     */
    val currencyCode: String,
    val financialInstitutionId: Long?,
    val referenceNumber: String, //I could probably make a better name for this
    val name: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
