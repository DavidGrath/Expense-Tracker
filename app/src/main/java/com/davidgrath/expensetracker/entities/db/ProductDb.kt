package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A simple template for populating items
 */
@Entity
data class ProductDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val name: String,
    val brand: String,
    val createdAt: String,
    val createdAtTimezone: String
)