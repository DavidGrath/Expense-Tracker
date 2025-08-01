package com.davidgrath.expensetracker.entities.db

import java.math.BigDecimal

data class PurchaseItemDb(
    val id: Long,
    val transactionId: Long,
    val amount: BigDecimal,
    val description: String,
    val categoryId: Long,
    val brand: String? = null
)