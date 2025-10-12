package com.davidgrath.expensetracker.entities.db.views

import java.math.BigDecimal

data class AccountWithStats(
    val id: Long,
    val profileId: Long,
    val currencyCode: String,
    val financialInstitutionId: Long?,
    val referenceNumber: String,
    val name: String,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String,
    val expenses: BigDecimal,
    val income: BigDecimal,
    val transactionCount: Int,
    val itemCount: Int //TODO Account for reductions
)
