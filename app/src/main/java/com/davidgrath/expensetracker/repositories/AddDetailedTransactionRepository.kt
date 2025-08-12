package com.davidgrath.expensetracker.repositories

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.db.dao.TempCategoryDao
import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import io.reactivex.rxjava3.core.Single
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal

class AddDetailedTransactionRepository(private val fileHandler: DraftFileHandler, private val tempImagesDao: TempImagesDao, private val transactionRepository: TransactionRepository, private val tempCategoryDao: TempCategoryDao, private val tempItemImagesDao: TempTransactionItemImagesDao) {

    //TODO Relying on this variable is probably a terrible way to work with a FileObserver
    private var currentEvent = TransactionDetailEvent.All
    private var currentPosition = -1
    private var incrementId = 0
    private val liveData = fileHandler.getDraft()

    fun initializeDraft(initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        val category = if(initialCategoryId != null) {
            tempCategoryDao.getById(initialCategoryId)
//                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .blockingGet()
        } else {
            tempCategoryDao.findByStringId("miscellaneous")
//                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .blockingGet()!!
        }
        val draft = AddDetailedTransactionDraft(listOf(AddTransactionItem(++incrementId, categoryDbToCategoryUi(category), initialAmount, initialDescription)))
        fileHandler.saveDraft(draft)
    }

    fun moveToTopOfDraft(initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        val category = if(initialCategoryId != null) {
            tempCategoryDao.getById(initialCategoryId)
//                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .blockingGet()
        } else {
            tempCategoryDao.findByStringId("miscellaneous")
//                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .blockingGet()!!
        }
        val draft = fileHandler.getDraftValue()
        val items = draft.items
        val maxId = items.map { it.id }.maxOrNull()
        if(maxId != null) {
            incrementId = maxId + 1
            val item = AddTransactionItem(incrementId, categoryDbToCategoryUi(category), initialAmount, initialDescription)
            val newItems = listOf(item) + items
            val newDraft = draft.copy(items = newItems)
            fileHandler.saveDraft(newDraft)
        }
    }

    fun addItem(): Boolean {
        val draft = fileHandler.getDraftValue()
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            currentEvent = TransactionDetailEvent.Insert
            currentPosition = currentList.size + 1
            val category = tempCategoryDao.findByStringId("miscellaneous")
//                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .blockingGet()!!
            val newItems = currentList + AddTransactionItem(++incrementId, categoryDbToCategoryUi(category))
            fileHandler.saveDraft(draft.copy(items = newItems))
            return true
        }
        return false
    }

    fun deleteItem(position: Int) {
        val draft = fileHandler.getDraftValue()
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            currentEvent = TransactionDetailEvent.Delete
            currentPosition = currentList.size + 1
            val newItems = currentList.toMutableList().apply {
                removeAt(position)
            }
            fileHandler.saveDraft(draft.copy(items = newItems))
        }
    }

    fun hashInDb(sha256: String): Boolean {
        return tempImagesDao.doesHashExist(sha256)
    }

    fun hashInDraft(sha256: String): Boolean {
        val draft = fileHandler.getDraftValue()
        val draftImageHashes = draft.imageHashes.values
        return (sha256 in draftImageHashes)
    }

    fun getDraftImageUri(sha256: String): Uri {
        val draft = fileHandler.getDraftValue()
        val draftImageMap = draft.imageHashes
        val key = draftImageMap.keys.find { draftImageMap[it] == sha256 }!!
        return key
    }

    fun getDbImageUri(sha256: String): Uri {
        return Uri.parse(tempImagesDao.findBySha256(sha256).uri)
    }

    fun changeItem(position: Int, item: AddTransactionItem) {
        val draft = fileHandler.getDraftValue()
        val currentList = draft.items
        currentEvent = TransactionDetailEvent.Change
        currentPosition = position
        val newItems = currentList.toMutableList().also {
            it[position] = item
        }
        fileHandler.saveDraft(draft.copy(items = newItems))
    }

    fun addImageToItem(itemId: Int, localUri: Uri, sha256: String) {
        val draft = fileHandler.getDraftValue()
        val currentList = draft.items
        val item = currentList.find { it.id == itemId }
        if(item == null) {
            return
        }
        val uriString = localUri.toString()
        if(uriString in item.images) {
            return
        }
        if(item.images.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM) {
            return
        }
        val index = currentList.indexOf(item)
        val newItem = item.copy(images = item.images + uriString)
        val newItems = currentList.toMutableList().also {
            it[index] = newItem
        }
        currentEvent = TransactionDetailEvent.ChangeInvalidate
        currentPosition = index
        val existingHash = draft.imageHashes[localUri]
        val newHashes =  if(existingHash == null) {
            draft.imageHashes + (localUri to sha256)
        } else {
            draft.imageHashes
        }
        fileHandler.saveDraft(AddDetailedTransactionDraft(newItems, newHashes))
    }

    fun getDraft(): LiveData<Triple<AddDetailedTransactionDraft, TransactionDetailEvent, Int>> {
        return fileHandler.getDraft().map {
            Triple(it, currentEvent, currentPosition)
        }
    }

    fun draftExists(): Boolean {
        return fileHandler.draftExists()
    }
    fun createDraft(): Boolean {
        return fileHandler.createDraft()
    }

    fun getDraftValue(): AddDetailedTransactionDraft {
        return fileHandler.getDraftValue()
    }

    fun finishTransaction() {
        val draft = liveData.value!!
        val total = draft.items.map { it.amount!! }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        val date = ZonedDateTime.now()
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val offset = date.offset.id
        val zone = date.zone.id

        val imagesMap = mutableMapOf<Uri, Long>()
        for((k,v) in draft.imageHashes) {

            val file = k.toFile()
            val hash = v
            val mimeType = "image/jpeg" //TODO Mime type fix
            val mainFile = fileHandler.moveFileToMain(file)
            val uri = mainFile.toUri()

            val length = mainFile.length()
            val image = ImageDb(null, length, hash, mimeType, uri.toString(), dateString, offset, zone)
            val imageId = tempImagesDao.addImage(image).blockingGet()
            imagesMap[uri] = imageId
        }
        val transaction = TransactionDb(null, 0, total, "USD", false, dateString, offset, zone, dateString, offset, zone)

        transactionRepository.addTransaction(transaction).flatMap { id ->
            val singles = draft.items.map { draftItem ->
                val item = TransactionItemDb(null, id, draftItem.amount!!, draftItem.brand, 1, draftItem.description!!, "", "", draftItem.category.id, dateString, offset, zone)
                transactionRepository.addTransactionItem(item)
                    .map {  itemId ->
                        for(image in draftItem.images) {
                            val uri = Uri.parse(image)
                            val imageId = imagesMap[uri]!!
                            val itemImage = TransactionItemImagesDb(null, itemId, imageId, dateString, offset, zone)
                            tempItemImagesDao.addImage(itemImage)
                        }
                    }
            }
            Single.mergeDelayError(singles).toList()
        }.subscribe({},{})

        fileHandler.deleteDraft()
    }

    enum class TransactionDetailEvent {
        Delete,
        Insert,
        All,
        Change,
        ChangeInvalidate,
        None
    }
}