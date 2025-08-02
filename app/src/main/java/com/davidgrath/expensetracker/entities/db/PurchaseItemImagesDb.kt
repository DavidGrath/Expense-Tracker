package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PurchaseItemImagesDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val purchaseItemID: Long,
    val imageID: Long,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
