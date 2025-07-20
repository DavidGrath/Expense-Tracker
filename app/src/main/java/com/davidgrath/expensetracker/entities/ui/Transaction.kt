package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal
import java.math.BigInteger

data class Transaction(
    val id: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val description: String,
    val isCashless: Boolean,
    val primaryCategory: String,
    val timestamp: ZonedDateTime,
    val recordedTimestamp: ZonedDateTime,
    val store: Store? = null
) {
}