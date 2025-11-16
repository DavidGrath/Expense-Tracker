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
    @Query("SELECT * FROM CategoryDb " +
            "WHERE profileId = :profileId")
    fun getAll(profileId: Long, ): Observable<List<CategoryDb>>

    @Query("SELECT * FROM CategoryDb " +
            "WHERE profileId = :profileId")
    fun getAllSingle(profileId: Long, ): Single<List<CategoryDb>>

    @Query("SELECT * FROM  CategoryDb WHERE id = :id")
    fun findById(id: Long): Maybe<CategoryDb>

    @Query("SELECT * FROM CategoryDb " +
            "WHERE profileId = :profileId " +
            "AND stringId = :stringId")
    fun findByProfileIdAndStringId(profileId: Long, stringId: String): Maybe<CategoryDb>

    @Query("SELECT c.* FROM CategoryDb c " +
            "INNER JOIN TransactionItemCategoriesDb t on t.categoryId=c.id " +
            "WHERE t.transactionItemId=:transactionItemId")
    fun getOthersByTransactionItemId(transactionItemId: Long): Single<List<CategoryDb>>
    //endregion
}