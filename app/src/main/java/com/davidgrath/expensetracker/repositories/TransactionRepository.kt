package com.davidgrath.expensetracker.repositories

import android.util.Log
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
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
    private val clock: Clock
) {

    fun addTransaction(amount: BigDecimal, description: String, categoryId: Long): Single<Long> {
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_TIME)

        val offset = date.offset.id
        val zone = date.zone.id
        val transaction = TransactionDb(
            null,
            0,
            amount,
            "USD",
            null,
            false,
            true,
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

    fun addTransaction(
        transactionDb: TransactionDb,
        items: List<TransactionItemDb>
    ): Single<List<Long>> {
        return Single.just(emptyList())
        /*return tempTransactionDao.addTransaction(transactionDb).flatMap { id ->
            val idItems = items.map { it.copy(transactionId = id) }
            tempTransactionItemDao.addTransactionItems(idItems)

        }*/
    }

    fun addTransaction(transactionDb: TransactionDb): Single<Long> {
        return transactionDao.insertTransaction(transactionDb)
            .subscribeOn(Schedulers.io())
    }

    fun addTransactionItem(item: TransactionItemDb): Single<Long> {
        return transactionItemDao.insertTransactionItem(item)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactions(): Flowable<List<TransactionWithItemAndCategory>> {
        return transactionItemDao.getItemsWithTransactionsAndCategoryFrom(LocalDate.now(clock).toString())
            .subscribeOn(Schedulers.io())
            .timeInterval()
            .map {
                Log.i("TransactionRepository", "getItemsWithTransactionsAndCategoryFrom time: ${it.time(TimeUnit.MILLISECONDS)} ms")
                Log.i("TransactionRepository", "getItemsWithTransactionsAndCategoryFrom size: ${it.value().size}")
                it.value()
            }
    }

    fun getTransactionById(id: Long): Observable<TransactionDb> {
        return transactionDao.getById(id)
    }

    fun getTotalSpent(): Observable<BigDecimal> {
        return transactionDao.getTransactionSumFrom(LocalDate.now(clock).toString())
            .subscribeOn(Schedulers.io())
    }

    fun getTotalSpentByDate(fromDate: String): Observable<List<DateAmountSummary>> {
        val originalSummary = transactionDao.getTransactionSumByDateFrom(fromDate)
        val filledSummary = originalSummary.map { list ->
            val start = LocalDate.parse(fromDate)
            val now = LocalDate.now(clock)
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
                        }
                    } else {
                        newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                    }
                } else {
                    newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                }
                currentDate = currentDate.plusDays(1)
            }
            newList.toList()
        }
        return filledSummary
    }

    fun getTotalSpentByDateFromTo(
        fromDate: String,
        toDate: String
    ): Observable<List<DateAmountSummary>> {
        //TODO Fill in missing/zero days for even spread
        return transactionDao.getTransactionSumByDateFromTo(fromDate, toDate)
    }
}