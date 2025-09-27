package com.davidgrath.expensetracker.entities.ui

import android.net.Uri
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

data class AddEditDetailedTransactionDraft(
    val items: List<AddTransactionItem>,
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
     * In edit mode, this means TODO nothing? allow edit existing date time?
     */
    val useCustomDateTime: Boolean = false,
    val customDate: LocalDate? = null,
    val customTime: LocalTime? = null //TODO Observable that ticks every few seconds in the ViewModel
)
