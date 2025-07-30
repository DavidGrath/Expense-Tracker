package com.davidgrath.expensetracker.entities.db

import java.math.BigDecimal

data class PurchaseItemDb(
    val transactionId: Long,
    val amount: BigDecimal,
    val description: String,
    val categoryId: Long,
    val brand: String? = null
)