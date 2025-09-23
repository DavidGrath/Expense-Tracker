package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.ImageDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

@Dao
interface ImageDao {
    //region Create
    @Insert
    fun insertImage(imageDb: ImageDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT * FROM ImageDb")
    fun getAllSingle(): Single<List<ImageDb>>

    @Query("SELECT * FROM ImageDb WHERE id IN (:ids)")
    fun getAllByIdsSingle(ids: List<Long>): Single<List<ImageDb>>

    @Query("SELECT * FROM ImageDb WHERE id = :id")
    fun getById(id: Long): Single<ImageDb>

    @Query("SELECT count(id) > 0 FROM ImageDb WHERE sha256 = :sha256")
    fun doesHashExist(sha256: String): Single<Boolean>

    @Query("SELECT * FROM ImageDb WHERE sha256 = :sha256")
    fun findBySha256(sha256: String): Maybe<ImageDb>

    @Query("SELECT im.* FROM ImageDb im INNER JOIN TransactionItemImagesDb tii ON tii.imageID = im.id " +
            "WHERE tii.transactionItemID = :itemId")
    fun getAllByItemSingle(itemId: Long): Single<List<ImageDb>>

    @Query("SELECT sum(sizeBytes) FROM ImageDb")
    fun storageSum(): Single<Long>
    //endregion

    //region Delete
    @Query("DELETE FROM ImageDb WHERE 1")
    fun deleteAll(): Single<Int>

    @Query("DELETE FROM ImageDb " +
            "WHERE id = :id")
    fun deleteByIdSingle(id: Long): Single<Int>
    //endregion
}