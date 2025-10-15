package com.davidgrath.expensetracker.di

import org.threeten.bp.Clock
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import java.util.Locale

class TestTimeAndLocaleHandler: TimeAndLocaleHandler {

    private var zoneId: ZoneId = ZoneId.of("UTC")
    private var locale: Locale = Locale.US
    private var clock = Clock.fixed(LocalDateTime.parse("2025-06-30T08:00:00.000").toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))

    override fun getClock(): Clock {
        return clock
    }

    override fun getZone(): ZoneId {
        return zoneId
    }

    override fun getLocale(): Locale {
        return locale
    }

    override fun changeClock(clock: Clock) {
        this.clock = clock
    }

    override fun changeZone(zoneId: ZoneId) {
        this.zoneId = zoneId
    }

    override fun changeLocale(locale: Locale) {
        this.locale = locale
    }
}