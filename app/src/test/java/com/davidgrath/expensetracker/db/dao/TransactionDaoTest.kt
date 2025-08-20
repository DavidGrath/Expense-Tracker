package com.davidgrath.expensetracker.db.dao

import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.entities.db.TransactionDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class TransactionDaoTest {

    //TODO Setup dummy data
    @Inject
    lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        (ApplicationProvider.getApplicationContext<TestExpenseTracker>().appComponent as TestComponent).inject(this)
    }

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

    @Test
    @Ignore("To test in next commit")
    fun sumByDateTest() {
        val transactionTime = "06:00"
        val transactionOffset = "+05:00"
        val transactionTimezone = "America/New_York"

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        transactionDao.insertTransaction(TransactionDb(null, 0, BigDecimal(110.00), "USD", false, "2025-08-18T08:00:00", "+00:00", "America/New_York", 0, firstTransactionDate.toString(), transactionTime, transactionOffset, transactionTimezone))
        transactionDao.insertTransaction(TransactionDb(null, 0, BigDecimal(300.00), "USD", false, "2025-08-18T08:01:00", "+00:00", "America/New_York", 0, secondTransactionDate.toString(), transactionTime, transactionOffset, transactionTimezone))
        transactionDao.insertTransaction(TransactionDb(null, 0, BigDecimal(450.00), "USD", false, "2025-08-18T08:02:00", "+00:00", "America/New_York", 0, thirdTransactionDate.toString(), transactionTime, transactionOffset, transactionTimezone))
        transactionDao.insertTransaction(TransactionDb(null, 0, BigDecimal(750.00), "USD", false, "2025-08-18T08:02:00", "+00:00", "America/New_York", 0, fourthTransactionDate.toString(), transactionTime, transactionOffset, transactionTimezone))
        transactionDao.insertTransaction(TransactionDb(null, 0, BigDecimal(1000.00), "USD", false, "2025-08-18T08:02:00", "+00:00", "America/New_York", 0, fifthTransactionDate.toString(), transactionTime, transactionOffset, transactionTimezone))

        val sums = transactionDao.getTransactionSumByDateFrom(firstTransactionDate.toString()).blockingFirst()
        val firstSum = sums.find { it.aggregateDate == firstTransactionDate }!!
        val secondSum = sums.find { it.aggregateDate == secondTransactionDate }!!
        val thirdSum = sums.find { it.aggregateDate == thirdTransactionDate }!!
        assertEquals(BigDecimal(110.00), firstSum)
        assertEquals(BigDecimal(300.00), secondSum)
        assertEquals(BigDecimal(1200.00), thirdSum)

        val boundSums = transactionDao.getTransactionSumByDateFromTo(firstTransactionDate.toString(), fourthTransactionDate.toString()).blockingFirst()
        val fourthSum = boundSums.find { it.aggregateDate == firstTransactionDate }
        assertNull(fourthSum)
    }

    @Test
    @Ignore("To test in next commit")
    fun totalSpentTest() {

        val time = LocalTime.of(8, 0, 0)
        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val builder = TestBuilder.defaultTransactionBuilder()
        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt(firstTransactionDate.atTime(time).toString()).datedAt(firstTransactionDate.toString()).build())
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt(secondTransactionDate.atTime(time).toString()).datedAt(secondTransactionDate.toString()).build())
        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt(thirdTransactionDate.atTime(time).toString()).datedAt(thirdTransactionDate.toString()).build())
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt(fourthTransactionDate.atTime(time).toString()).datedAt(fourthTransactionDate.toString()).build())
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt(fifthTransactionDate.atTime(time).toString()).datedAt(fifthTransactionDate.toString()).build())

        val firstSum = transactionDao.getTransactionSumFrom(firstTransactionDate.toString()).blockingFirst()

        assertEquals(BigDecimal(2610.00), firstSum)

        val secondSum = transactionDao.getTransactionSumFromTo(firstTransactionDate.toString(), fourthTransactionDate.toString()).blockingFirst()
        assertEquals(BigDecimal(1610.00), secondSum)
    }
}