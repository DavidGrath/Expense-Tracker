package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FinancialInstitutionDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val name: String,
    val createdAt: String,
    val createdAtTimezone: String
)
