package com.davidgrath.expensetracker.di

import org.threeten.bp.Clock
import org.threeten.bp.ZoneId

interface TimeHandler {
    fun getClock(): Clock
    fun getZone(): ZoneId

    /**
     * For testing. Should no-op in regular code
     */
    fun changeZone(zoneId: ZoneId)
}

