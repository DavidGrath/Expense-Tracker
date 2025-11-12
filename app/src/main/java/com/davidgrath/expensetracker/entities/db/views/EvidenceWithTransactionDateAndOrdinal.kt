package com.davidgrath.expensetracker.entities.db.views

data class EvidenceWithTransactionDateAndOrdinal(
    val id: Long,
    val transactionId: Long,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val uri: String, //TODO Add document type, ordinal
    val transactionDatedAt: String,
    val transactionOrdinal: Int
)
