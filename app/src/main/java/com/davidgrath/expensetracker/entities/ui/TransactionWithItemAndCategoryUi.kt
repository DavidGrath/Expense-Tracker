package com.davidgrath.expensetracker.entities.ui

import android.net.Uri
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.math.BigDecimal

data class TransactionWithItemAndCategoryUi(
    val transactionId: Long,
    val itemId: Long,
    val accountId: Long,
    val transactionTotal: BigDecimal,
    val itemAmount: BigDecimal,
    val currencyCode: String,
    val cashOrCredit: Boolean,
    val description: String,
    val transactionCreatedAt: LocalDateTime,
    val transactionDatedAt: LocalDate,
    val transactionDatedAtTime: LocalTime?,
    val category: CategoryUi,
    val itemImages: List<Uri>
)