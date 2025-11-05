package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["transactionItemId"]),
        Index(value = ["categoryId"])
              ],
    foreignKeys = [
        ForeignKey(TransactionItemDb::class, parentColumns = ["id"], childColumns = ["transactionItemId"]),
        ForeignKey(CategoryDb::class, parentColumns = ["id"], childColumns = ["categoryId"]),
    ]
)
data class TransactionItemCategoriesDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val transactionItemId: Long,
    val categoryId: Long,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)
