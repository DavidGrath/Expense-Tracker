package com.davidgrath.expensetracker.entities.db

import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal

data class TransactionDb(
    val id: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val isCashless: Boolean,
    val timestamp: ZonedDateTime,
    val datedTimestamp: ZonedDateTime,
)