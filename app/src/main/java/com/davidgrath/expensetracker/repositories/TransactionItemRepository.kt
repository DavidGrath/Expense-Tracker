package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.ui.main.MainViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionItemRepository
 @Inject constructor(
     private val transactionItemDao: TransactionItemDao,
     private val timeAndLocaleHandler: TimeAndLocaleHandler
 ){

    fun getTotalExpenseByCategory(profileId: Long,
        fromDate: String? = null, toDate: String? = null, accountIds: List<Long>, dates: List<String>, categories: List<Long>
    ): Observable<List<ItemSumByCategory>> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionItemDao.getDebitSumByCategory(profileId, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
            .map {
                LOGGER.info("getTotalExpenseByCategory: Item count: {} items", it.size)
                it
            }
            .subscribeOn(Schedulers.io())
    }

    fun getTotalIncomeByCategory(profileId: Long,
        fromDate: String? = null, toDate: String? = null, accountIds: List<Long>, dates: List<String>, categories: List<Long>
    ): Observable<List<ItemSumByCategory>> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionItemDao.getCreditSumByCategory(profileId, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
            .map {
                LOGGER.info("getTotalIncomeByCategory: Item count: {} items", it.size)
                it
            }
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionItems(transactionId: Long): Observable<List<TransactionItemDb>> {
        return transactionItemDao.getAllByTransactionId(transactionId)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionItemsSingle(transactionId: Long): Single<List<TransactionItemDb>> {
        return transactionItemDao.getAllByTransactionIdSingle(transactionId).timeInterval()
            .map {
                LOGGER.info("getTransactionItemsSingle time: ID: {}; time: {} ms; size: {}", transactionId, it.time(TimeUnit.MILLISECONDS), it.value().size) //TODO UOM/UCUM Maybe?
                it.value()
            }.subscribeOn(Schedulers.io())
    }

    fun addTransactionItem(item: TransactionItemDb): Single<Long> {
        return transactionItemDao.insertTransactionItem(item)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionItemCount(profileId: Long,
        fromDate: String? = null, toDate: String? = null, accountIds: List<Long>, dates: List<String>, categories: List<Long>): Observable<TransactionAndItemCount> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionItemDao.getTransactionItemCount(profileId, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionItemCountSingle(profileId: Long,
                                fromDate: String? = null, toDate: String? = null, accountIds: List<Long>, dates: List<String>, categories: List<Long>): Single<TransactionAndItemCount> {
        val emptyAccounts = accountIds.isEmpty()
        val datesEmpty = dates.isEmpty()
        val categoriesEmpty = categories.isEmpty()
        return transactionItemDao.getTransactionItemCountSingle(profileId, fromDate, toDate, emptyAccounts, accountIds, datesEmpty, dates, categoriesEmpty, categories)
            .subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionItemRepository::class.java)
    }
}