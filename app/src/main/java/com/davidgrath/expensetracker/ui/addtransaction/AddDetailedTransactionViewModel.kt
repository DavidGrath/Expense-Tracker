package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.logging.Handler

class AddDetailedTransactionViewModel(private val application: Application, private val repository: AddDetailedTransactionRepository): AndroidViewModel(application) {


    var getImageItemId = -1
    init {
        if(!repository.draftExists()) {
            repository.createDraft()
            repository.addItem()
        }
    }
    val transactionItemsLiveData = repository.getDraft()
    val transactionTotalLiveData : LiveData<BigDecimal> = transactionItemsLiveData.map { items -> items.first.items.map { it.amount?: BigDecimal.ZERO }.reduceOrNull { acc, bd -> acc.plus(bd) }?.setScale(2, RoundingMode.HALF_UP)?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) }

    fun addItem(): Boolean {
        return repository.addItem()
    }

    fun onItemChanged(position: Int, item: AddTransactionItem) {
        repository.changeItem(position, item)
    }

    fun addItemFile(returnedUri: Uri) {
//        viewModelScope.launch(Dispatchers.IO) {
        //TODO Threads and idling resources
            val mimeType = application.contentResolver.getType(returnedUri)?:""
            var inputStream = application.contentResolver.openInputStream(returnedUri)!!
            val checksum = getSha256(inputStream)
//            inputStream.close()
            if(repository.hashInDb(checksum)) {
                val existingDraftImage = repository.getDbImageUri(checksum)
                repository.addImageToItem(getImageItemId, existingDraftImage, checksum)
            } else if(repository.hashInDraft(checksum)) {
                val existingDraftImage = repository.getDraftImageUri(checksum)
                repository.addImageToItem(getImageItemId, existingDraftImage, checksum)
            } else {
                val root = File(application.filesDir, Constants.FOLDER_NAME_DRAFT)
                val imagesFolder = File(root, Constants.SUBFOLDER_NAME_IMAGES)
                imagesFolder.mkdirs()
                val filename = UUID.randomUUID().toString()
                val extension = when(mimeType) {
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    else -> ""
                }
                inputStream = application.contentResolver.openInputStream(returnedUri)!!
                val file = File(imagesFolder, "$filename$extension")
                val outputStream = file.outputStream()
                inputStream.copyTo(outputStream)
//                inputStream.close()
                outputStream.close()
                repository.addImageToItem(getImageItemId, file.toUri(), checksum)
            }
            getImageItemId = -1
//        }
    }

    fun onItemDeleted(position: Int) {
        repository.deleteItem(position)
    }

    fun finishDraft() {
        repository.finishTransaction()
    }

}