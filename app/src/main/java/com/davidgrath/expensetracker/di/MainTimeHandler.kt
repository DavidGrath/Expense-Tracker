package com.davidgrath.expensetracker.di

import org.threeten.bp.Clock
import org.threeten.bp.ZoneId

class MainTimeHandler: TimeHandler {

    override fun getClock(): Clock {
        return Clock.systemDefaultZone()
    }

    override fun getZone(): ZoneId {
        return ZoneId.systemDefault()
    }

    override fun changeZone(zoneId: ZoneId) {
        //Do Nothing
    }
}