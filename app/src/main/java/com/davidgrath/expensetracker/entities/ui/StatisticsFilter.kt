package com.davidgrath.expensetracker.entities.ui

import com.davidgrath.expensetracker.entities.TransactionMode
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

/**
 * For lists, empty is the same as selecting everything
 */
data class StatisticsFilter(
    val accountIds: List<Long> = emptyList(),
    val hasImage: Boolean = false,
    val hasEvidence: Boolean = false,
    val categories: List<Long> = emptyList(),
    val weekdays: List<DayOfWeek> = emptyList(),
    val modes: List<TransactionMode> = emptyList(),
    val startDay: LocalDate? = null,
    val endDay: LocalDate? = null,
)
