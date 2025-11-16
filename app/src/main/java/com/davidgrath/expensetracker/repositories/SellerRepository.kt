package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.dao.SellerDao
import com.davidgrath.expensetracker.db.dao.SellerLocationDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.SellerDb
import com.davidgrath.expensetracker.entities.db.SellerLocationDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SellerRepository
@Inject
constructor(
    private val sellerDao: SellerDao,
    private val sellerLocationDao: SellerLocationDao,
    private val timeAndLocaleHandler: TimeAndLocaleHandler
) {

    fun createSeller(profileId: Long, name: String): Single<Long> {
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val seller = SellerDb(
            null,
            profileId,
            name,
            dateTime,
            offset,
            zone
        )
        return sellerDao.insertSeller(seller)
            .subscribeOn(Schedulers.io())
    }

    fun createSellerLocation(location: String, sellerId: Long): Single<Long> {
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val seller = SellerLocationDb(
            null,
            sellerId,
            location,
            false,
            null, null, null,
            dateTime,
            offset,
            zone
        )
        return sellerLocationDao.insertSellerLocation(seller)
            .subscribeOn(Schedulers.io())
    }

    fun getSellers(profileId: Long): Observable<List<SellerDb>> {
        return sellerDao.getAllByProfileId(profileId)
            .subscribeOn(Schedulers.io())
    }

    fun getSellerByIdSingle(id: Long): Single<SellerDb> {
        return sellerDao.getByIdSingle(id)
            .subscribeOn(Schedulers.io())
    }

    fun getSellersSingle(profileId: Long): Single<List<SellerDb>> {
        return sellerDao.getAllByProfileIdSingle(profileId)
            .subscribeOn(Schedulers.io())
    }

    fun getSellerLocations(sellerId: Long): Observable<List<SellerLocationDb>> {
        return sellerLocationDao.getAllBySeller(sellerId)
            .subscribeOn(Schedulers.io())
    }


    fun getSellerLocationByIdSingle(id: Long): Single<SellerLocationDb> {
        return sellerLocationDao.getByIdSingle(id)
            .subscribeOn(Schedulers.io())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerRepository::class.java)
    }
}