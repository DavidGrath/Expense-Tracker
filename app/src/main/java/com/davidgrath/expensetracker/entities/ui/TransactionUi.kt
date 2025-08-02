package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal

data class TransactionUi(
    val id: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val isCashless: Boolean,
    val timestamp: LocalDateTime,
    val datedTimestamp: LocalDateTime,
    val seller: SellerUi? = null,
    val items: List<PurchaseItemUi>
) {
}