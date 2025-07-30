package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class AddTransactionPurchaseItem(
    val amount: BigDecimal? = null,
    val description: String? = null,
    val category: Category = Category.TEMP_DEFAULT_CATEGORIES.find { it.stringId == "miscellaneous" }!!,
    val showDetails: Boolean = false,
    val brand: String? = null
) {
}