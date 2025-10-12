package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.getDefaultAccountId
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class TransactionRepositoryTest {

    //TODO Inject properly, setup dummy data
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    @Ignore("Maybe move to Activity Test(s)")
    fun givenTransactionIsOneDayBeforeWhenSelectByLastNDaysThenTransactionNotSelected() {

    }

    @Test
    @Ignore("Maybe move to Activity Test(s)")
    fun givenTransactionIsOneDayBeforeWhenFetchByPastMonthThenTransactionNotSelected() {

    }

    @Test
    fun givenTransactionRangeDoesNotHaveDatesInIntervalWhenFetchTotalThenDatesIntervalHaveZeroes() {
        val time = LocalTime.of(8, 0, 0)
        val firstTransactionDate = LocalDate.parse("2025-01-02")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-07")
        val fourthTransactionDate = LocalDate.parse("2025-01-09")
//        val fifthTransactionDate = LocalDate.parse("2025-01-11")
        val fifthTransactionDate = LocalDate.parse("2025-07-11")

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal.ZERO)
        //Empty Set
        var transactionSumByDates = transactionRepository.getTotalAmountByDate(true, "2025-07-01").blockingFirst()
        assertEquals(0, transactionSumByDates.size)

        //Today and empty
        transactionSumByDates = transactionRepository.getTotalAmountByDate(true, "2025-06-30").blockingFirst()
        assertEquals(1, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, transactionSumByDates.first().sum)

        //Multiple days and empty
        transactionSumByDates = transactionRepository.getTotalAmountByDate(true, "2025-06-01").blockingFirst()
        val daysInJune = 30
        var grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        assertEquals(daysInJune, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, grandSum)

        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt(firstTransactionDate.atTime(time).toString()).datedAt(firstTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt(secondTransactionDate.atTime(time).toString()).datedAt(secondTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        //Just 1 transaction
        transactionSumByDates = transactionRepository.getTotalAmountByDate(true, "2025-01-02").blockingFirst()
        val daysBetweenJan2AndJun30Inclusive = 180
        assertEquals(daysBetweenJan2AndJun30Inclusive, transactionSumByDates.size)

        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt(thirdTransactionDate.atTime(time).toString()).datedAt(thirdTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt(fourthTransactionDate.atTime(time).toString()).datedAt(fourthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt(fifthTransactionDate.atTime(time).toString()).datedAt(fifthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        transactionSumByDates = transactionRepository.getTotalAmountByDate(true, "2025-01-02").blockingFirst()
        val sum1 = transactionSumByDates.find { it.aggregateDate == firstTransactionDate }!!
        var missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!
        val missingSum2 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-08") }!!
        val missingSum3 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-10") }!!
        assertEqualsBD(BigDecimal(410), sum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum2.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum3.sum)

        transactionSumByDates = transactionRepository.getTotalAmountByDate(true, firstTransactionDate.toString(), thirdTransactionDate.toString()).blockingFirst()
        val daysBetweenFirstAndThirdTransactionInclusive = 6

        grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!

        assertEquals(daysBetweenFirstAndThirdTransactionInclusive, transactionSumByDates.size)
        assertEqualsBD(BigDecimal(860), grandSum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
    }

}