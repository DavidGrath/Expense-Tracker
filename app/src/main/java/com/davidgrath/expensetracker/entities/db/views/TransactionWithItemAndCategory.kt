package com.davidgrath.expensetracker.entities.db.views

import java.math.BigDecimal

data class TransactionWithItemAndCategory(
    val transactionId: Long,
    val itemId: Long,
    val accountId: Long,
    val primaryCategoryId: Long,
    val transactionTotal: BigDecimal,
    val itemAmount: BigDecimal,
    val currencyCode: String,
    val debitOrCredit: Boolean,
    val description: String,
    val transactionCreatedAt: String,
    val transactionCreatedAtOffset: String,
    val transactionCreatedAtTimezone: String,
    val transactionDatedAt: String,
    val transactionDatedAtTime: String?,
    val categoryStringId: String?,
    val categoryIsCustom: Boolean,
    val categoryName: String?,
)