package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
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
            .subscribeOn(Schedulers.io())
    }

    fun getTransactionItems(transactionId: Long): Observable<List<TransactionItemDb>> {
        return transactionItemDao.getAllByTransactionId(transactionId)
    }
}