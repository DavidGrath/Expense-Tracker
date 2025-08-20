package com.davidgrath.expensetracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidgrath.expensetracker.entities.db.CategoryDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface CategoryDao {
    //region Create
    @Insert
    fun insertCategory(categoryDb: CategoryDb): Single<Long>
    //endregion

    //region Read
    @Query("SELECT * FROM CategoryDb")
    fun getAll(): Observable<List<CategoryDb>>

    @Query("SELECT * FROM CategoryDb")
    fun getAllSingle(): Single<List<CategoryDb>>

    @Query("SELECT * FROM  CategoryDb WHERE id = :id")
    fun findById(id: Long): Maybe<CategoryDb>

    @Query("SELECT * FROM CategoryDb WHERE stringID = :stringId")
    fun findByStringId(stringId: String): Maybe<CategoryDb>
    //endregion
}