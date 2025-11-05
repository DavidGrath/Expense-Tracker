package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A simple template for populating items
 */
@Entity(
    indices = [Index(value = ["profileId"])],
    foreignKeys = [ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["profileId"])]
)
data class ProductDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val name: String,
    val brand: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)