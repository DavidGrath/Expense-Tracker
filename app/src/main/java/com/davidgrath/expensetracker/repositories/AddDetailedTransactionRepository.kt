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
import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import com.davidgrath.expensetracker.entities.ui.AddEditDetailedTransactionDraft
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import javax.inject.Inject
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.math.BigDecimal
import javax.inject.Singleton

@Singleton
class AddDetailedTransactionRepository
@Inject
constructor(
    private val fileHandler: DraftFileHandler, private val imageDao: ImageDao,
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val categoryDao: CategoryDao,
    private val itemImagesDao: TransactionItemImagesDao,
    private val clock: Clock,
    private val evidenceDao: EvidenceDao
) {

    private var incrementId = 0
    private var draft = AddEditDetailedTransactionDraft(emptyList())
    private val _draftLiveData = MutableLiveData<Triple<AddEditDetailedTransactionDraft, TransactionDetailEvent, Int>>(Triple(draft, TransactionDetailEvent.None, -1))
    private val draftLiveData: LiveData<Triple<AddEditDetailedTransactionDraft, TransactionDetailEvent, Int>> = _draftLiveData
    private var currentMode = "add"
    private var transactionId: Long? = null

    fun setMode(mode: String) {
        currentMode = mode
    }

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
        val draft = AddEditDetailedTransactionDraft(listOf(AddTransactionItem(incrementId++, null, categoryDbToCategoryUi(category), initialAmount, initialDescription)))
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
        fileHandler.saveDraft(draft).subscribe()
    }

    fun initializeEdit(transactionId: Long): Single<Unit> {
        this.transactionId = transactionId
        return transactionRepository.getTransactionByIdSingle(transactionId)
            .map { transaction ->
                val existingDraft = fileHandler.getDraft().blockingGet()?: AddEditDetailedTransactionDraft(emptyList())
                val transactionItems = transactionItemRepository.getTransactionItemsSingle(transactionId).blockingGet()
                draft = AddEditDetailedTransactionDraft(emptyList())
                //Set Items, incrementId
                val imagesMap = mutableMapOf<String, Uri>()
                incrementId = 0
                val items = transactionItems.map {
                    val category = categoryDao.findById(it.primaryCategoryId).blockingGet()!!
                    val images = imageDao.getAllByItemSingle(it.id!!).blockingGet()
                    val imagesDraft = images.map {
                        val uri = Uri.parse(it.uri)
                        if(imagesMap[it.sha256] == null) {
                            imagesMap[it.sha256] = uri
                        }
                        AddEditTransactionFile(it.id, uri, it.mimeType, it.sha256, it.sizeBytes, true)
                    }
                    AddTransactionItem(incrementId++, it.id!!, categoryDbToCategoryUi(category), it.amount, it.description, false, it.brand, imagesDraft)
                }
                draft = draft.copy(items, imageHashes = imagesMap + existingDraft.imageHashes)
                //Set Evidence
                val evidenceMap = mutableMapOf<String, Uri>()
                val evidenceList = evidenceDao.getAllByTransactionIdSingle(transactionId).blockingGet().map {
                    val uri = Uri.parse(it.uri)
                    if(evidenceMap[it.sha256] == null) {
                        evidenceMap[it.sha256] = uri
                    }
                    AddEditTransactionFile(it.id, uri, it.mimeType, it.sha256, it.sizeBytes, true)
                }
                draft = draft.copy(evidence = evidenceList, evidenceHashes = evidenceMap)
                //Set Note
                draft = draft.copy(note = transaction.note)
                //Set Account
                //Set Seller
                //Set Date
                //Set Time
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
            }.subscribeOn(Schedulers.io())
    }

    fun moveToTopOfDraft(initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        if(currentMode != "add") {
            return
        }
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
                val item = AddTransactionItem(incrementId, null, categoryDbToCategoryUi(category), initialAmount, initialDescription)
                val newItems = listOf(item) + items
                val newDraft = draft.copy(items = newItems)
                _draftLiveData.postValue(Triple(newDraft, TransactionDetailEvent.Insert, 0))
                fileHandler.saveDraft(newDraft).subscribe()
            }
            Log.i(LOG_TAG, "Moved initial details to top of existing draft")
        }.blockingGet()

    }

    fun addItem(): Boolean {
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            val category = categoryDao.findByStringId("miscellaneous")
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
            val maxId = currentList.maxOfOrNull { it.id }?: -1
            incrementId = maxId + 1
            val newItems = currentList + AddTransactionItem(incrementId++, null, categoryDbToCategoryUi(category))
            draft = draft.copy(items = newItems)
            _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Insert, currentList.size))
            if (currentMode == "add") {
                fileHandler.saveDraft(draft).subscribe()
            }
            return true
        }
        return false
    }

    fun deleteItem(position: Int) {
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) { //TODO Huh?
            val item = currentList[position]
            if(currentMode == "edit") {
                if(item.dbId != null) {
                    val existingDeleted = draft.deletedDbItems
                    draft = draft.copy(deletedDbItems = existingDeleted + item)
                }
            }
            val newItems = currentList.toMutableList().apply {
                removeAt(position)
            }
            draft = draft.copy(items = newItems)
            _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Delete, position))
            if (currentMode == "add") {
                fileHandler.saveDraft(draft).subscribe()
            }
        }
    }

    fun deleteItemImage(position: Int, imagePosition: Int) {
        val items = draft.items.toMutableList()
        var currentItem = items[position]
        val images = currentItem.images.toMutableList()
        val image = images[imagePosition]
        if(currentMode == "edit") {
            if(currentItem.dbId != null) {
                val currentDeletedImages = currentItem.deletedDbImages.toMutableList()
                if(currentDeletedImages.find { it.dbId == image.dbId } == null) {
                    currentDeletedImages += image
                    currentItem = currentItem.copy(deletedDbImages = currentDeletedImages)
                }
            }
        }
        images.removeAt(imagePosition)
        items[position] = currentItem.copy(images = images)
        draft = draft.copy(items = items)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Delete, position))
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    private fun imageHashInDb(sha256: String): Single<Boolean> {
        return imageDao.doesHashExist(sha256)
    }

    //TODO It should be okay to add the same evidence to the device for different transactions since
    // it's not likely that that will happen in the first place, unlike item images.
    // Rework this flow
    private fun evidenceHashInDb(transactionId: Long, sha256: String): Single<Boolean> {
        return evidenceDao.doesHashExist(sha256)
    }

    private fun getDbEvidenceByHash(transactionId: Long, sha256: String): Maybe<EvidenceDb> {
        return evidenceDao.findByTransactionIdAndSha256(transactionId, sha256)
    }

    private fun getDbImageByHash(sha256: String): Maybe<ImageDb> {
        return imageDao.findBySha256(sha256)
    }

    /**
     * Assumed to be called from Schedulers.io
     */
    private fun createImageForItem(item: AddTransactionItem, uriHash: String, uri: Uri?, externalUri: Uri, mimeType: String): Pair<List<AddEditTransactionFile>, Int> {
        var xUri = uri
        if(xUri == null) {
            val file: AddEditTransactionFile
            if(imageHashInDb(uriHash).blockingGet()) {
                Log.i(LOG_TAG,"File already exists in DB for image")
                val im = getDbImageByHash(uriHash).blockingGet()!!
                xUri = Uri.parse(im.uri)
                val size = xUri.toFile().length()
                file = AddEditTransactionFile(im.id, xUri, mimeType, uriHash, size)
            } else {
                xUri = fileHandler.copyUriToDraft(externalUri, mimeType, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val size = xUri.toFile().length()
                file = AddEditTransactionFile(null, xUri, mimeType, uriHash, size)
            }
            return ((item.images + file) to item.images.size)
        } else {
            val index = item.images.indexOfFirst { it.sha256 == uriHash}
            if(index == -1) {
                val size = xUri.toFile().length()
                return (item.images + AddEditTransactionFile(null, xUri, mimeType, uriHash, size)) to item.images.size
            } else {
                return ArrayList(item.images) to index
            }
        }
    }

    fun changeItem(position: Int, item: AddTransactionItem) {
        val currentList = draft.items
        val newItems = currentList.toMutableList().also {
            it[position] = item
        }
        draft = draft.copy(items = newItems)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.Change, position))
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }
    fun changeItemInvalidate(position: Int, item: AddTransactionItem) {
        val currentList = draft.items
        val newItems = currentList.toMutableList().also {
            it[position] = item
        }
        draft = draft.copy(items = newItems)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.ChangeInvalidate, position))
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun addImageToItem(itemId: Int, externalUri: Uri, mimeType: String): Single<Unit> {
        return Single.fromCallable {
            val currentList = draft.items
            
            val item = currentList.find { it.id == itemId }
            if (item == null) {
                return@fromCallable Unit
            }

            val uriHash = fileHandler.getFileHash(externalUri).blockingGet()
            val image = item.images.find { it.sha256 == uriHash }
            if (image != null) {
                return@fromCallable Unit
            }
//            if (item.images.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM) {
//                return
//            }
            val index = currentList.indexOf(item)
            var newItem = item
            val uri: Uri
            if (currentMode == "edit") {
                if (item.dbId != null) {
                    var deletedItemImages = item.deletedDbImages
                    val idx = deletedItemImages.indexOfFirst { it.sha256 == uriHash }
                    if (idx != -1) {
                        val mutableList = deletedItemImages.toMutableList()
                        val im = mutableList.removeAt(idx)
                        newItem =
                            item.copy(images = item.images + im, deletedDbImages = mutableList)
                        uri = im.uri
                    } else {
                        var xUri = draft.imageHashes[uriHash]
                        val (list, idex) = createImageForItem(item, uriHash, xUri, externalUri, mimeType)
                        uri = list[idex].uri
                        newItem = item.copy(images = list)
                    }
                } else {
                    var xUri = draft.imageHashes[uriHash]
                    val (list, idx) = createImageForItem(item, uriHash, xUri, externalUri, mimeType)
                    uri = list[idx].uri
                    newItem = item.copy(images = list)
                }

            } else {
                var xUri = draft.imageHashes[uriHash]
                val (list, idx) = createImageForItem(item, uriHash, xUri, externalUri, mimeType)
                uri = list[idx].uri
                newItem = item.copy(images = list)
            }
            val newItems = currentList.toMutableList().also {
                it[index] = newItem
            }
            val existingUri = draft.imageHashes[uriHash]
            val newHashes = if (existingUri == null) {
                draft.imageHashes + (uriHash to uri)
            } else {
                draft.imageHashes
            }
            draft = draft.copy(items = newItems, imageHashes = newHashes)
            
            _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.ChangeInvalidate, index))
            if (currentMode == "add") {
                fileHandler.saveDraft(draft).subscribe()
            }
        }
    }

    /**
     * @return The Uri for an existing or newly created local file
     */
    fun addEvidence(externalUri: Uri, mimeType: String): Single<Uri> {
        return Single.fromCallable {
            val currentList = draft.evidence
            val uris = currentList.map { it.uri }
            val newEvidence = currentList.toMutableList()
            val newHashes = draft.evidenceHashes.toMutableMap()

            val uriHash = fileHandler.getFileHash(externalUri).blockingGet()
            if(currentMode == "edit") {
                val deletedList = draft.deletedDbEvidence.toMutableList()
                val deletedEvidencePosition = deletedList.indexOfFirst { it.sha256 == uriHash }
                val uri: Uri
                if (deletedEvidencePosition != -1) {
                    val ev = deletedList.removeAt(deletedEvidencePosition)
                    newEvidence += ev
                    draft = draft.copy(deletedDbEvidence = deletedList)
                    uri = ev.uri
                } else {
                    var xUri = draft.evidenceHashes[uriHash]
                    if (xUri == null) {
                        val file: AddEditTransactionFile
                        if(evidenceHashInDb(transactionId!!, uriHash).blockingGet()) {
                            Log.i(LOG_TAG,"File already exists in DB for evidence")
                            val evidence = getDbEvidenceByHash(transactionId!!, uriHash).blockingGet()!!
                            xUri = Uri.parse(evidence.uri)
                            val size = xUri.toFile().length()
                            file = AddEditTransactionFile(evidence.id, xUri, mimeType, uriHash, size, true)
                        } else {
                            // val subFolder = file(Constants.SUBFOLDER_NAME_DOCUMENTS, year, month, day) //On the chance that someone edits a draft across more than one day, this might cause problems
                            xUri = fileHandler.copyUriToDraft(externalUri, mimeType, Constants.SUBFOLDER_NAME_DOCUMENTS).blockingGet()
                            val size = xUri.toFile().length()
                            file = AddEditTransactionFile(null, xUri, mimeType, uriHash, size)
                        }
                        newEvidence += file
                        newHashes += (file.sha256 to xUri!!)
                    }
                    uri = xUri!!
                }
                draft = draft.copy(evidenceHashes = newHashes, evidence = newEvidence)
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.None, -1))
                if (currentMode == "add") {
                    fileHandler.saveDraft(draft).subscribe()
                }
                return@fromCallable uri
            } else {
                var uri = newHashes[uriHash]
                if (uri == null) {
                    // val subFolder = file(Constants.SUBFOLDER_NAME_DOCUMENTS, year, month, day) //On the chance that someone edits a draft across more than one day, this might cause problems
                    uri = fileHandler.copyUriToDraft(externalUri, mimeType, Constants.SUBFOLDER_NAME_DOCUMENTS).blockingGet()
                    val size = uri.toFile().length()
                    val file = AddEditTransactionFile(null, uri, mimeType, uriHash, size)
                    
                    newEvidence += file
                    newHashes += (uriHash to uri)
                } else {
                    Log.i(LOG_TAG, "File already exists in Draft for evidence")
                }
                draft = draft.copy(evidenceHashes = newHashes, evidence = newEvidence)
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.None, -1))
                if (currentMode == "add") {
                    fileHandler.saveDraft(draft).subscribe()
                }
                return@fromCallable uri
            }

        }.subscribeOn(Schedulers.io())
    }

    fun removeEvidence(position: Int) {
        val evidenceList = draft.evidence.toMutableList()
        if(position >= evidenceList.size) {
            return
        }
        val evidence = evidenceList.removeAt(position)
        if(currentMode == "edit") {
            if(evidence.dbId != null) {
                val currentDeletedEvidence = draft.deletedDbEvidence.toMutableList()
                if(currentDeletedEvidence.find { it.dbId == evidence.dbId } == null) {
                    currentDeletedEvidence += evidence
                    draft = draft.copy(deletedDbEvidence = currentDeletedEvidence)
                }
            }
        }
        draft = draft.copy(evidence = evidenceList)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.None, -1))
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun getDraft(): LiveData<Triple<AddEditDetailedTransactionDraft, TransactionDetailEvent, Int>> {
        return draftLiveData
    }

    fun draftExists(): Boolean {
        return fileHandler.draftExists()
    }
    fun createDraft(): Single<Boolean> {
        return fileHandler.createDraft()
            .flatMap {  bool ->
                fileHandler.getDraft().toSingle().map { draft ->
                    this.draft = draft
                    _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
                    bool
                }
            }
    }

    fun restoreDraft(): Single<Unit> {
        return fileHandler.getDraft().toSingle()
            .map { draft ->
                //Account for potential changes to images made with previous edits
                val imageHashes = draft.imageHashes
                val newImageHashes = imageHashes.toMutableMap()
                val idMap = mutableMapOf<String, Long>()
                for((hash, _) in imageHashes) {
                    val image = getDbImageByHash(hash).blockingGet()
                    if(image != null) {
                        newImageHashes[hash] = Uri.parse(image.uri)
                        idMap[hash] = image.id!!
                    }
                }
                val newItems = draft.items.toMutableList()
                draft.items.forEachIndexed { i, item ->
                    val newImages = item.images.toMutableList()
                    item.images.forEachIndexed { j, image ->
                        val id = idMap[image.sha256]
                        if(id != null) {
                            newImages[j] = image.copy(dbId = id, uri = newImageHashes[image.sha256]!!)
                        }
                    }
                    newItems[i] = item.copy(images = newImages)
                }

                val newDraft = draft.copy(items = newItems, imageHashes = newImageHashes)
                this.draft = newDraft
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
            }
    }

    fun getDraftValue(): AddEditDetailedTransactionDraft {
        return draft
    }

    fun finishTransaction(): Single<Unit> {
        if(!validateDraft()) {
            
            return Single.just(Unit).doOnTerminate { setNote("add"); transactionId = null }
        }
        if(currentMode == "add") {
            return saveDraft().doOnTerminate { transactionId = null }
        } else if(currentMode == "edit"){
            return saveEdit().doOnTerminate { setNote("add"); transactionId = null }
        } else {
            return Single.just(Unit).doOnTerminate { setNote("add"); transactionId = null }
        }
    }

    private fun saveDraft(): Single<Unit> {
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

            val actuallyUsedNewImages = draft.items.map { it.images }.fold(emptyList<AddEditTransactionFile>()) { acc, list -> acc + list }.filter { it.dbId == null }.toSet()
            val imagesMap = mutableMapOf<String, Long>()
            for(image in actuallyUsedNewImages) {
                if(imagesMap[image.sha256] != null) {
                    continue
                }
                val file = image.uri.toFile()
                val hash = image.sha256
                val mimeType = image.mimeType
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val imageDb = ImageDb(null, length, hash, mimeType, finalUri.toString(), dateTimeString, offset, zone)
                val imageId = imageDao.insertImage(imageDb).blockingGet()
                imagesMap[image.sha256] = imageId
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
                    val localDate = LocalDate.now(clock)
                    val year = String.format("%04d", localDate.year)
                    val month = String.format("%02d", localDate.monthValue)
                    val day = String.format("%02d", localDate.dayOfMonth)
                    val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_DOCUMENTS + File.separator + year + File.separator + month + File.separator + day).blockingGet()
                    val finalUri = mainFile.toUri()

                    val length = mainFile.length()
                    val evidence = EvidenceDb(null, id, length, evidence.sha256, evidence.mimeType, finalUri.toString(), dateTimeString, offset, zone)
                    evidenceDao.insertEvidence(evidence).blockingGet()
                }

                val singles = draft.items.map { draftItem ->
                    val item = TransactionItemDb(null, id, draftItem.amount!!, draftItem.brand, 1, draftItem.description!!, "", "", draftItem.category.id, dateTimeString, offset, zone)
                    transactionItemRepository.addTransactionItem(item)
                        .flatMap {  itemId ->
                            val imageSingles = draftItem.images.map { image ->
                                val imageId = imagesMap[image.sha256]?: image.dbId!! //TODO When might this null assertion crash?
                                val itemImage = TransactionItemImagesDb(null, itemId, imageId, dateTimeString, offset, zone)
                                itemImagesDao.insertItemImage(itemImage)
                            }
                            Single.mergeDelayError(imageSingles).toList()
                        }
                }
                Single.mergeDelayError(singles).toList()
            }.flatMap {
                incrementId = 0
                draft = AddEditDetailedTransactionDraft(emptyList())
                _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
                fileHandler.deleteDraftFiles()
            }.blockingGet()
            Unit
        }.subscribeOn(Schedulers.io())
    }

    private fun saveEdit(): Single<Unit> {
        return Single.fromCallable {
            val imageToItemsMap = mutableMapOf<String, Set<Long>>()

            for (item in draft.deletedDbItems) {
                for (image in item.images) {
                    val set = imageToItemsMap[image.sha256] ?: emptySet()
                    if(image.dbIsLinked) {
                        imageToItemsMap[image.sha256] = set + item.dbId!!
                    }
                }
                for (image in item.deletedDbImages) {
                    val set = imageToItemsMap[image.sha256] ?: emptySet()
                    if(image.dbIsLinked) {
                        imageToItemsMap[image.sha256] = set + item.dbId!!
                    }
                }
            }



            
            
            val (dateTime, offset, zone) = dateTimeOffsetZone(clock)

            val actuallyUsedNewImages = draft.items.map { it.images }.fold(emptyList<AddEditTransactionFile>()) { acc, list -> acc + list }.filter { it.dbId == null }.toSet()
            val imagesMap = mutableMapOf<String, Long>()
            for(image in actuallyUsedNewImages) {
//                if(imagesMap[image.sha256] != null) { //?
//                    continue
//                }
                val file = image.uri.toFile()
                val hash = image.sha256
                val mimeType = image.mimeType
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val imageDb = ImageDb(null, length, hash, mimeType, finalUri.toString(), dateTime, offset, zone)
                val imageId = imageDao.insertImage(imageDb).blockingGet()
                imagesMap[image.sha256] = imageId
            }

            for(item in draft.items) {
                val itemId = if(item.dbId != null) {
                    item.dbId
                    //TODO I forgot to update existing items. Fix in next commit
                } else {
                    val itemDb = TransactionItemDb(
                        null,
                        transactionId!!,
                        item.amount!!,
                        item.brand,
                        1,
                        item.description!!,
                        "",
                        "",
                        item.category.id,
                        dateTime,
                        offset,
                        zone
                    )
                    transactionItemRepository.addTransactionItem(itemDb).blockingGet()
                }
                for (image in item.images) {

                    val set = imageToItemsMap[image.sha256] ?: emptySet()
                    imageToItemsMap[image.sha256] = set + itemId

                    val imageId = if (image.dbId == null) {
                        imagesMap[image.sha256]!!
                    } else {
                        image.dbId
                    }
                    if(image.dbIsLinked) {
                        continue
                    }
                    val itemImage = TransactionItemImagesDb(
                        null,
                        itemId,
                        imageId,
                        dateTime,
                        offset,
                        zone
                    )
                    itemImagesDao.insertItemImage(itemImage).blockingGet()

                }
            }
            for(item in draft.items) {
                
                for(image in item.deletedDbImages) {
                    var set = imageToItemsMap[image.sha256] ?: emptySet()
                    if (image.dbIsLinked) {
                        set -= item.dbId!!
                        imageToItemsMap[image.sha256] = set
                        itemImagesDao.deleteByItemAndImageId(item.dbId!!, image.dbId!!)
                            .blockingSubscribe()
                    }
                        
                        val imageAttachedToOtherImagesInSameTransaction = set.isNotEmpty()
                        val imageAttachedToOtherImagesOutsideTransaction =
                            itemImagesDao.countItemImagesForOtherTransactions(transactionId!!)
                                .blockingGet() > 0
                        
                        if (!imageAttachedToOtherImagesInSameTransaction && !imageAttachedToOtherImagesOutsideTransaction) {
                            //TODO handle Glide resource release first
                            val file = image.uri.toFile()
                            if (file.delete()) {
                                Log.i(
                                    LOG_TAG,
                                    "File $file no longer needed. Deleted"
                                )
                                
                            } else {
                                Log.i(
                                    LOG_TAG,
                                    "File $file no longer needed, but failed to delete"
                                )
                                
                            }
                            imageDao.deleteByIdSingle(image.dbId!!).blockingSubscribe()
                        }


                }

            }

            for(item in draft.deletedDbItems) {
                for(image in item.images) {
                    if(image.dbId != null) {
                        var set = imageToItemsMap[image.sha256] ?: emptySet()
                        if(image.dbIsLinked) {

                            set -= item.dbId!!
                            imageToItemsMap[image.sha256] = set
                            itemImagesDao.deleteByItemAndImageId(item.dbId!!, image.dbId!!)
                                .blockingSubscribe()
                        }
                            val imageAttachedToOtherImagesInSameTransaction = set.isNotEmpty()
                            val imageAttachedToOtherImagesOutsideTransaction =
                                itemImagesDao.countItemImagesForOtherTransactions(transactionId!!)
                                    .blockingGet() > 0
                            if (!imageAttachedToOtherImagesInSameTransaction && !imageAttachedToOtherImagesOutsideTransaction) {
                                //TODO handle Glide resource release first
                                val file = image.uri.toFile()
                                if (file.delete()) {
                                    Log.i(
                                        LOG_TAG,
                                        "File $file no longer needed. Deleted"
                                    )
                                    
                                } else {
                                    Log.i(
                                        LOG_TAG,
                                        "File $file no longer needed, but failed to delete"
                                    )
                                    
                                }
                                imageDao.deleteByIdSingle(image.dbId!!).blockingSubscribe()
                            }

                    }
                }
            }
            for(evidence in draft.deletedDbEvidence) {
                val file = evidence.uri.toFile()
                if (file.delete()) {
                    Log.i(LOG_TAG, "File $file no longer needed. Deleted")
                    
                } else {
                    Log.i(LOG_TAG, "File $file no longer needed, but failed to delete")
                    
                }
                evidenceDao.deleteByIdSingle(evidence.dbId!!).blockingSubscribe()
            }
            /*for(evidence in draft.evidence) {
//                if(imagesMap[image.sha256] != null) { //?
//                    continue
//                }
                val file = evidence.uri.toFile()
                val localDate = LocalDate.now(clock)
                val year = String.format("%04d", localDate.year)
                val month = String.format("%02d", localDate.monthValue)
                val day = String.format("%02d", localDate.dayOfMonth)
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_DOCUMENTS + File.separator + year + File.separator + month + File.separator + day).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val evidence = EvidenceDb(null, transactionId!!, length, evidence.sha256, evidence.mimeType, finalUri.toString(), dateTime, offset, zone)
                evidenceDao.insertEvidence(evidence).blockingGet()
            }*/
        }.subscribeOn(Schedulers.io())
    }

    fun deleteDraft() {
        incrementId = 0
        if(currentMode == "add") {
            draft = AddEditDetailedTransactionDraft(emptyList())
            _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.All, -1))
            fileHandler.deleteDraftFiles().blockingGet()
        } else if(currentMode == "edit") {

        }
    }

    fun setNote(note: String) {
        draft = draft.copy(note = note)
        _draftLiveData.postValue(Triple(draft, TransactionDetailEvent.None, -1))
        //TODO Debounce
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun validateDraft(): Boolean {
        val items = draft.items
        var valid = true
        if(items.isEmpty()) {
            Log.i(LOG_TAG, "Draft is empty, nothing to save")
            return false
        }
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

    enum class TransactionDetailEvent {
        Delete,
        Insert,
        All,
        Change,
        ChangeInvalidate,
        None
    }
    
    companion object {
        const val LOG_TAG = "AddDetTransRepository"
    }
}