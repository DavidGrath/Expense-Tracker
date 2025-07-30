package com.davidgrath.expensetracker.entities.ui

data class TransactionItem(
    val transactionOrItem: Boolean,
    val transaction: Transaction? = null,
    val purchaseItem: PurchaseItem? = null
)
