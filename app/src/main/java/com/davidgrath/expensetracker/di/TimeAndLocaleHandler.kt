package com.davidgrath.expensetracker.di

import org.threeten.bp.Clock
import org.threeten.bp.ZoneId
import java.util.Locale

interface TimeAndLocaleHandler {
    fun getClock(): Clock
    fun getZone(): ZoneId

    fun getLocale(): Locale

    fun changeClock(clock: Clock)

    /**
     * For testing. Should no-op in regular code
     */
    fun changeZone(zoneId: ZoneId)

    /**
     * For testing. Should no-op in regular code
     */
    fun changeLocale(locale: Locale)
}

