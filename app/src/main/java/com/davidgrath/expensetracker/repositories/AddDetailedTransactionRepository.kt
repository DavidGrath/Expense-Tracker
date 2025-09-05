package com.davidgrath.expensetracker.repositories

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddTransactionEvidence
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.Clock
import javax.inject.Inject
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal

class AddDetailedTransactionRepository
@Inject
constructor(
    private val fileHandler: DraftFileHandler, private val imageDao: ImageDao,
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao,
    private val itemImagesDao: TransactionItemImagesDao,
    private val clock: Clock,
    private val evidenceDao: EvidenceDao
) {

    //TODO Relying on this variable is probably a terrible way to work with a file
    private var currentEvent = TransactionDetailEvent.All
    private var currentItemPosition = -1
    private var incrementId = 0
    private val liveData = fileHandler.getDraft()

    fun initializeDraft(initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        val category = if(initialCategoryId != null) {
            categoryDao.findById(initialCategoryId)
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
        } else {
            categoryDao.findByStringId("miscellaneous")
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
        }
        val draft = AddDetailedTransactionDraft(listOf(AddTransactionItem(++incrementId, categoryDbToCategoryUi(category), initialAmount, initialDescription)))
        fileHandler.saveDraft(draft)
    }

    fun moveToTopOfDraft(initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        val category = if(initialCategoryId != null) {
            categoryDao.findById(initialCategoryId)
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
        } else {
            categoryDao.findByStringId("miscellaneous")
                .subscribeOn(Schedulers.io())
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
            currentItemPosition = currentList.size + 1
            val category = categoryDao.findByStringId("miscellaneous")
                .subscribeOn(Schedulers.io())
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
            currentItemPosition = currentList.size + 1
            val newItems = currentList.toMutableList().apply {
                removeAt(position)
            }
            fileHandler.saveDraft(draft.copy(items = newItems))
        }
    }

    fun imageHashInDb(sha256: String): Single<Boolean> {
        return imageDao.doesHashExist(sha256)
    }

    fun imageHashInDraft(sha256: String): Boolean {
        val draft = fileHandler.getDraftValue()
        val draftImageHashes = draft.imageHashes.values
        return (sha256 in draftImageHashes)
    }

    fun evidenceHashInDb(sha256: String): Single<Boolean> {
        return evidenceDao.doesHashExist(sha256)
    }

    fun evidenceHashInDraft(sha256: String): Boolean {
        val draft = fileHandler.getDraftValue()
        val draftEvidenceHashes = draft.evidenceHashes.values
        return (sha256 in draftEvidenceHashes)
    }

    fun getDbEvidenceUri(sha256: String): Uri {
        return Uri.parse(evidenceDao.findBySha256(sha256).blockingGet()!!.uri)
    }

    fun getDraftEvidenceUri(sha256: String): Uri {
        val draft = fileHandler.getDraftValue()
        val draftEvidenceMap = draft.evidenceHashes
        val key = draftEvidenceMap.keys.find { draftEvidenceMap[it] == sha256 }!!
        return key
    }

    fun getDraftImageUri(sha256: String): Uri {
        val draft = fileHandler.getDraftValue()
        val draftImageMap = draft.imageHashes
        val key = draftImageMap.keys.find { draftImageMap[it] == sha256 }!!
        return key
    }

    fun getDbImageUri(sha256: String): Uri {
        return Uri.parse(imageDao.findBySha256(sha256).blockingGet()!!.uri)
    }

    fun changeItem(position: Int, item: AddTransactionItem) {
        val draft = fileHandler.getDraftValue()
        val currentList = draft.items
        currentEvent = TransactionDetailEvent.Change
        currentItemPosition = position
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
        currentItemPosition = index
        val existingHash = draft.imageHashes[localUri]
        val newHashes =  if(existingHash == null) {
            draft.imageHashes + (localUri to sha256)
        } else {
            draft.imageHashes
        }
        fileHandler.saveDraft(draft.copy(items = newItems, imageHashes = newHashes))
    }

    fun addEvidence(localUri: Uri, sha256: String, mimeType: String): Int {
        val draft = fileHandler.getDraftValue()
        val currentList = draft.evidence
        val uris = currentList.map { it.uri }

        if(localUri in uris) {
            return uris.indexOf(localUri)
        }
        if(currentList.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_EVIDENCE) {
            return -1
        }

        val newEvidence = currentList + AddTransactionEvidence(localUri, mimeType, sha256)
        val position = newEvidence.size - 1
        currentEvent = TransactionDetailEvent.None
        currentItemPosition = -1
        val existingHash = draft.evidenceHashes[localUri]
        val newHashes =  if(existingHash == null) {
            draft.imageHashes + (localUri to sha256)
        } else {
            draft.imageHashes
        }
        fileHandler.saveDraft(draft.copy(evidenceHashes = newHashes, evidence = newEvidence))
        return position
    }

    fun getDraft(): LiveData<Triple<AddDetailedTransactionDraft, TransactionDetailEvent, Int>> {
        return fileHandler.getDraft().map {
            Triple(it, currentEvent, currentItemPosition)
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

    fun finishTransaction(): Single<Unit> {
        return Single.fromCallable {
            val draft = fileHandler.getDraftValue()
            val total = draft.items.map { it.amount!! }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            //TODO Dates, Times, and Ordinals
            val date = ZonedDateTime.now(clock)
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val timeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_TIME)

            val offset = date.offset.id
            val zone = date.zone.id

            val imagesMap = mutableMapOf<Uri, Long>()
            for((k,v) in draft.imageHashes) {

                val file = k.toFile()
                val hash = v
                val mimeType = "image/jpeg" //TODO Mime type fix
                val mainFile = fileHandler.moveFileToMain(file).blockingGet()
                val uri = mainFile.toUri()

                val length = mainFile.length()
                val image = ImageDb(null, length, hash, mimeType, uri.toString(), dateString, offset, zone)
                val imageId = imageDao.insertImage(image).blockingGet()
                imagesMap[k] = imageId
            }
            val transaction = TransactionDb(null, 0, total, "USD", false, dateTimeString, offset, zone, 0, dateString, timeString, offset, zone)

            transactionRepository.addTransaction(transaction).flatMap { id ->
                val singles = draft.items.map { draftItem ->
                    val item = TransactionItemDb(null, id, draftItem.amount!!, draftItem.brand, 1, draftItem.description!!, "", "", draftItem.category.id, dateTimeString, offset, zone)
                    transactionRepository.addTransactionItem(item)
                        .flatMap {  itemId ->
                            val imageSingles = draftItem.images.map { image ->
                                val uri = Uri.parse(image)
                                val imageId = imagesMap[uri]!!
                                val itemImage = TransactionItemImagesDb(null, itemId, imageId, dateTimeString, offset, zone)
                                itemImagesDao.insertItemImage(itemImage)
                            }
                            Single.mergeDelayError(imageSingles).toList()
                        }
                }
                Single.mergeDelayError(singles).toList()
            }.blockingGet()
            fileHandler.deleteDraft()
            Unit
        }.subscribeOn(Schedulers.io())
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