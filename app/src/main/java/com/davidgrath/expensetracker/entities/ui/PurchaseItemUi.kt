package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class PurchaseItemUi(
    val transaction: TransactionUi,
    val amount: BigDecimal,
    val description: String,
    val category: CategoryUi,
    val brand: String? = null
)
