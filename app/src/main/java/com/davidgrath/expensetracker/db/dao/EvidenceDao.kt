package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

@Dao
interface EvidenceDao {
    //region Create
    @Insert
    fun insertEvidence(evidenceDb: EvidenceDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT count(id) > 0 FROM EvidenceDb WHERE sha256 = :sha256")
    fun doesHashExist(sha256: String): Single<Boolean>

    @Query("SELECT * FROM EvidenceDb WHERE sha256 = :sha256")
    fun findBySha256(sha256: String): Maybe<EvidenceDb>
    //endregion
}