package com.davidgrath.expensetracker.db

import androidx.room.TypeConverter
import org.threeten.bp.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode

class Converters {
    @TypeConverter
    fun doubleToBigDecimal(value: Double): BigDecimal {
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
    }

    @TypeConverter
    fun bigDecimalToDouble(value: BigDecimal): Double {
        return value.toDouble()
    }

    @TypeConverter
    fun stringToLocalDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString)
    }
}