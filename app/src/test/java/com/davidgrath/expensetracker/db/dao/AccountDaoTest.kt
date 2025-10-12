package com.davidgrath.expensetracker.db.dao

import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.di.TestComponent
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
    lateinit var app: TestExpenseTracker

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<TestExpenseTracker>()
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    fun getAccountWithStatsTest() {
        val totalExpense = BigDecimal(100)
        val totalIncome = BigDecimal(200)
        val totalTransactions = 2
        val totalItems = 3

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val category = categoryRepository.findByStringId("miscellaneous").subscribeOn(Schedulers.io()).blockingGet()!!
        val transactionBuilder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100))
        val id = transactionDao.insertTransaction(transactionBuilder.build()).subscribeOn(Schedulers.io()).blockingGet()
        val itemBuilder = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal(100), category.id!!)
        val itemId = transactionItemDao.insertTransactionItem(itemBuilder.build()).subscribeOn(Schedulers.io()).blockingGet()

        val secondTransaction = transactionBuilder.amount(BigDecimal(200)).debitOrCredit(false).build()
        val id2 = transactionDao.insertTransaction(secondTransaction).subscribeOn(Schedulers.io()).blockingGet()
        transactionItemDao.insertTransactionItemMultiple(listOf(
            itemBuilder.transactionId(id2).build(),
            itemBuilder.transactionId(id2).build()
        )
        ).subscribeOn(Schedulers.io()).blockingSubscribe()

        val accountStats = accountDao.getAccountSummarySingle(accountId).subscribeOn(Schedulers.io()).blockingGet()
        assertEqualsBD(totalExpense, accountStats.expenses)
        assertEqualsBD(totalIncome, accountStats.income)
        assertEquals(totalTransactions, accountStats.transactionCount)
        assertEquals(totalItems, accountStats.itemCount)

        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
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

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val category = categoryRepository.findByStringId("miscellaneous").subscribeOn(Schedulers.io()).blockingGet()!!
        val transactionBuilder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100))
        val id = transactionDao.insertTransaction(transactionBuilder.build()).subscribeOn(Schedulers.io()).blockingGet()
        val itemBuilder = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal(100), category.id!!)
        val itemId = transactionItemDao.insertTransactionItem(itemBuilder.build()).subscribeOn(Schedulers.io()).blockingGet()

        val secondTransaction = transactionBuilder.amount(BigDecimal(200)).debitOrCredit(false).build()
        val id2 = transactionDao.insertTransaction(secondTransaction).subscribeOn(Schedulers.io()).blockingGet()
        transactionItemDao.insertTransactionItemMultiple(listOf(
            itemBuilder.transactionId(id2).build(),
            itemBuilder.transactionId(id2).build()
        )
        ).subscribeOn(Schedulers.io()).blockingSubscribe()

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
        val LOGGER = LoggerFactory.getLogger(AccountDaoTest::class.java)
    }
}