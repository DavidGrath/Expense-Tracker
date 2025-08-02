package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ImageDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val sizeBytes: Int,
    val sha256: String,
    val mimeType: String,
    val uri: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
