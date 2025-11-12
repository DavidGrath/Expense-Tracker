package com.davidgrath.expensetracker.entities.db.views

data class ImageWithStats(
    val id: Long,
    val profileId: Long,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val uri: String,
    val createdAt: String,
    val transactionCount: Long,
    val itemCount: Long
)
