package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An entity that sells items
 */
@Entity
data class SellerDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val profileId: Long,
    val name: String,
    val createdAt: String,
    val createdAtTimezone: String
)