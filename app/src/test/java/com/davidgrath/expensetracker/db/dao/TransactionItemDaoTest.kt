package com.davidgrath.expensetracker.db.dao

import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

    @Before
    fun setUp() {
        (ApplicationProvider.getApplicationContext<TestExpenseTracker>().appComponent as TestComponent).inject(this)
    }

    @Test
    @Ignore("To test in next commit")
    fun sumByCategoryTest() {
        val fitness = categoryDao.findByStringId("fitness").blockingGet()!!
        val food = categoryDao.findByStringId("food").blockingGet()!!

        val fromDate = LocalDate.parse("2025-01-01")
        val toDate = LocalDate.parse("2025-01-02")
        val transactionDb = TestBuilder.defaultTransactionBuilder(BigDecimal(4_000.00)).datedAt(fromDate.toString()).build()
        val id = transactionDao.insertTransaction(transactionDb).blockingGet()
        val bread = TransactionItemDb(null, id, BigDecimal(1_500), null, 1, "Bread", "", null, food.id!!, transactionDb.createdAt, transactionDb.createdAtOffset, transactionDb.createdAtTimezone)
        val dumbbells = TransactionItemDb(null, id, BigDecimal(2_500), null, 1, "Dumbbells", "", null, fitness.id!!, transactionDb.createdAt, transactionDb.createdAtOffset, transactionDb.createdAtTimezone)

        val transactionDb2 = TestBuilder.defaultTransactionBuilder(BigDecimal(6_000.00)).datedAt(toDate.toString()).build()
        val id2 = transactionDao.insertTransaction(transactionDb2).blockingGet()
        val water = TransactionItemDb(null, id2, BigDecimal(1_000), null, 1, "Water", "", null, food.id!!, transactionDb2.createdAt, transactionDb2.createdAtOffset, transactionDb2.createdAtTimezone)
        val sweatpants = TransactionItemDb(null, id2, BigDecimal(5_000), null, 1, "Sweatpants", "", null, fitness.id!!, transactionDb2.createdAt, transactionDb2.createdAtOffset, transactionDb2.createdAtTimezone)

        val transactionDb3 = TestBuilder.defaultTransactionBuilder(BigDecimal(8_000.00)).datedAt(toDate.plusDays(1).toString()).build()
        val id3 = transactionDao.insertTransaction(transactionDb3).blockingGet()
        val fish = TransactionItemDb(null, id3, BigDecimal(3_000), null, 1, "Fish", "", null, food.id!!, transactionDb3.createdAt, transactionDb3.createdAtOffset, transactionDb3.createdAtTimezone)
        val jumpRope = TransactionItemDb(null, id3, BigDecimal(5_000), null, 1, "Jump Rope", "", null, fitness.id!!, transactionDb3.createdAt, transactionDb3.createdAtOffset, transactionDb3.createdAtTimezone)

        transactionItemDao.insertTransactionItemMultiple(listOf(bread, dumbbells, water, sweatpants, fish, jumpRope))
        val sumList = transactionItemDao.getSumByCategoryFrom(fromDate.toString()).blockingFirst()
        var foodSum = sumList.find { it.categoryId == food.id }!!.sum
        var fitnessSum = sumList.find { it.categoryId == fitness.id }!!.sum

        assertEquals(BigDecimal(5_500), foodSum)
        assertEquals(BigDecimal(12_500), fitnessSum)

        val sumListTo = transactionItemDao.getSumByCategoryFromTo(fromDate.toString(), toDate.toString()).blockingFirst()
        foodSum = sumListTo.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumListTo.find { it.categoryId == fitness.id }!!.sum

        assertEquals(BigDecimal(2_500), foodSum)
        assertEquals(BigDecimal(7_500), fitnessSum)
    }
}