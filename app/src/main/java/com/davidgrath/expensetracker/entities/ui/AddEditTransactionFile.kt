package com.davidgrath.expensetracker.entities.ui

import android.net.Uri

data class AddEditTransactionFile(
    val dbId: Long?,
    val uri: Uri,
    val mimeType: String,
    val sha256: String,
    val sizeBytes: Long,
    /**
     * Should always be false if the item is new
     */
    val dbIsLinked: Boolean = false
)