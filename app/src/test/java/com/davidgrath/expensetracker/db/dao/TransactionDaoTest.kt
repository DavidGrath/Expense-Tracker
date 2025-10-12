package com.davidgrath.expensetracker.db.dao

import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class TransactionDaoTest {

    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository

    @Before
    fun setUp() {
        (ApplicationProvider.getApplicationContext<TestExpenseTracker>().appComponent as TestComponent).inject(this)
    }

    //Ignore zones for now
    @Test //D
    fun givenTransactionTimeOutsideSpecifiedOffsetDayAndUseSpecifiedOffsetWhenFetchThenTransactionNotPresent() {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100)).datedAt("2025-06-30") // 8 AM UTC
        val honolulu = builder.datedAtOffset("-10:00").datedAtTimezone("Pacific/Honolulu").build()  // 10 PM Honolulu Previous day, 6 AM Noronha
        val guamBuilder = builder.datedAtOffset("+10:00").datedAtTimezone("Pacific/Guam") // 6 PM Guam, 6 AM Noronha
        val guam = guamBuilder.build()
        val guam2 = guamBuilder.datedAtTime("00:00:00").build()  // 10 AM Guam, 10 PM Noronha previous day
        transactionDao.insertTransaction(honolulu).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam2).subscribeOn(Schedulers.io()).blockingSubscribe()
        //Starting from 2025-06-30T02:00:00Z
        val transactions = transactionDao.getAllFromSpecifiedOffsetSingle("2025-06-30", "-02:00").subscribeOn(Schedulers.io()).blockingGet() // America/Noronha
        LOGGER.debug("transactions: {}", transactionDao.getAllTemp().subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(2, transactions.size)
        assertEquals("Pacific/Honolulu", transactions[0].datedAtTimezone)
        assertEquals("Pacific/Guam", transactions[1].datedAtTimezone)
    }

    @Test //C
    fun givenTransactionTimeWithinSpecifiedOffsetDayAndUseSpecifiedOffsetWhenFetchThenTransactionPresent() {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100)).datedAt("2025-06-30")
        val honolulu = builder.datedAtOffset("-10:00").datedAtTimezone("Pacific/Honolulu").build()
        val guamBuilder = builder.datedAtOffset("+10:00").datedAtTimezone("Pacific/Guam")
        val guam = guamBuilder.build()
        val guam2 = guamBuilder.datedAtTime("00:00:00").build()
        transactionDao.insertTransaction(honolulu).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam2).subscribeOn(Schedulers.io()).blockingSubscribe()
        //Starting from 2025-06-29T02:00:00
        val transactions = transactionDao.getAllFromToSpecifiedOffsetSingle("2025-06-29", "2025-06-30", "-02:00").subscribeOn(Schedulers.io()).blockingGet() // America/Noronha
        LOGGER.debug("transactions: {}", transactionDao.getAllTemp().subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(3, transactions.size)
    }

    @Test //A
    fun givenTransactionTimeWithinTransactionOffsetDayAndUseTransactionOffsetWhenFetchThenTransactionPresent() {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100)).datedAt("2025-06-30")
        val honolulu = builder.datedAtOffset("-10:00").datedAtTimezone("Pacific/Honolulu").build()
        val guamBuilder = builder.datedAtOffset("+10:00").datedAtTimezone("Pacific/Guam")
        val guam = guamBuilder.build()
        val guam2 = guamBuilder.datedAtTime("00:00:00").build()
        transactionDao.insertTransaction(honolulu).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam2).subscribeOn(Schedulers.io()).blockingSubscribe()
        val transactions = transactionDao.getAllFromTransactionOffsetSingle("2025-06-29").subscribeOn(Schedulers.io()).blockingGet()
        LOGGER.debug("transactions: {}", transactionDao.getAllTemp().subscribeOn(Schedulers.io()).blockingGet())
        assertEquals(3, transactions.size)
    }

    @Test //B
    fun givenTransactionTimeOutsideTransactionOffsetDayAndUseTransactionOffsetWhenFetchThenTransactionNotPresent() {
        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100)).datedAt("2025-06-30") // 8 AM UTC
        val honolulu = builder.datedAtOffset("-10:00").datedAtTimezone("Pacific/Honolulu").build() // 10 PM Honolulu previous day
        val guamBuilder = builder.datedAtOffset("+10:00").datedAtTimezone("Pacific/Guam") // 6 PM Guam
        val guam = guamBuilder.build()
        val guam2 = guamBuilder.datedAtTime("00:00:00").build() // 12 AM Guam
        transactionDao.insertTransaction(honolulu).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(guam2).subscribeOn(Schedulers.io()).blockingSubscribe()
        val transactions = transactionDao.getAllFromTransactionOffsetSingle("2025-06-30").subscribeOn(Schedulers.io()).blockingGet()
        LOGGER.debug("transactions: {}", transactions)
        assertEquals(2, transactions.size)
    }

    @Test
    fun sumByDateTest() {
        val transactionTime = "06:00"
        val transactionOffset = "+05:00"
        val transactionTimezone = "America/New_York"

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal.ZERO).currencyCode("USD").createdAtTimezone("America/New_York").datedAtTime(transactionTime).datedAtOffset(transactionOffset).datedAtTimezone(transactionTimezone)
        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt("2025-06-18T08:00:00").createdAtOffset("+00:00").datedAt(firstTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt("2025-06-18T08:01:00").createdAtOffset("+00:00").datedAt(secondTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt("2025-06-18T08:02:00").createdAtOffset("+00:00").datedAt(thirdTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt("2025-06-18T08:02:00").createdAtOffset("+00:00").datedAt(fourthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt("2025-06-18T08:02:00").createdAtOffset("+00:00").datedAt(fifthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        val sums = transactionDao.getTransactionSumByDate(true, firstTransactionDate.toString()).blockingFirst()
        val firstSum = sums.find { it.aggregateDate == firstTransactionDate }!!
        val secondSum = sums.find { it.aggregateDate == secondTransactionDate }!!
        val thirdSum = sums.find { it.aggregateDate == thirdTransactionDate }!!
        assertEquals(0, BigDecimal(110.00).compareTo(firstSum.sum))
        assertEquals(0, BigDecimal(300.00).compareTo(secondSum.sum))
        assertEquals(0, BigDecimal(1200.00).compareTo(thirdSum.sum))

        val boundSums = transactionDao.getTransactionSumByDate(true, firstTransactionDate.toString(), fourthTransactionDate.toString()).blockingFirst()
        val fourthSum = boundSums.find { it.aggregateDate == fifthTransactionDate }
        assertNull(fourthSum)
    }

    @Test
    fun totalSpentTest() {

        val time = LocalTime.of(8, 0, 0)
        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal.ZERO)
        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt(firstTransactionDate.atTime(time).toString()).datedAt(firstTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt(secondTransactionDate.atTime(time).toString()).datedAt(secondTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt(thirdTransactionDate.atTime(time).toString()).datedAt(thirdTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt(fourthTransactionDate.atTime(time).toString()).datedAt(fourthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt(fifthTransactionDate.atTime(time).toString()).datedAt(fifthTransactionDate.toString()).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        val firstSum = transactionDao.getTransactionDebitSum(firstTransactionDate.toString(), null).blockingFirst()

        assertEquals(0, BigDecimal(2610.00).compareTo(firstSum))

        val secondSum = transactionDao.getTransactionDebitSum(firstTransactionDate.toString(), fourthTransactionDate.toString()).blockingFirst()
        assertEquals(0, BigDecimal(1610.00).compareTo(secondSum))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionDaoTest::class.java)
    }
}