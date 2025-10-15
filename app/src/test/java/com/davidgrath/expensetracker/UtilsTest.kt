package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.di.TestTimeAndLocaleHandler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

@RunWith(JUnit4::class)
class UtilsTest {

    @Test
    fun timezoneConversionTest() {
        val timeHandler = TestTimeAndLocaleHandler()
        timeHandler.changeZone(ZoneId.of("Pacific/Honolulu"))
        val transactionDateTime = "2025-06-30T08:00"
        val offset = "Z"

        val convertedDateTime = offsetTimeToLocalTime(timeHandler, transactionDateTime, offset)

        val expectedDateTime = LocalDateTime.parse("2025-06-29T22:00:00")
        assertEquals(expectedDateTime, convertedDateTime)
    }
}