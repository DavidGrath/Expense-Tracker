package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.entities.db.ProfileDb
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository
@Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getByStringId(stringId: String): Single<ProfileDb> {
        return profileDao.getByStringId(stringId)
            .subscribeOn(Schedulers.io())
    }
}