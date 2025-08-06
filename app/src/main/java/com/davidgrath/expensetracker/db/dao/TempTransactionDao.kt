package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.TransactionDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

class TempTransactionDao {

    private var incrementId = 0L
    private val transactions = arrayListOf<TransactionDb>()
    private val behaviorSubject = BehaviorSubject.create<List<TransactionDb>>()

    fun getTransactions(): Observable<List<TransactionDb>> {
        return behaviorSubject
    }
    fun addTransaction(transaction: TransactionDb): Single<Long> {
        transactions.add(transaction.copy(id = ++incrementId))
        behaviorSubject.onNext(transactions)
        return Single.just(incrementId)
    }
}