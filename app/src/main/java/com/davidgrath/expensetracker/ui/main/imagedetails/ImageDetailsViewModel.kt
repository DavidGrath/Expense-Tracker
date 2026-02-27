package com.davidgrath.expensetracker.ui.main.imagedetails

import androidx.lifecycle.ViewModel
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class ImageDetailsViewModel

constructor(
    val documentOrImage: Boolean,
    val imageId: Long?,
    val documentId: Long?,
    val imageRepository: ImageRepository,
    val documentRepository: EvidenceRepository
): ViewModel() {
    val uriSizeType: Single<Triple<String, Long, String>> = if(documentOrImage) {
        documentRepository.getDocumentSingle(documentId!!)
            .map { Triple(it.uri, it.sizeBytes, it.mimeType) }
    } else {
        imageRepository.getImageSingle(imageId!!)
            .map { Triple(it.uri, it.sizeBytes, it.mimeType) }
    }
}