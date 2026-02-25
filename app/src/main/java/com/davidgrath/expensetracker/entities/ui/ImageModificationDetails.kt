package com.davidgrath.expensetracker.entities.ui

data class ImageModificationDetails(
    val originalFileSize: Long,
    val itemId: Int?,
    val originalFileHash: String,
    val fileMimeType: String,
    /**
     * Non-null implies it was too large
     */
    val reducedFileSize: Long?,
    val originalImageWidthHeight: Pair<Int, Int>?,
    val reducedImageWidthHeight: Pair<Int, Int>?,
    val locationLongLat: Pair<Double, Double>?
)
