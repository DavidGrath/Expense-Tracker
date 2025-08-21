package com.davidgrath.expensetracker.entities.ui

import android.net.Uri

data class AddTransactionEvidence(
    val uri: Uri,
    val mimeType: String,
    val sha256: String
)