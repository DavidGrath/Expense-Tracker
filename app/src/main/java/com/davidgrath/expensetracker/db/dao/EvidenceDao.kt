package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface EvidenceDao {
    //region Create
    @Insert
    fun insertEvidence(evidenceDb: EvidenceDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT * FROM EvidenceDb WHERE transactionId=:transactionId")
    fun getAllByTransactionIdSingle(transactionId: Long): Single<List<EvidenceDb>>

    @Query("SELECT * FROM EvidenceDb WHERE transactionId=:transactionId")
    fun getAllByTransactionId(transactionId: Long): Observable<List<EvidenceDb>>

    @Query("SELECT count(id) > 0 FROM EvidenceDb " +
            "WHERE transactionId = :transactionId " +
            "AND sha256 = :sha256")
    fun doesHashExist(transactionId: Long, sha256: String): Single<Boolean>

    @Query("SELECT * FROM EvidenceDb WHERE transactionId = :transactionId AND sha256 = :sha256")
    fun findByTransactionIdAndSha256(transactionId: Long, sha256: String): Maybe<EvidenceDb>

    @Query("SELECT sum(sizeBytes) FROM EvidenceDb e " +
            "INNER JOIN TransactionDb t ON e.transactionId=t.id " +
            "INNER JOIN AccountDb a ON a.id=t.accountId " +
            "WHERE a.profileId = :profileId")
    fun storageSum(profileId: Long): Single<Long>
    //endregion

    //region Delete

    @Query("DELETE FROM EvidenceDb " +
            "WHERE id = :id")
    fun deleteByIdSingle(id: Long): Single<Int>

    //endregion
}