package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.views.CategoryWithStats
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

class CategoryRepository
    @Inject
    constructor(
    private val categoryDao: CategoryDao,
        private val timeAndLocaleHandler: TimeAndLocaleHandler
        ) {

    fun addCategory(profileId: Long, categoryName: String, iconId: String): Single<Long> {
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val category = CategoryDb(null, profileId, null, true, categoryName, dateTime, offset, zone, iconId)
        return categoryDao.insertCategory(category)
            .subscribeOn(Schedulers.io())
    }

    fun getCategoriesWithStats(profileId: Long): Observable<List<CategoryWithStats>> {
        return categoryDao.getAllByProfileIdWithStats(profileId)
            .subscribeOn(Schedulers.io())
    }

    fun getCategoriesSingle(profileId: Long): Single<List<CategoryDb>> {
        return categoryDao.getAllSingle(profileId)
            .subscribeOn(Schedulers.io())
            .doOnSuccess {
                LOGGER.info("getCategories: {} items", it.size)
            }
    }

    fun findByProfileIdAndStringId(profileId: Long, stringId: String): Maybe<CategoryDb> {
        return categoryDao.findByProfileIdAndStringId(profileId, stringId)
    }

    fun getById(id: Long): Single<CategoryDb> {
        return categoryDao.findById(id).toSingle()
    }

    fun getOtherItemCategories(transactionItemId: Long): Single<List<CategoryDb>> {
        return categoryDao.getOthersByTransactionItemId(transactionItemId)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CategoryRepository::class.java)
    }
}