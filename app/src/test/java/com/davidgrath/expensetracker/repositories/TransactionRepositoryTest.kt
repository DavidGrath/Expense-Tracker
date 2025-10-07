package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.di.TestComponent
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

        val accountId = getDefaultAccountId()
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal.ZERO)
        //Empty Set
        var transactionSumByDates = transactionRepository.getTotalSpentByDate("2025-07-01").blockingFirst()
        assertEquals(0, transactionSumByDates.size)

        //Today and empty
        transactionSumByDates = transactionRepository.getTotalSpentByDate("2025-06-30").blockingFirst()
        assertEquals(1, transactionSumByDates.size)
        assertEquals(0, BigDecimal.ZERO.compareTo(transactionSumByDates.first().sum))

        //Multiple days and empty
        transactionSumByDates = transactionRepository.getTotalSpentByDate("2025-06-01").blockingFirst()
        val daysInJune = 30
        val grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        assertEquals(daysInJune, transactionSumByDates.size)
        assertEquals(0, BigDecimal.ZERO.compareTo(grandSum))

        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt(firstTransactionDate.atTime(time).toString()).datedAt(firstTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt(secondTransactionDate.atTime(time).toString()).datedAt(secondTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        //Just 1 transaction
        transactionSumByDates = transactionRepository.getTotalSpentByDate("2025-01-02").blockingFirst()
        val daysBetweenJan2AndJun30Inclusive = 180
        assertEquals(daysBetweenJan2AndJun30Inclusive, transactionSumByDates.size)

        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt(thirdTransactionDate.atTime(time).toString()).datedAt(thirdTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt(fourthTransactionDate.atTime(time).toString()).datedAt(fourthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt(fifthTransactionDate.atTime(time).toString()).datedAt(fifthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        transactionSumByDates = transactionRepository.getTotalSpentByDate("2025-01-02").blockingFirst()
        val sum1 = transactionSumByDates.find { it.aggregateDate == firstTransactionDate }!!
        val missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!
        val missingSum2 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-08") }!!
        val missingSum3 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-10") }!!
        assertEquals(0, BigDecimal(410).compareTo(sum1.sum))
        assertEquals(0, BigDecimal.ZERO.compareTo(missingSum1.sum))
        assertEquals(0, BigDecimal.ZERO.compareTo(missingSum2.sum))
        assertEquals(0, BigDecimal.ZERO.compareTo(missingSum3.sum))
    }

    fun getDefaultAccountId(): Long {
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val accountId = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().firstOrNull()!!.id
        return accountId!!
    }

}