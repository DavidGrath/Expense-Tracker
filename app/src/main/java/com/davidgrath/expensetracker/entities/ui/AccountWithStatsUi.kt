package com.davidgrath.expensetracker.entities.ui

import java.math.BigDecimal

data class AccountWithStatsUi(
    val id: Long,
    val profileId: Long,
    val currencyCode: String,
    val currencyDisplayName: String,
    val name: String,
    val expenses: BigDecimal,
    val income: BigDecimal,
    val transactionCount: Int,
    val itemCount: Int
)
