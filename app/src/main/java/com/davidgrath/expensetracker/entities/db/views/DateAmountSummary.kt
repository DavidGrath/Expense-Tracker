package com.davidgrath.expensetracker.entities.db.views

import org.threeten.bp.LocalDate
import java.math.BigDecimal

data class DateAmountSummary(
    val aggregateDate: LocalDate,
    val sum: BigDecimal
)