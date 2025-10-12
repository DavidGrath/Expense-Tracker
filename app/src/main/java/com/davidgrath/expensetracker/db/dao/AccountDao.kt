package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.views.AccountWithStats
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

@Dao
interface AccountDao {
    //region Create
    @Insert
    fun insertAccount(accountDb: AccountDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT * FROM AccountDb WHERE profileId=:profileId")
    fun getAllByProfileId(profileId: Long): Observable<List<AccountDb>>

    @Query("SELECT * FROM AccountDb WHERE profileId=:profileId")
    fun getAllByProfileIdSingle(profileId: Long): Single<List<AccountDb>>

    @Query("SELECT * FROM AccountDb WHERE id=:id")
    fun findByIdSingle(id: Long): Maybe<AccountDb>

    @Query("SELECT a.*, " +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND debitOrCredit) as expenses," +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND not(debitOrCredit)) as income," +
            "(SELECT count(id) FROM TransactionDb WHERE accountId = a.id) as transactionCount," +
            "(SELECT count(ti.id) FROM TransactionItemDb ti INNER JOIN TransactionDb t ON t.id=ti.transactionId WHERE t.accountId = a.id) as itemCount " +
            "FROM AccountDb a " +
            "WHERE a.id=:id"
    )
    fun getAccountSummary(id: Long): Observable<AccountWithStats>

    @Query("SELECT a.*, " +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND debitOrCredit) as expenses," +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND not(debitOrCredit)) as income," +
            "(SELECT count(id) FROM TransactionDb WHERE accountId = a.id) as transactionCount," +
            "(SELECT count(ti.id) FROM TransactionItemDb ti INNER JOIN TransactionDb t ON t.id=ti.transactionId WHERE t.accountId = a.id) as itemCount " +
            "FROM AccountDb a " +
            "WHERE a.id=:id"
    )
    fun getAccountSummarySingle(id: Long): Single<AccountWithStats>


    @Query("SELECT a.*, " +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND debitOrCredit) as expenses," +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND not(debitOrCredit)) as income," +
            "(SELECT count(id) FROM TransactionDb WHERE accountId = a.id) as transactionCount," +
            "(SELECT count(ti.id) FROM TransactionItemDb ti INNER JOIN TransactionDb t ON t.id=ti.transactionId WHERE t.accountId = a.id) as itemCount " +
            "FROM AccountDb a " +
            "WHERE a.profileId=:profileId"
    )
    fun getAllByProfileIdWithStats(profileId: Long): Observable<List<AccountWithStats>>

    @Query("SELECT a.*, " +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND debitOrCredit) as expenses," +
            "(SELECT ifnull(sum(amount), 0) FROM TransactionDb WHERE accountId = a.id AND not(debitOrCredit)) as income," +
            "(SELECT count(id) FROM TransactionDb WHERE accountId = a.id) as transactionCount," +
            "(SELECT count(ti.id) FROM TransactionItemDb ti INNER JOIN TransactionDb t ON t.id=ti.transactionId WHERE t.accountId = a.id) as itemCount " +
            "FROM AccountDb a " +
            "WHERE a.profileId=:profileId"
    )
    fun getAllByProfileIdWithStatsSingle(profileId: Long): Single<List<AccountWithStats>>
    //endregion

    //region Update
    @Query("UPDATE AccountDb SET name = :name " +
            "WHERE id = :id") //AND profileId = :profileId
    fun updateAccountName(id: Long, name: String): Single<Int>
    // endregion

    //region Delete

    @Query("DELETE FROM AccountDb " +
            "WHERE id = :id " +
            "AND profileId = :profileId")
    fun deleteAccountById(profileId: Long, id: Long): Single<Int>
    //endregion
}