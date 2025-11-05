package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import java.math.BigDecimal

data class TransactionDetailsUi(
    val id: Long,
    val accountName: String,
    val accountCurrencyCode: String,
    val accountReferenceNumber: String?,
    val amount: BigDecimal,
    val currencyCode: String,
    val debitOrCredit: Boolean,
    val note: String?,
    val timestamp: LocalDateTime,
    val datedDate: LocalDate,
    val datedTime: LocalTime?,
    val seller: SellerUi? = null,
) {
}