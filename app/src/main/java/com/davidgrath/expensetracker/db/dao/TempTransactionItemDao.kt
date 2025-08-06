package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

class TempTransactionItemDao {
    private var incrementId = 0L
    private val transactionItems = arrayListOf<TransactionItemDb>()
    private val behaviorSubject = BehaviorSubject.create<List<TransactionItemDb>>()

    fun getTransactionItems(transactionId: Long): Observable<List<TransactionItemDb>> {
        return behaviorSubject.map { list -> list.filter {  t -> t.transactionId == transactionId } }
    }

    fun getTransactionItemsSingle(transactionId: Long): Single<List<TransactionItemDb>> {
        return Single.just(transactionItems.filter {  t -> t.transactionId == transactionId } )
    }

    fun addTransactionItem(transactionItem: TransactionItemDb): Single<Unit> {
        transactionItems.add(transactionItem.copy(id = ++incrementId))
        behaviorSubject.onNext(transactionItems)
        return Single.just(Unit)
    }
}