package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import javax.inject.Inject
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
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
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
    private val accountRepository: AccountRepository
) {

    fun addTransaction(accountId: Long, amount: BigDecimal, description: String, categoryId: Long): Single<Long> {
        val date = ZonedDateTime.now(timeAndLocaleHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeString = date.format(DateTimeFormatter.ISO_LOCAL_TIME)

        val offset = date.offset.id
        val zone = date.zone.id
        val account = accountRepository.getAccountByIdSingle(accountId).blockingGet()!!
        val maxOrdinal = getMaxOrdinalInDayForAccount(accountId, dateString).blockingGet()?: 0
        val ordinal = maxOrdinal + 1
        val transaction = TransactionDb(
            null,
            accountId,
            amount,
            account.currencyCode,
            null,
            false,
            TransactionMode.Other,
            null,
            null,
            null,
            dateTimeString,
            offset,
            zone,
            ordinal,
            dateString,
            timeString
        )
        return transactionDao.insertTransaction(transaction)
            .subscribeOn(Schedulers.io())
            .flatMap { id ->
            val item = TransactionItemDb(null, id, amount, null, 1, description, "", null, categoryId, false, 1, dateTimeString, offset, zone)
            transactionItemDao.insertTransactionItem(item).map { id }
        }.subscribeOn(Schedulers.io())
    }

    fun addTransaction(transactionDb: TransactionDb): Single<Long> {
        return transactionDao.insertTransaction(transactionDb)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactions(profileId: Long, accountId: Long, startDate: String?, endDate: String?): Observable<List<TransactionWithItemAndCategory>> {
        return transactionItemDao.getItemsWithTransactionsAndCategory(profileId,
            startDate, endDate, false, listOf(accountId), true, emptyList(), true, emptyList()
        )
            .subscribeOn(Schedulers.io())
            .timeInterval()
            .map {
                LOGGER.info("getItemsWithTransactionsAndCategoryFrom time: {} ms", it.time(TimeUnit.MILLISECONDS))
                LOGGER.info("getItemsWithTransactionsAndCategoryFrom size: {}", it.value().size)
                it.value()
            }
    }

    fun getTransactionsFiltered(profileId: Long, startDate: String?, endDate: String?, accountIds: List<Long>, dates: List<String>, categories: List<Long>): Single<List<TransactionWithItemAndCategory>> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionItemDao.getItemsWithTransactionsAndCategorySingle(profileId,
            startDate, endDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories
        )
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

    fun getTotalExpense(profileId: Long, fromDate: String?, toDate: String?, accountIds: List<Long>, dates: List<String>, categories: List<Long>): Observable<BigDecimal> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionDao.getTransactionDebitSum(profileId, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
            .subscribeOn(Schedulers.io())
    }
    fun getTotalIncome(profileId: Long, fromDate: String?, toDate: String?, accountIds: List<Long>, dates: List<String>, categories: List<Long>): Observable<BigDecimal> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionDao.getTransactionCreditSum(profileId, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
            .subscribeOn(Schedulers.io())
    }

    fun getTotalAmountByDate(profileId: Long, debitOrCredit: Boolean, fromDate: String?, toDate: String? = null, accountIds: List<Long>, dates: List<String>, categories: List<Long>): Observable<List<DateAmountSummary>> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        val originalSummary = transactionDao.getTransactionSumByDate(profileId, debitOrCredit, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
        val filledSummary = originalSummary.map { list ->
            var zeroCount = 0
            val start = if(fromDate == null) {
                LocalDate.now(timeAndLocaleHandler.getClock())
            } else {
                LocalDate.parse(fromDate)
            }
            val end = if(toDate == null) {
                LocalDate.now(timeAndLocaleHandler.getClock())
            } else {
                LocalDate.parse(toDate)
            }
            var currentDate = start

            val newList = arrayListOf<DateAmountSummary>()

            val listSize = list.size
            var existingDateIndex = if (listSize > 0) 0 else -1
            while(currentDate <= end) {
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

    fun updateTransaction(transactionDb: TransactionDb): Single<Int> {
        return transactionDao.updateTransaction(transactionDb)
            .subscribeOn(Schedulers.io())
    }

    fun getEarliestTransactionDate(profileId: Long, accountIds: List<Long>): Maybe<LocalDate> {
        val emptyAccounts = accountIds.isEmpty()
        return transactionDao.getEarliestTransactionDate(profileId, emptyAccounts, accountIds)
            .subscribeOn(Schedulers.io())
    }

    fun getMaxOrdinalInDayForAccount(accountId: Long, date: String): Maybe<Int> {
        return transactionDao.getMaxOrdinalInDayForAccount(accountId, date)
            .subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepository::class.java)
    }
}