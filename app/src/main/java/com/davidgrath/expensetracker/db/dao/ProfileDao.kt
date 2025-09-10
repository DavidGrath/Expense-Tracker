package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.ProfileDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

@Dao
interface ProfileDao {
    //region Create
    @Insert
    fun insertProfile(profileDb: ProfileDb): Single<Long>
    //endregion
    //region Read
    @Query("SELECT * FROM ProfileDb WHERE stringId = :stringId")
    fun getByStringId(stringId: String): Single<ProfileDb>
    @Query("SELECT * FROM ProfileDb WHERE stringId = :stringId")
    fun findByStringId(stringId: String): Maybe<ProfileDb>
    @Query("SELECT count(id) from ProfileDb")
    fun countProfiles(): Single<Long>
    //endregion
}