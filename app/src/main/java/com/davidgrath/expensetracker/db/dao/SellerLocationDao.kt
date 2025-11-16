package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.SellerDb
import com.davidgrath.expensetracker.entities.db.SellerLocationDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface SellerLocationDao {
    //region Create

    @Insert
    fun insertSellerLocation(sellerLocationDb: SellerLocationDb): Single<Long>

    //endregion

    //region Read

    @Query("SELECT * FROM SellerLocationDb WHERE id = :id")
    fun getByIdSingle(id: Long): Single<SellerLocationDb>

    @Query("SELECT * FROM SellerLocationDb WHERE sellerId = :sellerId")
    fun getAllBySeller(sellerId: Long): Observable<List<SellerLocationDb>>

    @Query("SELECT * FROM SellerLocationDb WHERE sellerId = :sellerId")
    fun getAllBySellerSingle(sellerId: Long): Single<List<SellerLocationDb>>

    //endregion
}