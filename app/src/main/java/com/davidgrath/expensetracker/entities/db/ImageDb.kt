package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ImageDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?, //TODO I just realized that Images have no profile ID. Fix later
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val uri: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
