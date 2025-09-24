package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionItemRepository
 @Inject constructor(
     private val transactionItemDao: TransactionItemDao,
     private val clock: Clock
 ){

    fun getTotalSpentByCategory(): Observable<List<ItemSumByCategory>> {
        return transactionItemDao.getSumByCategoryFrom(LocalDate.now(clock).toString())
            .map {
                LOGGER.info("getTotalSpentByCategory: Item count: {} items", it.size)
                it
            }
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionItems(transactionId: Long): Observable<List<TransactionItemDb>> {
        return transactionItemDao.getAllByTransactionId(transactionId)
    }

    fun getTransactionItemsSingle(transactionId: Long): Single<List<TransactionItemDb>> {
        return transactionItemDao.getAllByTransactionIdSingle(transactionId)
            .doOnSuccess {
                LOGGER.info("getTransactionItemsSingle: ID: {}, {} items", transactionId, it.size)
            }
    }

    fun addTransactionItem(item: TransactionItemDb): Single<Long> {
        return transactionItemDao.insertTransactionItem(item)
            .subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionItemRepository::class.java)
    }
}