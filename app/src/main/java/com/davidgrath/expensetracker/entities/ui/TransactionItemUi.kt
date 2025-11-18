package com.davidgrath.expensetracker.entities.ui

import android.net.Uri
import java.math.BigDecimal

data class TransactionItemUi(
    val transaction: TransactionUi,
    val amount: BigDecimal,
    val description: String,
    val category: CategoryUi,
    val isLast: Boolean,
    val brand: String? = null,
    val images: List<Uri> = emptyList()
)
