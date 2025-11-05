package com.davidgrath.expensetracker.entities.ui

import android.net.Uri
import com.davidgrath.expensetracker.entities.TransactionMode
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset

data class AddEditDetailedTransactionDraft(
    val items: List<AddTransactionItem>,
    val accountId: Long,
    val debitOrCredit: Boolean = true,
    val mode: TransactionMode = TransactionMode.Other,
    /**
     * To prevent the user from adding duplicate images
     */
    val imageHashes: Map<String, Uri> = emptyMap(),
    val evidence: List<AddEditTransactionFile> = emptyList(),
    val evidenceHashes: Map<String, Uri> = emptyMap(),
    val note: String? = null,
    val deletedDbItems: List<AddTransactionItem> = emptyList(),
    val deletedDbEvidence: List<AddEditTransactionFile> = emptyList(),
    /**
     * In edit mode, this means nothing
     */
    val dbOriginalDate: LocalDate? = null,
    val dbOriginalTime: LocalTime? = null,
    val customDate: LocalDate? = null,
    val customTime: LocalTime? = null //TODO Observable that ticks every few seconds in the ViewModel
)
