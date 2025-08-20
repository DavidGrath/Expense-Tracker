package com.davidgrath.expensetracker.entities.db.views

import java.math.BigDecimal

data class ItemSumByCategory(
    val categoryId: Long,
    val stringID: String?,
    val isCustom: Boolean,
    val name: String?,
    val sum: BigDecimal
)