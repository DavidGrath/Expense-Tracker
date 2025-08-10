package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.CategoryDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

class TempCategoryDao() {

    private var incrementId = 0L
    private val categories = arrayListOf<CategoryDb>()
    private val behaviorSubject = BehaviorSubject.create<List<CategoryDb>>()

    fun findByStringId(stringId: String): Maybe<CategoryDb> {
        val category = categories.find { it.stringID == stringId }
        if(category != null) {
            return Maybe.just(category)
        } else {
            return Maybe.empty()
        }
    }

    fun getById(id: Long): Single<CategoryDb> {
        return Single.just(categories.find { it.id == id }!!)
    }
    fun addCategory(categoryDb: CategoryDb): Single<Long> {
        categories.add(categoryDb.copy(id = ++incrementId))
        behaviorSubject.onNext(categories)
        return Single.just(incrementId)
    }

    fun getCategories(): Observable<List<CategoryDb>> {
        return behaviorSubject
    }

    fun getCategoriesSingle(): Single<List<CategoryDb>> {
        return Single.just(categories)
    }
}