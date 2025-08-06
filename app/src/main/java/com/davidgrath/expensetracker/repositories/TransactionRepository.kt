package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.SortedMap
import java.util.TreeMap

class TransactionRepository(private val transactionDao: TempTransactionDao, private val transactionItemDao: TempTransactionItemDao) {

    fun addTransaction(transactionDb: TransactionDb): Single<Long> {
        return transactionDao.addTransaction(transactionDb)
    }
    fun getTransactions(): Observable<SortedMap<TransactionDb, List<TransactionItemDb>>> {
        //TODO Terrible solution
        return transactionDao.getTransactions().map { transactions ->
            val map = TreeMap<TransactionDb, List<TransactionItemDb>>()
            for(t in transactions) {
                val items = transactionItemDao.getTransactionItemsSingle(t.id!!).subscribeOn(Schedulers.io()).blockingGet()
                map.put(t, items)
            }
            map
        }
    }
}