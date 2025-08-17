package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.TransactionDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.math.BigDecimal

class TempTransactionDao {

    private var incrementId = 0L
    private val transactions = arrayListOf<TransactionDb>()
    private val behaviorSubject = BehaviorSubject.create<List<TransactionDb>>()

    //region Create
    fun addTransaction(transaction: TransactionDb): Single<Long> {
        transactions.add(transaction.copy(id = ++incrementId))
        behaviorSubject.onNext(transactions)
        return Single.just(incrementId)
    }
    //endregion

    //region Read
    fun getTransactions(): Observable<List<TransactionDb>> {
        return behaviorSubject
    }
    fun getTotalSpent(from: LocalDateTime? = null, to: LocalDateTime? = null): Observable<BigDecimal> {
        return behaviorSubject.map { list ->
            list.filter {
                val fromBool =
                if(from != null) {
                    from >= LocalDateTime.parse(it.datedAt)
                } else {
                    true
                }
                val toBool =
                    if(to != null) {
                        to <= LocalDateTime.parse(it.datedAt)
                    } else {
                        true
                    }
                fromBool && toBool
            }.map { it.amount }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        }
    }
    fun getTransactionSumByDate(): Observable<List<Pair<LocalDate, BigDecimal>>> {
        return behaviorSubject.map { list ->
            val byCategories = list.groupBy {
                val localDateTime = LocalDateTime.parse(it.datedAt)
                localDateTime.toLocalDate()
            }.toSortedMap()
            val sumByDay = byCategories.entries.associate { (cat, items) ->
                val sum = (items.map { it.amount }. reduce { acc, bigDecimal -> acc.plus(bigDecimal) })
                cat to sum
            }.toList()
            sumByDay
        }
    }
    //endregion

    //region Delete
    fun deleteAll() {
        transactions.clear()
        behaviorSubject.onNext(transactions)
    }
    //endregion
}