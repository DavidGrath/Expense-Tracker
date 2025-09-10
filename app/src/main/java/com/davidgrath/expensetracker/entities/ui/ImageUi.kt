package com.davidgrath.expensetracker.entities.ui

import android.net.Uri
import org.threeten.bp.LocalDateTime

data class ImageUi(
    val id: Long,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val uri: Uri,
    val timestamp: LocalDateTime
)