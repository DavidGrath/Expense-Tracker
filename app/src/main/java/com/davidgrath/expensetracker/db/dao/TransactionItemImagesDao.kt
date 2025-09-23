package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import io.reactivex.rxjava3.core.Single

@Dao
interface TransactionItemImagesDao {

    //region Create
    @Insert
    fun insertItemImage(transactionItemImage: TransactionItemImagesDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT count(tii.id) FROM TransactionItemImagesDb tii " +
            "INNER JOIN TransactionItemDb ti ON ti.id=tii.transactionItemID " +
            "INNER JOIN TransactionDb t ON t.id=ti.transactionId " +
            "WHERE t.id != :transactionId")
    fun countItemImagesForOtherTransactions(transactionId: Long): Single<Long>
    //endregion

    //region Delete
    @Query("DELETE FROM TransactionItemImagesDb WHERE 1")
    fun deleteAll(): Single<Int>

    @Query("DELETE FROM TransactionItemImagesDb " +
            "WHERE transactionItemID = :transactionItemId " +
            "AND imageID = :imageId")
    fun deleteByItemAndImageId(transactionItemId: Long, imageId: Long): Single<Int>
    //endregion
}