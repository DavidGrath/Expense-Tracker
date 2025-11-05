package com.davidgrath.expensetracker.db.dao

import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.TransactionBuilder
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {

    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var accountDao: AccountDao
    @Inject
    lateinit var expenseTrackerDatabase: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    lateinit var app: TestExpenseTracker
    lateinit var dataBuilder: DataBuilder

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<TestExpenseTracker>()
        (app.appComponent as TestComponent).inject(this)
        dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
    }

    @Test
    fun getAccountWithStatsTest() {
        val totalExpense = BigDecimal(100)
        val totalIncome = BigDecimal(200)
        val totalTransactions = 2
        val totalItems = 3

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .commit()

        dataBuilder.createTransaction()
            .debitOrCredit(false)
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withItem("Description 2", "miscellaneous", BigDecimal(100))
            .commit()


        val accountStats = accountDao.getAccountSummarySingle(accountId).subscribeOn(Schedulers.io()).blockingGet()
        assertEqualsBD(totalExpense, accountStats.expenses)
        assertEqualsBD(totalIncome, accountStats.income)
        assertEquals(totalTransactions, accountStats.transactionCount)
        assertEquals(totalItems, accountStats.itemCount)

        val newAccountId = accountRepository.createAccount(profile.id!!, "British", "GBP").blockingGet()


        val newAccountStats = accountDao.getAccountSummarySingle(newAccountId).subscribeOn(Schedulers.io()).blockingGet()
        assertEqualsBD(BigDecimal.ZERO, newAccountStats.expenses)
        assertEqualsBD(BigDecimal.ZERO, newAccountStats.income)
        assertEquals(0, newAccountStats.transactionCount)
        assertEquals(0, newAccountStats.itemCount)
    }

    @Test
    fun statsByProfileTest() {
        val totalExpense = BigDecimal(100)
        val totalIncome = BigDecimal(200)
        val totalTransactions = 2
        val totalItems = 3


        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .commit()

        dataBuilder.createTransaction()
            .debitOrCredit(false)
            .withDefaultItemPrice(BigDecimal(100))
            .withItem("Description").withItem("Description 2")
            .commit()

        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val newAccountId = accountRepository.createAccount(profile.id!!, "British", "GBP").blockingGet()

        val accountStatsList = accountDao.getAllByProfileIdWithStatsSingle(profile.id!!).subscribeOn(Schedulers.io()).blockingGet()


        assertEqualsBD(totalExpense, accountStatsList[0].expenses)
        assertEqualsBD(totalIncome, accountStatsList[0].income)
        assertEquals(totalTransactions, accountStatsList[0].transactionCount)
        assertEquals(totalItems, accountStatsList[0].itemCount)

        LOGGER.debug("Account stats: {}", accountStatsList)
        assertEqualsBD(BigDecimal.ZERO, accountStatsList[1].expenses)
        assertEqualsBD(BigDecimal.ZERO, accountStatsList[1].income)
        assertEquals(0, accountStatsList[1].transactionCount)
        assertEquals(0, accountStatsList[1].itemCount)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountDaoTest::class.java)
    }
}