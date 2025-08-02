package com.davidgrath.expensetracker.db.dao

import org.junit.Ignore
import org.junit.Test

class TransactionDaoTest {

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionAtUTCTimeAndTransactionTimeOutsideLocalOffsetDayWhenFetchByLocalOffsetThenTransactionNotPresent() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionAtUTCTimeAndTransactionTimeWithinLocalOffsetDayWhenFetchByLocalOffsetThenTransactionPresent() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionAtUTCTimeAndTransactionTimeOutsideUTCDayWhenFetchByUTCThenTransactionNotPresent() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionAtUTCTimeAndTransactionTimeWithinUTCDayWhenFetchByUTCThenTransactionPresent() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionAtUTCTimeAndTransactionTimeOutsideLocalOffsetDayAndTransactionWithinTransactionOffsetDayWhenFetchByTransactionOffsetThenTransactionPresent() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionAtUTCTimeAndTransactionTimeWithinLocalOffsetDayAndTransactionOutsideTransactionOffsetDayWhenFetchByTransactionOffsetThenTransactionNotPresent() {

    }
}