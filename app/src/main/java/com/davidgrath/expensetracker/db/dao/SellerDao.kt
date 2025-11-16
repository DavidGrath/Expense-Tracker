package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.SellerDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface SellerDao {

    //region Create

    @Insert
    fun insertSeller(sellerDb: SellerDb): Single<Long>

    //endregion

    //region Read

    @Query("SELECT * FROM SellerDb WHERE id = :id")
    fun getByIdSingle(id: Long): Single<SellerDb>


    @Query("SELECT * FROM SellerDb WHERE profileId = :profileId")
    fun getAllByProfileId(profileId: Long): Observable<List<SellerDb>>

    @Query("SELECT * FROM SellerDb WHERE profileId = :profileId")
    fun getAllByProfileIdSingle(profileId: Long): Single<List<SellerDb>>

    //endregion

    //region Update
    @Query("UPDATE SellerDb SET name = :name WHERE id = :id")
    fun updateSellerName(id: Long, name: String): Single<Int>
    //endregion
}