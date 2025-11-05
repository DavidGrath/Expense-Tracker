package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index("profileId", "sha256", unique = true)],
    foreignKeys = [ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["profileId"])]
)
data class ImageDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val uri: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
