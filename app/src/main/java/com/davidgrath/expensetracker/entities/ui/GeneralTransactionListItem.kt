package com.davidgrath.expensetracker.entities.ui

data class GeneralTransactionListItem(
    val transactionOrItem: Boolean,
    val transaction: TransactionUi? = null,
    val transactionItem: TransactionItemUi? = null
)
