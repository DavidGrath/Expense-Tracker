package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
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

    //TODO Inject properly
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var expenseTrackerDatabase: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: TestExpenseTracker

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    fun givenTransactionRangeDoesNotHaveDatesInIntervalWhenFetchTotalThenDatesIntervalHaveZeroes() {

        val dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val profileId = profile.id!!
        val time = LocalTime.of(8, 0, 0)
        val firstTransactionDate = LocalDate.parse("2025-01-02")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-07")
        val fourthTransactionDate = LocalDate.parse("2025-01-09")
//        val fifthTransactionDate = LocalDate.parse("2025-01-11")
        val fifthTransactionDate = LocalDate.parse("2025-07-11")

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        //Empty Set
        var transactionSumByDates = transactionRepository.getTotalAmountByDate(profileId, true, "2025-07-01", null, emptyList(), emptyList(), emptyList()).blockingFirst()
        assertEquals(0, transactionSumByDates.size)

        //Today and empty
        transactionSumByDates = transactionRepository.getTotalAmountByDate(profileId, true, "2025-06-30", null, emptyList(), emptyList(), emptyList()).blockingFirst()
        assertEquals(1, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, transactionSumByDates.first().sum)

        //Multiple days and empty
        transactionSumByDates = transactionRepository.getTotalAmountByDate(profileId, true, "2025-06-01", null, emptyList(), emptyList(), emptyList()).blockingFirst()
        val daysInJune = 30
        var grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        assertEquals(daysInJune, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, grandSum)

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(secondTransactionDate)
            .commit()

        //Just 1 transaction
        transactionSumByDates = transactionRepository.getTotalAmountByDate(profileId, true, "2025-01-02", null, emptyList(), emptyList(), emptyList()).blockingFirst()
        val daysBetweenJan2AndJun30Inclusive = 180
        assertEquals(daysBetweenJan2AndJun30Inclusive, transactionSumByDates.size)

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(450.00))
            .atDate(thirdTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(750.00))
            .atDate(fourthTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(1000.00))
            .atDate(fifthTransactionDate)
            .commit()


        transactionSumByDates = transactionRepository.getTotalAmountByDate(profileId, true, "2025-01-02", null, emptyList(), emptyList(), emptyList()).blockingFirst()
        val sum1 = transactionSumByDates.find { it.aggregateDate == firstTransactionDate }!!
        var missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!
        val missingSum2 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-08") }!!
        val missingSum3 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-10") }!!
        assertEqualsBD(BigDecimal(410), sum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum2.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum3.sum)

        transactionSumByDates = transactionRepository.getTotalAmountByDate(profileId, true, firstTransactionDate.toString(), thirdTransactionDate.toString(), emptyList(), emptyList(), emptyList()).blockingFirst()
        val daysBetweenFirstAndThirdTransactionInclusive = 6

        grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!

        assertEquals(daysBetweenFirstAndThirdTransactionInclusive, transactionSumByDates.size)
        assertEqualsBD(BigDecimal(860), grandSum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
    }

    @Test
    fun multipleDistinctCurrencyStatisticsTest() {
        //Insert new accounts - 1 USD, 1 GBP
        //1 transaction per account
        //When try to fetch any stats then pick anyone and only filter by that
    }

}