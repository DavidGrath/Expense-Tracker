package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SellerLocationDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val sellerID: Long,
    val location: String,
    val isVirtual: Boolean,
    val longitude: Double?,
    val latitude: Double?,
    val address: String?,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)