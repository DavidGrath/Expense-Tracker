package com.davidgrath.expensetracker.ui.addtransaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.repositories.ImageRepository

class AddDetailedTransactionGetImageViewModel(
    private val imageRepository: ImageRepository
): ViewModel() {

    fun getImages() : LiveData<List<ImageDb>> {
        return imageRepository.getAllImagesSingle().toFlowable().toLiveData()
    }
}