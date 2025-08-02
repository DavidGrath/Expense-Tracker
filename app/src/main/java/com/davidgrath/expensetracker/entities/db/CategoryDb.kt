package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CategoryDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileID: Long,
    val stringID: String?,
    val isCustom: Boolean,
    val name: String?,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
