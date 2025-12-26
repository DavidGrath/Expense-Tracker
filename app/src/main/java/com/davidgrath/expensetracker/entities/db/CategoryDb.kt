package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["profileId"])],
    foreignKeys = [
        ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["profileId"])
    ]
)
data class CategoryDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String?,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String,
    val icon: String
)
