package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
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
            .doOnSuccess {
                LOGGER.info("getTransactionItemImages: {} items", it.size)
            }
    }

    fun getAllImagesSingle(profileId: Long): Single<List<ImageDb>> {
        return imageDao.getAllByProfileIdSingle(profileId)
            .subscribeOn(Schedulers.io())
            .doOnSuccess {
                LOGGER.info("getAllImagesSingle: {} items", it.size)
            }
    }

    fun getImageCount(profileId: Long): Observable<Long> {
        return imageDao.countAll(profileId)
            .subscribeOn(Schedulers.io())
    }
    fun getImageCountSingle(profileId: Long): Single<Long> {
        return imageDao.countAllSingle(profileId)
            .subscribeOn(Schedulers.io())
    }

    fun getStorageSum(profileId: Long): Observable<Long> {
        return imageDao.storageSum(profileId)
            .subscribeOn(Schedulers.io())
//            .timeInterval()
//            .map {
//                LOGGER.info("getStorageSum time: {} ms", it.time(TimeUnit.MILLISECONDS))
//                it.value()
//            }
    }

    fun getImagesWithStats(profileId: Long): Observable<List<ImageWithStats>> {
        return imageDao.getAllByProfileId(profileId)
            .subscribeOn(Schedulers.io())
            .map {  list ->
                list.map {
                    val stats = imageDao.getImageStatsSingle(it.id!!).blockingGet()
                    ImageWithStats(it.id, it.profileId, it.sizeBytes, it.sha256, it.mimeType, it.uri, it.createdAt, stats.transactionCount, stats.itemCount)
                }
            }

    }

    fun getImageSingle(id: Long): Single<ImageDb> {
        return imageDao.getById(id)
            .subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageRepository::class.java)
    }
}