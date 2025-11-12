package com.davidgrath.expensetracker.ui.main.documents

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.views.EvidenceWithTransactionDateAndOrdinal
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import io.reactivex.rxjava3.core.BackpressureStrategy
import javax.inject.Inject

class DocumentStatsViewModel
@Inject
    constructor(
        val application: Application,
        val evidenceRepository: EvidenceRepository
    )
    : ViewModel() {

        val documents: LiveData<List<EvidenceWithTransactionDateAndOrdinal>>
        val totalSize: LiveData<Long>

        init {
            val profileObservable = (application as ExpenseTracker).profileObservable

            totalSize = profileObservable.switchMap {
                evidenceRepository.getTotalSizeBytes(it.id!!)
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()

            documents = profileObservable.switchMap {
                evidenceRepository.getAllByProfileId(it.id!!)
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
}