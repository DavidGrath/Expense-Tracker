package com.davidgrath.expensetracker.db.dao

import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class TransactionItemDaoTest {

    @Inject
    lateinit var transactionDao: TransactionDao
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var categoryDao: CategoryDao
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
        app = ApplicationProvider.getApplicationContext<TestExpenseTracker>()
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    fun sumByCategoryTest() {

        val dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val fitness = categoryDao.findByProfileIdAndStringId(profile.id!!, "fitness").subscribeOn(Schedulers.io()).blockingGet()!!
        val food = categoryDao.findByProfileIdAndStringId(profile.id!!, "food").subscribeOn(Schedulers.io()).blockingGet()!!

        val fromDate = LocalDate.parse("2025-01-01")
        val id = dataBuilder.createTransaction()
            .atDate(fromDate)
            .withItem("Bread", "food", BigDecimal(1_500))
            .withItem("Dumbbells", "fitness", BigDecimal(2_500))
            .commit().first()
        val toDate = LocalDate.parse("2025-01-02")
        val accountId = getDefaultAccountId(profileRepository, accountRepository)

        val id2 = dataBuilder.createTransaction()
            .atDate(toDate)
            .withItem("Water", "food", BigDecimal(1_000))
            .withItem("Sweatpants", "fitness", BigDecimal(5_000))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(toDate.plusDays(1))
            .withItem("Fish", "food", BigDecimal(3_000))
            .withItem("Jump Rope", "fitness", BigDecimal(5_000))
            .commit()


        val sumList = transactionItemDao.getDebitSumByCategory(profile.id!!, fromDate.toString(), null, true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList()).subscribeOn(Schedulers.io()).blockingFirst()
        var foodSum = sumList.find { it.categoryId == food.id }!!.sum
        var fitnessSum = sumList.find { it.categoryId == fitness.id }!!.sum
        LOGGER.debug("sumList: {}", sumList)
        assertEqualsBD(BigDecimal(5_500), foodSum)
        assertEqualsBD(BigDecimal(12_500), fitnessSum)

        val sumListTo = transactionItemDao.getDebitSumByCategory(profile.id!!, fromDate.toString(), toDate.toString(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList(), true, emptyList()).subscribeOn(Schedulers.io()).blockingFirst()
        foodSum = sumListTo.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumListTo.find { it.categoryId == fitness.id }!!.sum

        assertEqualsBD(BigDecimal(2_500), foodSum)
        assertEqualsBD(BigDecimal(7_500), fitnessSum)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionItemDaoTest::class.java)
    }

}