package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepository
@Inject constructor(
    private val evidenceDao: EvidenceDao
){
    fun getByTransactionId(transactionId: Long): Observable<List<EvidenceDb>> {
        return evidenceDao.getAllByTransactionId(transactionId)
    }
}