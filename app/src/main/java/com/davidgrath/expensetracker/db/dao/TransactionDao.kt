package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

@Dao
interface TransactionDao {
    //region Create
    @Insert
    fun insertTransaction(transactionDb: TransactionDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT * FROM TransactionDb WHERE id = :id")
    fun getById(id: Long): Observable<TransactionDb>
    @Query("SELECT * FROM TransactionDb WHERE id = :id")
    fun getByIdSingle(id: Long): Single<TransactionDb>

    @Query("SELECT * FROM TransactionDb")
    fun getAllTemp(): Single<List<TransactionDb>>

    @Query("SELECT * FROM TransactionDb WHERE date(datedAt) >= date(:fromDate)")
    fun getAllFromUTC(fromDate: String): Observable<List<TransactionDb>>

    @Query("SELECT * FROM TransactionDb WHERE date(datetime(datedAt || \"T\" || datedAtTime, datedAtOffset)) >= date(:fromDate)")
    fun getAllFromTransactionOffset(fromDate: String): Observable<List<TransactionDb>>

    @Query("SELECT * FROM TransactionDb WHERE date(datetime(datedAt || \"T\" || datedAtTime, :offset)) >= date(datetime(:fromDate || \"T00:00:00\" || :offset))")
    fun getAllFromSpecifiedOffset(fromDate: String, offset: String): Observable<List<TransactionDb>>

    @Query("SELECT * FROM TransactionDb WHERE date(datetime(datedAt || \"T\" || datedAtTime, :offset)) >= date(datetime(:fromDate || \"T00:00:00\" || :offset))")
    fun getAllFromSpecifiedOffsetSingle(fromDate: String, offset: String): Single<List<TransactionDb>>
    @Query("SELECT * FROM TransactionDb WHERE " +
            "date(datetime(datedAt || \"T\" || datedAtTime, :offset)) >= date(datetime(:fromDate || \"T00:00:00\" || :offset)) " +
            "AND " +
            "date(datetime(datedAt || \"T\" || datedAtTime, :offset)) <= date(datetime(:toDate || \"T00:00:00\" || :offset)) ")
    fun getAllFromToSpecifiedOffsetSingle(fromDate: String, toDate: String, offset: String): Single<List<TransactionDb>>

    @Query("SELECT * FROM TransactionDb WHERE date(datetime(datedAt || \"T\" || datedAtTime, datedAtOffset)) >= date(:fromDate)")
    fun getAllFromTransactionOffsetSingle(fromDate: String): Single<List<TransactionDb>>

    @Query("SELECT * FROM TransactionDb WHERE " +
            "date(datetime(datedAt || \"T\" || datedAtTime, datedAtOffset)) >= date(:fromDate) " +
            "AND " +
            "date(datetime(datedAt || \"T\" || datedAtTime, datedAtOffset)) <= date(:toDate) ")
    fun getAllFromToTransactionOffsetSingle(fromDate: String, toDate: String): Single<List<TransactionDb>>

    @Query("SELECT date(datedAt) as aggregateDate, sum(amount) as sum FROM TransactionDb WHERE aggregateDate >= date(:fromDate) GROUP BY aggregateDate ORDER BY aggregateDate")
    fun getTransactionSumByDateFrom(fromDate: String): Observable<List<DateAmountSummary>>

    @Query("SELECT date(datedAt) as aggregateDate, sum(amount) as sum FROM TransactionDb WHERE aggregateDate >= date(:fromDate) AND aggregateDate <= date(:toDate) GROUP BY aggregateDate ORDER BY aggregateDate")
    fun getTransactionSumByDateFromTo(fromDate: String, toDate: String): Observable<List<DateAmountSummary>>

    @Query("SELECT sum(amount) FROM TransactionDb WHERE date(datedAt) >= date(:fromDate)")
    fun getTransactionSumFrom(fromDate: String): Observable<BigDecimal>

    @Query("SELECT sum(amount) FROM TransactionDb WHERE date(datedAt) >= date(:fromDate) AND date(datedAt) <= date(:toDate)")
    fun getTransactionSumFromTo(fromDate: String, toDate: String): Observable<BigDecimal>
    //endregion

    //region Update
    @Update
    fun updateTransaction(transactionDb: TransactionDb): Single<Int>
    //endregion

    //region Delete
    @Query("DELETE FROM TransactionDb WHERE 1")
    fun deleteAll(): Single<Int> //TODO Replace with clearAllTables
    //endregion
}