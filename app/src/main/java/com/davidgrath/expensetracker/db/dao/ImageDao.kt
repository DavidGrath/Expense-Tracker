package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface ImageDao {
    //region Create
    @Insert
    fun insertImage(imageDb: ImageDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT i.* FROM ImageDb i " +
            "WHERE i.profileId = :profileId ")
    fun getAllByProfileId(profileId: Long): Observable<List<ImageDb>>
    @Query("SELECT i.* FROM ImageDb i " +
            "WHERE i.profileId = :profileId ")
    fun getAllByProfileIdSingle(profileId: Long): Single<List<ImageDb>>

    @Query("SELECT * FROM ImageDb WHERE id IN (:ids)")
    fun getAllByIdsSingle(ids: List<Long>): Single<List<ImageDb>>

    @Query("SELECT * FROM ImageDb WHERE id = :id")
    fun getById(id: Long): Single<ImageDb>

    @Query("SELECT count(id) > 0 FROM ImageDb " +
            "WHERE profileId = :profileId " +
            "AND sha256 = :sha256")
    fun doesHashExist(profileId: Long, sha256: String): Single<Boolean>

    @Query("SELECT * FROM ImageDb " +
            "WHERE profileId = :profileId " +
            "AND sha256 = :sha256")
    fun findBySha256(profileId: Long, sha256: String): Maybe<ImageDb>

    @Query("SELECT im.* FROM ImageDb im INNER JOIN TransactionItemImagesDb tii ON tii.imageID = im.id " +
            "WHERE tii.transactionItemID = :itemId")
    fun getAllByItemSingle(itemId: Long): Single<List<ImageDb>>

    @Query("SELECT ifnull(sum(sizeBytes), 0) FROM ImageDb " +
            "WHERE profileId = :profileId")
    fun storageSum(profileId: Long): Observable<Long>
    @Query("SELECT ifnull(sum(sizeBytes), 0) FROM ImageDb " +
            "WHERE profileId = :profileId")
    fun storageSumSingle(profileId: Long): Single<Long>

    @Query("SELECT count(*) FROM ImageDb " +
            "WHERE profileId = :profileId")
    fun countAll(profileId: Long): Observable<Long>
    @Query("SELECT count(*) FROM ImageDb " +
            "WHERE profileId = :profileId")
    fun countAllSingle(profileId: Long): Single<Long>

    @Query("SELECT count(distinct(t.id)) as transactionCount, count(ti.id) as itemCount " +
            "FROM ImageDb i " +
            "INNER JOIN TransactionItemImagesDb tii ON  tii.imageId=i.id " +
            "INNER JOIN TransactionItemDb ti ON ti.id=tii.transactionItemId " +
            "INNER JOIN TransactionDb t ON t.id=ti.transactionId " +
            "WHERE i.id = :id " +
            "ORDER BY tii.createdAt DESC"
    )
    fun getImageStatsSingle(id: Long): Single<TransactionAndItemCount>
    //endregion

    //region Delete

    @Query("DELETE FROM ImageDb " +
            "WHERE id = :id")
    fun deleteByIdSingle(id: Long): Single<Int>
    //endregion
}