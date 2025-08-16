package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.math.BigDecimal

class TempTransactionItemDao {
    private var incrementId = 0L
    private val transactionItems = arrayListOf<TransactionItemDb>()
    private val behaviorSubject = BehaviorSubject.create<List<TransactionItemDb>>()

    //region Create
    fun addTransactionItem(transactionItem: TransactionItemDb): Single<Long> {
        transactionItems.add(transactionItem.copy(id = ++incrementId))
        behaviorSubject.onNext(transactionItems)
        return Single.just(incrementId)
    }

    fun addTransactionItems(transactionItems: List<TransactionItemDb>): Single<List<Long>> {
        val ids = arrayListOf<Long>()
        for(transactionItem in transactionItems) {
            val id = ++incrementId
            this.transactionItems.add(transactionItem.copy(id = id))
            ids.add(id)
        }
        behaviorSubject.onNext(transactionItems)
        return Single.just(ids)
    }
    //endregion

    //region Read
    fun getTransactionItems(transactionId: Long): Observable<List<TransactionItemDb>> {
        return behaviorSubject.map { list -> list.filter {  t -> t.transactionId == transactionId } }
    }


    fun getTransactionItems(): Observable<List<TransactionItemDb>> {
        return behaviorSubject
    }

    fun getTransactionItemsSingle(transactionId: Long): Single<List<TransactionItemDb>> {
        return Single.just(transactionItems.filter {  t -> t.transactionId == transactionId } )
    }
    fun getTransactionItemsSumByCategory(transactionIds: List<Long>): Observable<List<Pair<Long, BigDecimal>>> {
        return behaviorSubject.map { list ->
            val byTransactions = list.filter { item -> item.transactionId in transactionIds }
            val byCategories = byTransactions.groupBy {
                it.primaryCategoryId
            }
            val sumByCategories = byCategories.entries.associate { (cat, items) ->
                val sum = (items.map { it.amount }. reduce { acc, bigDecimal -> acc.plus(bigDecimal) })
                cat to sum
            }.toList()
            sumByCategories
        }
    }
    //endregion

    //region Update
    //endregion

    //region Delete
    fun deleteAll() {
        transactionItems.clear()
        behaviorSubject.onNext(transactionItems)
    }
    //endregion
}