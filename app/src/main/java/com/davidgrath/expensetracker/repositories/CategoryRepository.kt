package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.TempCategoryDao
import com.davidgrath.expensetracker.entities.db.CategoryDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class CategoryRepository(private val categoryDao: TempCategoryDao) {

    fun getCategories(): Observable<List<CategoryDb>> {
        return categoryDao.getCategories()
    }

    fun getCategoriesSingle(): Single<List<CategoryDb>> {
        return categoryDao.getCategoriesSingle()
    }

    fun findByStringId(stringId: String): Maybe<CategoryDb> {
        return categoryDao.findByStringId(stringId)
    }
}