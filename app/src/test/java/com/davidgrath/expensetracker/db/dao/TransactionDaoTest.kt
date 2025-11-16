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
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal.ZERO).currencyCode("USD").createdAtTimezone("America/New_York").datedAtTime(transactionTime)
        var ordinal = 1
        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt("2025-06-18T08:00:00").createdAtOffset("+00:00").datedAt(firstTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt("2025-06-18T08:01:00").createdAtOffset("+00:00").datedAt(secondTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt("2025-06-18T08:02:00").createdAtOffset("+00:00").datedAt(thirdTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt("2025-06-18T08:02:00").createdAtOffset("+00:00").datedAt(fourthTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt("2025-06-18T08:02:00").createdAtOffset("+00:00").datedAt(fifthTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        val sums = transactionDao.getTransactionSumByDate(profile.id!!, true, firstTransactionDate.toString(), null, true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList()).blockingFirst()
        val firstSum = sums.find { it.aggregateDate == firstTransactionDate }!!
        val secondSum = sums.find { it.aggregateDate == secondTransactionDate }!!
        val thirdSum = sums.find { it.aggregateDate == thirdTransactionDate }!!
        assertEquals(0, BigDecimal(110.00).compareTo(firstSum.sum))
        assertEquals(0, BigDecimal(300.00).compareTo(secondSum.sum))
        assertEquals(0, BigDecimal(1200.00).compareTo(thirdSum.sum))

        val boundSums = transactionDao.getTransactionSumByDate(profile.id!!, true, firstTransactionDate.toString(), fourthTransactionDate.toString(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList()).blockingFirst()
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
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val builder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal.ZERO)
        var ordinal = 1
        transactionDao.insertTransaction(builder.amount(BigDecimal(110.00)).createdAt(firstTransactionDate.atTime(time).toString()).datedAt(firstTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(300.00)).createdAt(secondTransactionDate.atTime(time).toString()).datedAt(secondTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(450.00)).createdAt(thirdTransactionDate.atTime(time).toString()).datedAt(thirdTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(750.00)).createdAt(fourthTransactionDate.atTime(time).toString()).datedAt(fourthTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()
        transactionDao.insertTransaction(builder.amount(BigDecimal(1000.00)).createdAt(fifthTransactionDate.atTime(time).toString()).datedAt(fifthTransactionDate.toString()).ordinal(ordinal++).build()).subscribeOn(Schedulers.io()).blockingSubscribe()

        val firstSum = transactionDao.getTransactionDebitSum(profile.id!!, firstTransactionDate.toString(), null, true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList()).blockingFirst()

        assertEquals(0, BigDecimal(2610.00).compareTo(firstSum))

        val secondSum = transactionDao.getTransactionDebitSum(profile.id!!, firstTransactionDate.toString(), fourthTransactionDate.toString(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList()).blockingFirst()
        assertEquals(0, BigDecimal(1610.00).compareTo(secondSum))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionDaoTest::class.java)
    }
}