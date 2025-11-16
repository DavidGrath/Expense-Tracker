package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class TransactionDetailItemUi(
    val id: Long,
    val transactionId: Long,
    val amount: BigDecimal,
    val accountCurrencyCode: String,
    val description: String,
    val primaryCategory: CategoryUi,
    val otherCategories: List<CategoryUi>,
    val brand: String? = null,
    val quantity: Int,
    val referenceNumber: String?,
    val variation: String?,
    val isReduction: Boolean,
    val images: List<ImageUi> = emptyList()
)
