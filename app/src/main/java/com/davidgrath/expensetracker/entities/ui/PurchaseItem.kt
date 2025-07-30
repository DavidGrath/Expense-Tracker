package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class PurchaseItem(
    val transaction: Transaction,
    val amount: BigDecimal,
    val description: String,
    val category: Category,
    val brand: String? = null
)
