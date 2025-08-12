package com.davidgrath.expensetracker.db.dao

import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

class TempTransactionItemImagesDao {

    private var incrementId = 0L
    private val itemImages = arrayListOf<TransactionItemImagesDb>()
    private val behaviorSubject = BehaviorSubject.create<List<TransactionItemImagesDb>>()


    fun addImage(transactionItemImagesDb: TransactionItemImagesDb): Single<Long> {
        itemImages.add(transactionItemImagesDb.copy(id = ++incrementId))
        behaviorSubject.onNext(itemImages)
        return Single.just(incrementId)
    }

    fun getTransactionItemImages(): Observable<List<TransactionItemImagesDb>> {
        return behaviorSubject
    }

    fun getTransactionItemImagesByItemIdSingle(itemId: Long): Single<List<TransactionItemImagesDb>> {
        return Single.just(itemImages.filter { it.transactionItemID == itemId })

    }
}