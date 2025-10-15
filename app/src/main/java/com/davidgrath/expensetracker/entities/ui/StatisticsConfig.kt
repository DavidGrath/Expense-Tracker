package com.davidgrath.expensetracker.entities.ui

import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.MonthDay
import org.threeten.bp.temporal.WeekFields
import java.util.Locale

data class StatisticsConfig(
    val locale: Locale,
    val dateMode: DateMode = DateMode.Daily,
    val xDays: Int = 1,
    val useLocalFirstDay: Boolean = true,
    val weeklyFirstDay: DayOfWeek = WeekFields.of(locale).firstDayOfWeek,
    val monthlyDayOfMonth: Int = 1,
    val monthDayOfYear: MonthDay = MonthDay.of(Month.JANUARY, 1),
    val rangeStartDay: LocalDate? = null,
    val rangeEndDay: LocalDate? = null,
    val xLyOffset: Int = 0,
    val filter: StatisticsFilter = StatisticsFilter()
) {
    enum class DateMode {
        Daily,
        PastXDays,
        PastWeek,
        Weekly,
        PastMonth,
        Monthly,
        PastYear,
        Yearly,
        Range,
        All
    }
}