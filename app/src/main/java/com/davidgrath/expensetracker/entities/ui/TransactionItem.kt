package com.davidgrath.expensetracker.entities.ui

data class TransactionItem(
    val transactionOrItem: Boolean,
    val transaction: TransactionUi? = null,
    val purchaseItem: PurchaseItemUi? = null
)
