package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.AccountDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

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

    //endregion
}