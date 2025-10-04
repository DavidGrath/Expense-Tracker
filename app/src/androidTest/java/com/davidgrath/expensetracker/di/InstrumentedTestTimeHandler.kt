package com.davidgrath.expensetracker.di

import org.threeten.bp.Clock
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset

class InstrumentedTestTimeHandler: TimeHandler {

    private var zoneId: ZoneId = ZoneId.of("UTC")

    override fun getClock(): Clock {
        return Clock.fixed(LocalDateTime.parse("2025-06-30T08:00:00.000").toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))
    }

    override fun getZone(): ZoneId {
        return zoneId
    }

    override fun changeZone(zoneId: ZoneId) {
        this.zoneId = zoneId
    }
}