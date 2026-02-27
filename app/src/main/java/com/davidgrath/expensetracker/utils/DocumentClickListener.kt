package com.davidgrath.expensetracker.utils

interface DocumentClickListener {
    /**
     * @param mimeType Needed to choose either PDF activity or image activity
     */
    fun onDocumentClicked(documentId: Long, mimeType: String)
}