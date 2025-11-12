package com.davidgrath.expensetracker

import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.number.Precision
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import systems.uom.unicode.CLDR.BYTE
import tech.units.indriya.format.NumberDelimiterQuantityFormat
import tech.units.indriya.format.SimpleQuantityFormat
import tech.units.indriya.format.SimpleUnitFormat
import tech.units.indriya.quantity.Quantities
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import javax.measure.BinaryPrefix

@RunWith(JUnit4::class)
class UtilsTest {

    @Test
    fun formatTest() {

//        val unit = BinaryPrefix.KIBI(BYTE)
        val unit = BYTE
        val quantity = Quantities.getQuantity(1_000.23, unit)
        val format = SimpleQuantityFormat.getInstance()
        val quantityFormat = NumberDelimiterQuantityFormat.getInstance(NumberFormat.getNumberInstance(), SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII))
        println(quantityFormat.format(quantity))
    }

    @Test
    fun decimalFormatTest() {
        val bigDecimal = BigDecimal("1234567.891")
        val german = Locale("de", "DE")
        val indian = Locale("en", "IN")
        val numberFormatterSettings = NumberFormatter.with().grouping(NumberFormatter.GroupingStrategy.ON_ALIGNED).precision(Precision.fixedFraction(2))
        var numberFormatter = numberFormatterSettings.locale(german)

        assertEquals("1.234.567,89", numberFormatter.format(bigDecimal).toString())

        numberFormatter = numberFormatterSettings.locale(indian)

        assertEquals("12,34,567.89", numberFormatter.format(bigDecimal).toString())
    }
}