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
    //endregion

    //region Delete
    @Query("DELETE FROM TransactionItemImagesDb WHERE 1")
    fun deleteAll(): Single<Int>
    //endregion
}