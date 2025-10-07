package com.davidgrath.expensetracker.repositories

import android.util.Log
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import javax.inject.Inject
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class TransactionRepository
@Inject
constructor(
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val transactionItemImagesDao: TransactionItemImagesDao,
    private val categoryDao: CategoryDao,
    private val timeHandler: TimeHandler,
    private val accountRepository: AccountRepository
) {

    fun addTransaction(accountId: Long, amount: BigDecimal, description: String, categoryId: Long): Single<Long> {
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_TIME)

        val offset = date.offset.id
        val zone = date.zone.id
        val account = accountRepository.getAccountByIdSingle(accountId).blockingGet()!!
        val transaction = TransactionDb(
            null,
            accountId,
            amount,
            account.currencyCode,
            null,
            false,
            null,
            null,
            null,
            dateTimeString,
            offset,
            zone,
            0,
            dateString,
            timeString,
            offset,
            zone
        )
        return transactionDao.insertTransaction(transaction)
            .subscribeOn(Schedulers.io())
            .flatMap { id ->
            val item = TransactionItemDb(null, id, amount, null, 1, description, "", null, categoryId, dateTimeString, offset, zone)
            transactionItemDao.insertTransactionItem(item).map { id }
        }.subscribeOn(Schedulers.io())
    }

    fun addTransaction(transactionDb: TransactionDb): Single<Long> {
        return transactionDao.insertTransaction(transactionDb)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactions(): Flowable<List<TransactionWithItemAndCategory>> {
        return transactionItemDao.getItemsWithTransactionsAndCategoryFrom(LocalDate.now(timeHandler.getClock()).toString())
            .subscribeOn(Schedulers.io())
            .timeInterval()
            .map {
                LOGGER.info("getItemsWithTransactionsAndCategoryFrom time: {} ms", it.time(TimeUnit.MILLISECONDS))
                LOGGER.info("getItemsWithTransactionsAndCategoryFrom size: {}", it.value().size)
                it.value()
            }
    }

    fun getTransactionById(id: Long): Observable<TransactionDb> {
        return transactionDao.getById(id)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionByIdSingle(id: Long): Single<TransactionDb> {
        return transactionDao.getByIdSingle(id)
            .subscribeOn(Schedulers.io())
    }

    fun getTotalSpent(): Observable<BigDecimal> {
        return transactionDao.getTransactionSumFrom(LocalDate.now(timeHandler.getClock()).toString())
            .subscribeOn(Schedulers.io())
    }

    fun getTotalSpentByDate(fromDate: String): Observable<List<DateAmountSummary>> {
        val originalSummary = transactionDao.getTransactionSumByDateFrom(fromDate)
        val filledSummary = originalSummary.map { list ->
            var zeroCount = 0
            val start = LocalDate.parse(fromDate)
            val now = LocalDate.now(timeHandler.getClock())
            var currentDate = start

            val newList = arrayListOf<DateAmountSummary>()

            val listSize = list.size
            var existingDateIndex = if (listSize > 0) 0 else -1
            while(currentDate <= now) {
                if(existingDateIndex != -1) {
                    if(existingDateIndex < listSize) {
                        val summary = list[existingDateIndex]
                        if (summary.aggregateDate == currentDate) {
                            newList.add(summary)
                            existingDateIndex += 1
                        } else {
                            newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                            zeroCount++
                        }
                    } else {
                        newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                        zeroCount++
                    }
                } else {
                    newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                    zeroCount++
                }
                currentDate = currentDate.plusDays(1)
            }
            if(zeroCount > 0) {
                LOGGER.info("getTotalSpentByDate: Filled {} empty dates with zeroes", zeroCount)
            }
            LOGGER.info("getTotalSpentByDate: Item Count: {}", newList.size)
            newList.toList()
        }
        return filledSummary.subscribeOn(Schedulers.io())
    }

    fun getTotalSpentByDateFromTo(
        fromDate: String,
        toDate: String
    ): Observable<List<DateAmountSummary>> {
        //TODO Fill in missing/zero days for even spread
        return transactionDao.getTransactionSumByDateFromTo(fromDate, toDate).subscribeOn(Schedulers.io())
    }

    fun updateTransaction(transactionDb: TransactionDb): Single<Int> {
        return transactionDao.updateTransaction(transactionDb)
            .subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepository::class.java)
    }
}