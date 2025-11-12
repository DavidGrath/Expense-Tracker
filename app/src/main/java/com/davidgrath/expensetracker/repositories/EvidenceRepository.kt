package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.views.EvidenceWithTransactionDateAndOrdinal
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepository
@Inject constructor(
    private val evidenceDao: EvidenceDao
){
    fun getByTransactionId(transactionId: Long): Observable<List<EvidenceDb>> {
        return evidenceDao.getAllByTransactionId(transactionId)
            .subscribeOn(Schedulers.io())
    }

    fun getAllByProfileId(profileId: Long): Observable<List<EvidenceWithTransactionDateAndOrdinal>> {
        return evidenceDao.getAllByProfileId(profileId)
            .subscribeOn(Schedulers.io())
    }

    fun getTotalSizeBytes(profileId: Long): Observable<Long> {
        return evidenceDao.storageSum(profileId)
            .subscribeOn(Schedulers.io())
    }
}