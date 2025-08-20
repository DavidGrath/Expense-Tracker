package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.math.BigDecimal

data class TransactionUi(
    val id: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val cashOrCredit: Boolean,
    val timestamp: LocalDateTime,
    val datedDate: LocalDate,
    val datedTime: LocalTime?,
    val seller: SellerUi? = null,
    val items: List<TransactionItemUi>
) {
}