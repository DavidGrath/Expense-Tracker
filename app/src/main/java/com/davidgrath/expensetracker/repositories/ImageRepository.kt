package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.entities.db.ImageDb
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository
@Inject constructor(
    private val imageDao: ImageDao,
){
    fun getTransactionItemImages(itemId: Long): Single<List<ImageDb>> {
        return imageDao.getAllByItemSingle(itemId)
            .subscribeOn(Schedulers.io())
    }
}