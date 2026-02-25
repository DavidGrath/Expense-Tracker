package com.davidgrath.expensetracker.entities.ui

data class ImageAddResult(
    val actionNeeded: Boolean,
    val imageModificationDetails: ImageModificationDetails?
)