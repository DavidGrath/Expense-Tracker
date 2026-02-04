package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.entities.db.views.TransactionIdAndOrdinal
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.threeten.bp.LocalDate
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


    // :-( https://stackoverflow.com/questions/2421189/version-of-sqlite-used-in-android
    // Window functions are in API 30
    @Query("SELECT date(datedAt) as aggregateDate, sum(amount) as sum FROM TransactionDb t " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR aggregateDate >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR aggregateDate <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds))" +
            "AND (:datesEmpty OR date(t.datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR t.id in (select t2.id FROM TransactionDb t2 INNER JOIN TransactionItemDb ti2 ON ti2.transactionId=t2.id where (ti2.primaryCategoryId in (:categories)))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "AND debitOrCredit = :debitOrCredit " +
            "GROUP BY aggregateDate ORDER BY aggregateDate")
    fun getTransactionSumByDate(
        profileId: Long, debitOrCredit: Boolean,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<List<DateAmountSummary>>

    @Query("SELECT sum(t.amount) FROM TransactionDb t " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(t.datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(t.datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds))" +
            "AND (:datesEmpty OR date(t.datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR t.id in (select t2.id FROM TransactionDb t2 INNER JOIN TransactionItemDb ti2 ON ti2.transactionId=t2.id where (ti2.primaryCategoryId in (:categories)))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "AND debitOrCredit")
    fun getTransactionDebitSum(
        profileId: Long,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<BigDecimal>

    @Query("SELECT sum(t.amount) FROM TransactionDb t " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(t.datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(t.datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds))" +
            "AND (:datesEmpty OR date(t.datedAt) in (:dates))" +
            "AND (:categoriesEmpty OR t.id in (select t2.id FROM TransactionDb t2 INNER JOIN TransactionItemDb ti2 ON ti2.transactionId=t2.id where (ti2.primaryCategoryId in (:categories)))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "AND not(debitOrCredit)")
    fun getTransactionCreditSum(
        profileId: Long,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<BigDecimal>

    @Query("SELECT min(date(datedAt)) FROM TransactionDb t " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:emptyAccounts OR accountId in (:accountIds))"
    )
    fun getEarliestTransactionDate(
        profileId: Long,
        emptyAccounts: Boolean, accountIds: List<Long>): Maybe<LocalDate>

    @Query("SELECT max(ordinal) FROM TransactionDb " +
            "WHERE accountId = :accountId " +
            "AND date(datedAt) = date(:date)")
    fun getMaxOrdinalInDayForAccount(accountId: Long, date: String): Maybe<Int>

    @Query("SELECT t.*  FROM TransactionDb t " +
            "WHERE t.id = " +
            "(SELECT id FROM (SELECT tt.id, max(tt.ordinal)  FROM TransactionDb tt WHERE tt.accountId = :accountId AND date(tt.datedAt) = date(:date) AND time(:time) > time(tt.datedAtTime)))"
    )
    fun getHighestTimeBeforeTimeInDayForAccount(accountId: Long, date: String, time: String): Maybe<TransactionDb>
    @Query("SELECT t.*  FROM TransactionDb t " +
            "WHERE t.id = " +
            "(SELECT id FROM (SELECT tt.id, max(tt.ordinal)  FROM TransactionDb tt WHERE tt.accountId = :accountId AND date(tt.datedAt) = date(:date) AND time(:time) > time(tt.datedAtTime) AND tt.id != :transactionId))"
    )
    fun getHighestTimeBeforeTimeInDayForAccountExcludingTransaction(transactionId: Long, accountId: Long, date: String, time: String): Maybe<TransactionDb>
    @Query("SELECT t.*  FROM TransactionDb t " +
            "WHERE t.id = " +
            "(SELECT id FROM (SELECT tt.id, min(tt.ordinal)  FROM TransactionDb tt WHERE tt.accountId = :accountId AND date(tt.datedAt) = date(:date) AND time(:time) <= time(tt.datedAtTime)  AND tt.id != :transactionId))"
    )
    fun getLowestTimeAfterTimeInDayForAccountExcludingTransaction(transactionId: Long, accountId: Long, date: String, time: String): Maybe<TransactionDb>
    @Query("SELECT t.*  FROM TransactionDb t " +
            "WHERE t.id = " +
            "(SELECT id FROM (SELECT tt.id, min(tt.ordinal)  FROM TransactionDb tt WHERE tt.accountId = :accountId AND date(tt.datedAt) = date(:date) AND time(:time) <= time(tt.datedAtTime)))"
    )
    fun getLowestTimeAfterTimeInDayForAccount(accountId: Long, date: String, time: String): Maybe<TransactionDb>

    @Query("SELECT t.id AS transactionId, t.ordinal AS transactionOrdinal " +
            "FROM TransactionDb t " +
            "WHERE t.accountId = :accountId " +
            "AND t.datedAt = :date " +
            "AND t.ordinal >= :startingOrdinal ")
    fun getIdsAndOrdinalsFromDateForAccountSingle(accountId: Long, date: String, startingOrdinal: Int): Single<List<TransactionIdAndOrdinal>>

    //endregion

    //region Update
    @Update
    fun updateTransaction(transactionDb: TransactionDb): Single<Int>

    @Query("UPDATE TransactionDb SET ordinal = :ordinal WHERE id=:id")
    fun updateOrdinal(id: Long, ordinal: Int): Single<Int>

    //endregion

    //region Delete

    //endregion
}