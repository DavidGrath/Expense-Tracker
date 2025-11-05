package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class AddTransactionItem(
    val id: Int,
    val dbId: Long?,
    val category: CategoryUi,
    val amount: BigDecimal? = null,
    val description: String? = null,
    val showDetails: Boolean = false,
    val brand: String? = null,
    val variation: String = "",
    val referenceNumber: String? = null,
    val quantity: Int = 1,
    val isReduction: Boolean = false,
    val ordinal: Int = 0,
    val images: List<AddEditTransactionFile> = emptyList(),
    val deletedDbImages: List<AddEditTransactionFile> = emptyList()
) {
}