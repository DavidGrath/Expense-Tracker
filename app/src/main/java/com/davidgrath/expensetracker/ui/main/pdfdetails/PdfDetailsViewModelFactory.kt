package com.davidgrath.expensetracker.ui.main.pdfdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.di.MainComponent
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import javax.inject.Inject

class PdfDetailsViewModelFactory(
    val documentId: Long,
    private val appComponent: MainComponent,
) : ViewModelProvider.Factory {

    @Inject
    lateinit var documentRepository: EvidenceRepository

    init {
        appComponent.inject(this)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PdfDetailsViewModel(documentId, documentRepository) as T
    }
}