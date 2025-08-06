package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class AddTransactionItem(
    val id: Int,
    val amount: BigDecimal? = null,
    val description: String? = null,
    val category: CategoryUi = CategoryUi.TEMP_DEFAULT_CATEGORIES.find { it.stringId == "miscellaneous" }!!,
    val showDetails: Boolean = false,
    val brand: String? = null,
    val images: List<String> = emptyList()
) {
}