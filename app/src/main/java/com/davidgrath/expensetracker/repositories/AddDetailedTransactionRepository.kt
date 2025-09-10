package com.davidgrath.expensetracker.repositories

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.EvidenceDb
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
import javax.inject.Singleton

@Singleton
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

    private var incrementId = 0
    private var draft = AddDetailedTransactionDraft(emptyList())
    private val _draftLiveData = MutableLiveData<Triple<AddDetailedTransactionDraft, TransactionDetailEvent, Int>>(Triple(draft, TransactionDetailEvent.None, -1))
    private val draftLiveData: LiveData<Triple<AddDetailedTransactionDraft, TransactionDetailEvent, Int>> = _draftLiveData

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
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
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
        fileHandler.getDraft().map { draft ->
            val items = draft.items
            val maxId = items.map { it.id }.maxOrNull()
            if(maxId != null) {
                incrementId = maxId + 1
                val item = AddTransactionItem(incrementId, categoryDbToCategoryUi(category), initialAmount, initialDescription)
                val newItems = listOf(item) + items
                val newDraft = draft.copy(items = newItems)
                _draftLiveData.postValue(Triple(newDraft, TransactionDetailEvent.Insert, 0))
                fileHandler.saveDraft(newDraft)
            }
            Log.i("AddDetTransRepo", "Moved initial details to top of existing draft")
        }.blockingGet()

    }

    fun addItem(): Boolean {
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            val category = categoryDao.findByStringId("miscellaneous")
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
            val newItems = currentList + AddTransactionItem(++incrementId, categoryDbToCategoryUi(category))
            draft = draft.copy(items = newItems)
            _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Insert, currentList.size))
            fileHandler.saveDraft(draft)
            return true
        }
        return false
    }

    fun deleteItem(position: Int) {
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            val newItems = currentList.toMutableList().apply {
                removeAt(position)
            }
            draft = draft.copy(items = newItems)
            _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Delete, position))
            fileHandler.saveDraft(draft)
        }
    }

    fun imageHashInDb(sha256: String): Single<Boolean> {
        return imageDao.doesHashExist(sha256)
    }

    fun imageHashInDraft(sha256: String): Boolean {
        val draftImageHashes = draft.imageHashes.values
        return (sha256 in draftImageHashes)
    }

    fun evidenceHashInDb(sha256: String): Single<Boolean> {
        return evidenceDao.doesHashExist(sha256)
    }

    fun evidenceHashInDraft(sha256: String): Boolean {
        val draftEvidenceHashes = draft.evidenceHashes.values
        return (sha256 in draftEvidenceHashes)
    }

    fun getDbEvidenceUri(sha256: String): Uri {
        return Uri.parse(evidenceDao.findBySha256(sha256).blockingGet()!!.uri)
    }

    fun getDraftEvidenceUri(sha256: String): Uri {
        val draftEvidenceMap = draft.evidenceHashes
        val key = draftEvidenceMap.keys.find { draftEvidenceMap[it] == sha256 }!!
        return key
    }

    fun getDraftImageUri(sha256: String): Uri {
        val draftImageMap = draft.imageHashes
        val key = draftImageMap.keys.find { draftImageMap[it] == sha256 }!!
        return key
    }

    fun getDbImageUri(sha256: String): Uri {
        return Uri.parse(imageDao.findBySha256(sha256).blockingGet()!!.uri)
    }

    fun changeItem(position: Int, item: AddTransactionItem) {
        val currentList = draft.items
        val newItems = currentList.toMutableList().also {
            it[position] = item
        }
        draft = draft.copy(items = newItems)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Change, position))
        fileHandler.saveDraft(draft)
    }

    fun addImageToItem(itemId: Int, localUri: Uri, sha256: String) {
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
        val existingHash = draft.imageHashes[localUri]
        val newHashes =  if(existingHash == null) {
            draft.imageHashes + (localUri to sha256)
        } else {
            draft.imageHashes
        }
        draft = draft.copy(items = newItems, imageHashes = newHashes)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.ChangeInvalidate, index))
        fileHandler.saveDraft(draft)
    }

    fun addEvidence(localUri: Uri, sha256: String, mimeType: String): Int {
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
        val existingHash = draft.evidenceHashes[localUri]
        val newHashes =  if(existingHash == null) {
            draft.evidenceHashes + (localUri to sha256)
        } else {
            draft.evidenceHashes
        }
        draft = draft.copy(evidenceHashes = newHashes, evidence = newEvidence)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.None, -1))
        fileHandler.saveDraft(draft)
        return position
    }

    fun getDraft(): LiveData<Triple<AddDetailedTransactionDraft, TransactionDetailEvent, Int>> {
        return draftLiveData
    }

    fun draftExists(): Boolean {
        return fileHandler.draftExists()
    }
    fun createDraft(): Single<Boolean> {
        return fileHandler.createDraft()
            .flatMap {  bool ->
                fileHandler.getDraft().map { draft ->
                    this.draft = draft
                    _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
                    bool
                }
            }
    }

    fun restoreDraft(): Single<Unit> {
        return fileHandler.getDraft()
            .map { draft ->
                this.draft = draft
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
            }
    }

    fun getDraftValue(): AddDetailedTransactionDraft {
        return draft
    }

    fun finishTransaction(): Single<Unit> {
        return Single.fromCallable {
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
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val image = ImageDb(null, length, hash, mimeType, finalUri.toString(), dateTimeString, offset, zone)
                val imageId = imageDao.insertImage(image).blockingGet()
                imagesMap[k] = imageId
            }
            val note = if(draft.note.isNullOrBlank()) {
                null
            } else {
                draft.note
            }
            val transaction = TransactionDb(null, 1, total, "USD", null, false, true, note, null, null, dateTimeString, offset, zone, 0, dateString, timeString, offset, zone)

            transactionRepository.addTransaction(transaction).flatMap { id ->
                draft.evidence.map { evidence ->
                    val file = evidence.uri.toFile()
                    val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_DOCUMENTS).blockingGet()
                    val finalUri = mainFile.toUri()

                    val length = mainFile.length()
                    val evidence = EvidenceDb(null, id, length, evidence.sha256, evidence.mimeType, finalUri.toString(), dateTimeString, offset, zone)
                    evidenceDao.insertEvidence(evidence).blockingGet()
                }

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
            }.flatMap {
                draft = AddDetailedTransactionDraft(emptyList())
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
                fileHandler.deleteDraft()
            }.blockingGet()
            Unit
        }.subscribeOn(Schedulers.io())
    }

    fun deleteDraft() {
        draft = AddDetailedTransactionDraft(emptyList())
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
        fileHandler.deleteDraft().blockingGet()
    }

    fun setNote(note: String) {
        draft = draft.copy(note = note)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.None, -1))
        //TODO Debounce
        fileHandler.saveDraft(draft)
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