package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An entity that sells items
 */
@Entity(
    indices = [Index(value = ["profileId"])],
    foreignKeys = [ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["profileId"])]
)
data class SellerDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val name: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)