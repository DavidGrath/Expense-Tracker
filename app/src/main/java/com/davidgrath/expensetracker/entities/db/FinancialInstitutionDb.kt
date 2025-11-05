package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An entity that holds accounts
 */
@Entity(
    indices = [Index(value = ["profileId"])],
    foreignKeys = [ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["profileId"])]
)
data class FinancialInstitutionDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val name: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
