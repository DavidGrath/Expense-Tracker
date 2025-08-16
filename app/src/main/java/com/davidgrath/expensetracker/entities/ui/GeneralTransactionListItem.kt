package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

data class GeneralTransactionListItem(
    val type: Type,
    val date: LocalDate? = null,
    val transaction: TransactionUi? = null,
    val transactionItem: TransactionItemUi? = null
) {
    enum class Type {
        Date,
        Transaction,
        TransactionItem
    }
}
