package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.MonthDay
import org.threeten.bp.temporal.WeekFields
import java.util.Locale

data class TempStatisticsConfig(
    val mode: Mode = Mode.Daily,
    val xDays: Int = 7,
    val useLocalFirstDay: Boolean = true,
    val weeklyFirstDay: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
    val monthlyDayOfMonth: Int = 1,
    val monthDayOfYear: MonthDay = MonthDay.of(Month.JANUARY, 1),
    val rangeStartDay: LocalDate? = null,
    val rangeEndDay: LocalDate? = null
) {
    enum class Mode {
        Daily,
        PastXDays,
        PastWeek,
        Weekly,
        PastMonth,
        Monthly,
        PastYear,
        Yearly,
        Range
    }
}