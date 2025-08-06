package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.LocalDateTime
import java.math.BigDecimal

data class TransactionUi(
    val id: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val cashOrCredit: Boolean,
    val timestamp: LocalDateTime,
    val datedTimestamp: LocalDateTime,
    val seller: SellerUi? = null,
    val items: List<TransactionItemUi>
) {
}