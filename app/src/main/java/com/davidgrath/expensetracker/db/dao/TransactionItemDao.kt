package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

@Dao
interface TransactionItemDao {
    //region Create
    @Insert
    fun insertTransactionItem(transactionItemDb: TransactionItemDb): Single<Long>
    @Insert
    fun insertTransactionItemMultiple(transactionItemDbs: List<TransactionItemDb>): Single<List<Long>>
    //endregion

    //region Read
    @Query("SELECT * FROM TransactionItemDb WHERE transactionId = :transactionId")
    fun getAllByTransactionId(transactionId: Long): Observable<List<TransactionItemDb>>

    @Query("SELECT * FROM TransactionItemDb WHERE transactionId = :transactionId")
    fun getAllByTransactionIdSingle(transactionId: Long): Single<List<TransactionItemDb>>

    @Query("SELECT c.id as categoryId, c.stringID, c.isCustom, c.name, sum(ti.amount) as sum FROM TransactionItemDb ti " +
            "INNER JOIN CategoryDb c ON ti.primaryCategoryId = c.id " +
            "INNER JOIN TransactionDb t ON ti.transactionId = t.id " +
            "WHERE date(t.datedAt) >= date(:fromDate) " +
            "AND t.debitOrCredit " +
            "GROUP BY ti.primaryCategoryId")
    fun getDebitSumByCategoryFrom(fromDate: String): Observable<List<ItemSumByCategory>>

    @Query("SELECT c.id as categoryId, c.stringID, c.isCustom, c.name, sum(ti.amount) as sum FROM TransactionItemDb ti " +
            "INNER JOIN CategoryDb c ON ti.primaryCategoryId = c.id " +
            "INNER JOIN TransactionDb t ON ti.transactionId = t.id " +
            "WHERE date(t.datedAt) >= date(:fromDate) " +
            "AND date(t.datedAt) <= date(:toDate) " +
            "AND t.debitOrCredit " +
            "GROUP BY ti.primaryCategoryId")
    fun getDebitSumByCategoryFromTo(fromDate: String, toDate: String): Observable<List<ItemSumByCategory>>

    @Query("SELECT ti.transactionId, ti.id AS itemId, t.accountId,ti.primaryCategoryId, " +
            "t.amount AS transactionTotal, ti.amount AS itemAmount,t.currencyCode, t.debitOrCredit, " +
            "ti.description, t.createdAt AS transactionCreatedAt, t.createdAtOffset AS transactionCreatedAtOffset, " +
            "t.createdAtTimezone AS transactionCreatedAtTimezone, t.datedAt AS transactionDatedAt, " +
            "t.datedAtTime AS transactionDatedAtTime, " +
            "t.datedAtOffset AS transactionDatedAtOffset, t.datedAtTimezone AS transactionDatedAtTimezone, " +
            "c.stringID AS categoryStringID, c.isCustom AS categoryIsCustom, c.name AS categoryName " +
            "FROM TransactionItemDb ti " +
            "INNER JOIN TransactionDb t ON t.id = ti.transactionId " +
            "INNER JOIN CategoryDb c ON c.id = ti.primaryCategoryId " +
            "WHERE date(t.datedAt) >= date(:fromDate) " +
            "ORDER BY date(t.datedAt), time(datedAtTime)") //TODO Think about ordinals later
    fun getItemsWithTransactionsAndCategoryFrom(fromDate: String): Flowable<List<TransactionWithItemAndCategory>>

    @Query("SELECT ti.transactionId, ti.id AS itemId, t.accountId,ti.primaryCategoryId, " +
            "t.amount AS transactionTotal, ti.amount AS itemAmount,t.currencyCode, t.debitOrCredit, " +
            "ti.description, t.createdAt AS transactionCreatedAt, t.createdAtOffset AS transactionCreatedAtOffset, " +
            "t.createdAtTimezone AS transactionCreatedAtTimezone, " +
            "t.datedAt AS transactionDatedAt, t.datedAtTime AS transactionDatedAtTime, " +
            "t.datedAtOffset AS transactionDatedAtOffset, t.datedAtTimezone AS transactionDatedAtTimezone, " +
            "c.stringID AS categoryStringID, c.isCustom AS categoryIsCustom, c.name AS categoryName " +
            "FROM TransactionItemDb ti " +
            "INNER JOIN TransactionDb t ON t.id = ti.transactionId " +
            "INNER JOIN CategoryDb c ON c.id = ti.primaryCategoryId " +
            "WHERE date(t.datedAt) >= date(:fromDate)" +
            "AND date(t.datedAt) <= date(:toDate) " +
            "ORDER BY date(t.datedAt), time(datedAtTime)") // TODO Think about ordinals later
    fun getItemsWithTransactionsAndCategoryFromTo(fromDate: String, toDate: String): Observable<List<TransactionWithItemAndCategory>>
    @Query("SELECT * FROM TransactionItemDb WHERE id = :id")
    fun getByIdSingle(id: Long): Single<TransactionItemDb>

    @Query("SELECT count(distinct(t.id)) as transactionCount, count(ti.id) as itemCount " +
            "FROM TransactionDb t INNER JOIN TransactionItemDb ti ON ti.transactionId=t.id " +
            "WHERE (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds))"
    )
    fun getTransactionItemCount(fromDate: String? = null, toDate: String? = null, emptyAccounts: Boolean, accountIds: List<Long>): Observable<TransactionAndItemCount>
    // endregion

    //region Update
    @Query("UPDATE TransactionItemDb SET amount = :amount, brand = :brand, quantity = :quantity, " +
            "description = :description, primaryCategoryId = :primaryCategoryId " +
            "WHERE id = :id")
    fun updateTransactionItem(id: Long, amount: BigDecimal, brand: String?, quantity: Int, description: String, primaryCategoryId: Long): Single<Int>
    //endregion

    //region Delete
    @Query("DELETE FROM TransactionItemDb WHERE 1")
    fun deleteAll(): Single<Int>
    @Query("DELETE FROM TransactionItemDb WHERE id = :id")
    fun deleteById(id: Long): Single<Int>

    /*@Query("DELETE FROM TransactionItemDb WHERE id = :transactionId")
    fun deleteByTransactionId(transactionId: Long): Single<Int>*/
    //endregion
}