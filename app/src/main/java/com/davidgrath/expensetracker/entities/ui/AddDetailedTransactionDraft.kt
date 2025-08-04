package com.davidgrath.expensetracker.entities.ui

import android.net.Uri

data class AddDetailedTransactionDraft(
    val items: List<AddTransactionPurchaseItem>,
    /**
     * To prevent the user from adding duplicate images
     */
    val imageHashes: Map<Uri, String> = emptyMap()
)
