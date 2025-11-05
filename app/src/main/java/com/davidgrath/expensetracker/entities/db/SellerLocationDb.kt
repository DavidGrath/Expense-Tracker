package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["sellerId"])],
    foreignKeys = [ForeignKey(SellerDb::class, parentColumns = ["id"], childColumns = ["sellerId"])]
)
data class SellerLocationDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val sellerId: Long,
    /**
     * Short, descriptive name, basically "branch name"
     */
    val location: String,
    val isVirtual: Boolean,
    val longitude: Double?,
    val latitude: Double?,
    /**
     * The kind of address you'd put in Google Maps
     */
    val address: String?,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)