package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.views.StringFieldWithTimestamp
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
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

    @Query("SELECT c.id as categoryId, c.stringId, c.isCustom, c.name, sum(ti.amount) as sum, c.icon AS categoryIcon " +
            "FROM TransactionItemDb ti " + //TODO how to factor reductions into this?
            "INNER JOIN CategoryDb c ON ti.primaryCategoryId = c.id " +
            "INNER JOIN TransactionDb t ON ti.transactionId = t.id " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds)) " +
            "AND (:datesEmpty OR date(datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR (ti.primaryCategoryId in (:categories))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "AND t.debitOrCredit " +
            "GROUP BY ti.primaryCategoryId " +
            "ORDER BY ti.primaryCategoryId ")
    fun getDebitSumByCategory(
        profileId: Long,
        fromDate: String?, toDate: String?,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<List<ItemSumByCategory>>

    @Query("SELECT c.id as categoryId, c.stringId, c.isCustom, c.name, sum(ti.amount) as sum, c.icon AS categoryIcon " +
            "FROM TransactionItemDb ti " + //TODO how to factor reductions into this?
            "INNER JOIN CategoryDb c ON ti.primaryCategoryId = c.id " +
            "INNER JOIN TransactionDb t ON ti.transactionId = t.id " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds)) " +
            "AND (:datesEmpty OR date(datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR (ti.primaryCategoryId in (:categories))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "AND not(t.debitOrCredit) " +
            "GROUP BY ti.primaryCategoryId " +
            "ORDER BY ti.primaryCategoryId ")
    fun getCreditSumByCategory(
        profileId: Long,
        fromDate: String?, toDate: String?,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<List<ItemSumByCategory>>

    @Query("SELECT ti.transactionId, ti.id AS itemId, t.accountId,ti.primaryCategoryId, " +
            "t.amount AS transactionTotal, ti.amount AS itemAmount,t.currencyCode, t.debitOrCredit, " +
            "ti.description, t.createdAt AS transactionCreatedAt, t.createdAtOffset AS transactionCreatedAtOffset, " +
            "t.createdAtTimezone AS transactionCreatedAtTimezone, t.datedAt AS transactionDatedAt, " +
            "t.datedAtTime AS transactionDatedAtTime, " +
            "c.stringId AS categoryStringId, c.isCustom AS categoryIsCustom, c.name AS categoryName, c.icon as categoryIcon " +
            "FROM TransactionItemDb ti " +
            "INNER JOIN TransactionDb t ON t.id = ti.transactionId " +
            "INNER JOIN CategoryDb c ON c.id = ti.primaryCategoryId " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds)) " +
            "AND (:datesEmpty OR date(datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR (ti.primaryCategoryId in (:categories))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "ORDER BY date(t.datedAt), t.ordinal, ti.ordinal")
    fun getItemsWithTransactionsAndCategory(
        profileId: Long,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<List<TransactionWithItemAndCategory>>
    @Query("SELECT ti.transactionId, ti.id AS itemId, t.accountId,ti.primaryCategoryId, " +
            "t.amount AS transactionTotal, ti.amount AS itemAmount,t.currencyCode, t.debitOrCredit, " +
            "ti.description, t.createdAt AS transactionCreatedAt, t.createdAtOffset AS transactionCreatedAtOffset, " +
            "t.createdAtTimezone AS transactionCreatedAtTimezone, t.datedAt AS transactionDatedAt, " +
            "t.datedAtTime AS transactionDatedAtTime, " +
            "c.stringId AS categoryStringId, c.isCustom AS categoryIsCustom, c.name AS categoryName," +
            "c.icon AS categoryIcon " +
            "FROM TransactionItemDb ti " +
            "INNER JOIN TransactionDb t ON t.id = ti.transactionId " +
            "INNER JOIN CategoryDb c ON c.id = ti.primaryCategoryId " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds)) " +
            "AND (:datesEmpty OR date(datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR (ti.primaryCategoryId in (:categories))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) " +
            "ORDER BY date(t.datedAt), t.ordinal, ti.ordinal")
    fun getItemsWithTransactionsAndCategorySingle(
        profileId: Long,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Single<List<TransactionWithItemAndCategory>>

    @Query("SELECT * FROM TransactionItemDb WHERE id = :id")
    fun getByIdSingle(id: Long): Single<TransactionItemDb>

    @Query("SELECT count(distinct(t.id)) as transactionCount, count(ti.id) as itemCount " +
            "FROM TransactionDb t INNER JOIN TransactionItemDb ti ON ti.transactionId=t.id " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds)) " +
            "AND (:datesEmpty OR date(datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR (ti.primaryCategoryId in (:categories))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) "
    )
    fun getTransactionItemCount(
        profileId: Long,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Observable<TransactionAndItemCount>

    @Query("SELECT count(distinct(t.id)) as transactionCount, count(ti.id) as itemCount " +
            "FROM TransactionDb t INNER JOIN TransactionItemDb ti ON ti.transactionId=t.id " +
            "INNER JOIN AccountDb a ON a.id = t.accountId " +
            "WHERE a.profileId=:profileId " +
            "AND (:fromDate IS NULL OR date(datedAt) >= date(:fromDate)) " +
            "AND (:toDate IS NULL OR date(datedAt) <= date(:toDate)) " +
            "AND (:emptyAccounts OR t.accountId in (:accountIds)) " +
            "AND (:datesEmpty OR date(datedAt) in (:dates)) " +
            "AND (:categoriesEmpty OR (ti.primaryCategoryId in (:categories))) " +
            "AND (:modesEmpty OR (t.mode in (:modes))) " +
            "AND (:sellersEmpty OR (t.sellerId in (:sellers))) "
    )
    fun getTransactionItemCountSingle(
        profileId: Long,
        fromDate: String? = null, toDate: String? = null,
        emptyAccounts: Boolean, accountIds: List<Long>,
        datesEmpty: Boolean, dates: List<String>,
        categoriesEmpty: Boolean, categories: List<Long>,
        modesEmpty: Boolean, modes: List<TransactionMode>,
        sellersEmpty: Boolean, sellers: List<Long>
    ): Single<TransactionAndItemCount>

    @Query("SELECT ti.description as stringField, ti.createdAt as createdAt FROM TransactionItemDb ti WHERE ti.description LIKE :prefixString || '%' " +
            "ORDER BY ti.createdAt DESC " +
            "LIMIT 1000")
    fun getDescriptionsAndDates(prefixString: String): Single<List<StringFieldWithTimestamp>>

    @Query("SELECT ti.brand as stringField, ti.createdAt as createdAt FROM TransactionItemDb ti WHERE ti.brand LIKE :prefixString || '%' " +
            "ORDER BY ti.createdAt DESC " +
            "LIMIT 1000")
    fun getBrandsAndDates(prefixString: String): Single<List<StringFieldWithTimestamp>>


    @Query("SELECT ti.variation as stringField, ti.createdAt as createdAt FROM TransactionItemDb ti WHERE ti.variation LIKE :prefixString || '%' " +
            "ORDER BY ti.createdAt DESC " +
            "LIMIT 1000")
    fun getVariationsAndDates(prefixString: String): Single<List<StringFieldWithTimestamp>>
    // endregion

    //region Update
    @Query("UPDATE TransactionItemDb SET amount = :amount, brand = :brand, quantity = :quantity, " +
            "description = :description, primaryCategoryId = :primaryCategoryId " +
            "WHERE id = :id")
    fun updateTransactionItem(id: Long, amount: BigDecimal, brand: String?, quantity: Int, description: String, primaryCategoryId: Long): Single<Int>
    //endregion

    //region Delete
    @Query("DELETE FROM TransactionItemDb WHERE id = :id")
    fun deleteById(id: Long): Single<Int>
    @Query("DELETE FROM TransactionItemDb WHERE transactionId = :transactionId")
    fun deleteByTransactionId(transactionId: Long): Single<Int>

    /*@Query("DELETE FROM TransactionItemDb WHERE id = :transactionId")
    fun deleteByTransactionId(transactionId: Long): Single<Int>*/
    //endregion
}