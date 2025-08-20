package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.entities.db.CategoryDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject
import javax.inject.Singleton

class CategoryRepository
    @Inject
    constructor(
    private val categoryDao: CategoryDao) {

    fun getCategories(): Observable<List<CategoryDb>> {
        return categoryDao.getAll()
    }

    fun getCategoriesSingle(): Single<List<CategoryDb>> {
        return categoryDao.getAllSingle()
    }

    fun findByStringId(stringId: String): Maybe<CategoryDb> {
        return categoryDao.findByStringId(stringId)
    }
}