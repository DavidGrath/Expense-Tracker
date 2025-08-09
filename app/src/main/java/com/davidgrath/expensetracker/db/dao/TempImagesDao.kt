package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

class TempImagesDao() {

    private var incrementId = 0L
    private val images = arrayListOf<ImageDb>()
    private val behaviorSubject = BehaviorSubject.create<List<ImageDb>>()
    fun doesHashExist(sha256: String): Boolean {
        return images.find { it.sha256 == sha256 } != null
    }

    fun findBySha256(sha256: String): ImageDb {
        return images.find { it.sha256 == sha256 }!!
    }

    fun addImage(imageDb: ImageDb): Single<Long> {
        images.add(imageDb.copy(id = ++incrementId))
        behaviorSubject.onNext(images)
        return Single.just(incrementId)
    }
}