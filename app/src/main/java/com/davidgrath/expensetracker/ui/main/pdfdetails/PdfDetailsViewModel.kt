package com.davidgrath.expensetracker.ui.main.pdfdetails

import androidx.lifecycle.ViewModel
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import io.reactivex.rxjava3.core.Single

class PdfDetailsViewModel

constructor(
    val documentId: Long,
    val documentRepository: EvidenceRepository
): ViewModel() {
    val document: Single<EvidenceDb> =
        documentRepository.getDocumentSingle(documentId)

}