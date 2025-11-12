package com.davidgrath.expensetracker.ui.main.images

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import com.davidgrath.expensetracker.repositories.ImageRepository
import io.reactivex.rxjava3.core.BackpressureStrategy
import javax.inject.Inject

class ImageStatsViewModel
@Inject
    constructor(
        val application: Application,
        val imageRepository: ImageRepository
    )
    : ViewModel() {

        val imageCount: LiveData<Long>
        val totalSize: LiveData<Long>
        val imageStats: LiveData<List<ImageWithStats>>

        init {
            val profileObservable = (application as ExpenseTracker).profileObservable
            imageCount = profileObservable.switchMap {
                imageRepository.getImageCount(it.id!!)
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()

            totalSize = profileObservable.switchMap {
                imageRepository.getStorageSum(it.id!!)
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()

            imageStats = profileObservable.switchMap {
                imageRepository.getImagesWithStats(it.id!!)
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
}