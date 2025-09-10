package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AccountDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val currencyCode: String,
    val financialInstitutionId: Long?,
    val referenceNumber: String, //I could probably make a better name for this
    val isCashless: Boolean,
    val name: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
