package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["transactionItemId"]),
        Index(value = ["imageId"]), //Keeping 2 separate indices since I might want to query from either direction
              ],
    foreignKeys = [
        ForeignKey(TransactionItemDb::class, parentColumns = ["id"], childColumns = ["transactionItemId"]),
        ForeignKey(ImageDb::class, parentColumns = ["id"], childColumns = ["imageId"]),
    ]
)
data class TransactionItemImagesDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val transactionItemId: Long,
    val imageId: Long,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
