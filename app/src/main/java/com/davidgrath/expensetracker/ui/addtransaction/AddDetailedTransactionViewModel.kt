package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class AddDetailedTransactionViewModel(
    private val application: Application,
    private val addDetailedTransactionRepository: AddDetailedTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val initialAmount: BigDecimal?,
    private val initialDescription: String?,
    private val initialCategoryId: Long?
) : AndroidViewModel(application) {


    var getImageItemId = -1

    init {
        if (!addDetailedTransactionRepository.draftExists()) {
            if (initialAmount != null || initialDescription != null || initialCategoryId != null) {
                addDetailedTransactionRepository.createDraft()
                addDetailedTransactionRepository.initializeDraft(
                    initialAmount,
                    initialDescription,
                    initialCategoryId
                )
            } else {
                addDetailedTransactionRepository.createDraft()
                addDetailedTransactionRepository.addItem()
            }
        } else {
            if (initialAmount != null || initialDescription != null || initialCategoryId != null) {
                addDetailedTransactionRepository.moveToTopOfDraft(
                    initialAmount,
                    initialDescription,
                    initialCategoryId
                )
            }
        }
    }

    val transactionItemsLiveData = addDetailedTransactionRepository.getDraft()
    val transactionTotalLiveData: LiveData<BigDecimal> = transactionItemsLiveData.map { items ->
        items.first.items.map {
            it.amount ?: BigDecimal.ZERO
        }.reduceOrNull { acc, bd -> acc.plus(bd) }?.setScale(2, RoundingMode.HALF_UP)
            ?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    fun addItem(): Boolean {
        return addDetailedTransactionRepository.addItem()
    }

    fun onItemChanged(position: Int, item: AddTransactionItem) {
        addDetailedTransactionRepository.changeItem(position, item)
    }

    fun addItemFile(returnedUri: Uri): LiveData<Unit> {
        val mimeType = application.contentResolver.getType(returnedUri) ?: ""
        var inputStream = application.contentResolver.openInputStream(returnedUri)!!
        val checksumSingle = getSha256(inputStream).doOnSuccess { inputStream.close() }
        return checksumSingle.concatMap { checksum ->
            addDetailedTransactionRepository.hashInDb(checksum).map { hashInDb ->
                if (hashInDb) {
                    val existingDraftImage =
                        addDetailedTransactionRepository.getDbImageUri(checksum)
                    addDetailedTransactionRepository.addImageToItem(
                        getImageItemId,
                        existingDraftImage,
                        checksum
                    )
                } else if (addDetailedTransactionRepository.hashInDraft(checksum)) {
                    val existingDraftImage =
                        addDetailedTransactionRepository.getDraftImageUri(checksum)
                    addDetailedTransactionRepository.addImageToItem(
                        getImageItemId,
                        existingDraftImage,
                        checksum
                    )
                } else {
                    val root = File(application.filesDir, Constants.FOLDER_NAME_DRAFT)
                    val imagesFolder = File(root, Constants.SUBFOLDER_NAME_IMAGES)
                    imagesFolder.mkdirs()
                    val filename = UUID.randomUUID().toString()
                    val extension = when (mimeType) {
                        "image/jpeg" -> ".jpg"
                        "image/png" -> ".png"
                        else -> ""
                    }
                    inputStream = application.contentResolver.openInputStream(returnedUri)!!
                    val file = File(imagesFolder, "$filename$extension")
                    val outputStream = file.outputStream()
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    addDetailedTransactionRepository.addImageToItem(
                        getImageItemId,
                        file.toUri(),
                        checksum
                    )
                }
            }.subscribeOn(Schedulers.io())
        }.subscribeOn(Schedulers.io()).map {
            getImageItemId = -1
        }.subscribeOn(Schedulers.io()).toFlowable().toLiveData()
    }

    fun onItemDeleted(position: Int) {
        addDetailedTransactionRepository.deleteItem(position)
    }

    fun finishDraft(): LiveData<Unit> {
        return addDetailedTransactionRepository.finishTransaction().toFlowable().toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle()
    }

    fun validateDraft(): Boolean {
        val draft = addDetailedTransactionRepository.getDraftValue()
        val items = draft.items
        var valid = true
        for (item in items) {
            if (item.amount == null) {
                valid = false
                break
            }
            if (item.amount.compareTo(BigDecimal.ZERO) == 0) {
                valid = false
                break
            }
            if (item.description == null) {
                valid = false
                break
            }
            if (item.description.isBlank()) {
                valid = false
                break
            }
        }
        return valid
    }
}