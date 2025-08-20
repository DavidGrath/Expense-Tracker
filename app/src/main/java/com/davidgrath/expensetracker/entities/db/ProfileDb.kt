package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["stringId"], unique = true)])
data class ProfileDb(
    @PrimaryKey val id: Long?,
    val name: String,
    /**
     * Meant for the SharedPreferences file name
     */
    val stringId: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
