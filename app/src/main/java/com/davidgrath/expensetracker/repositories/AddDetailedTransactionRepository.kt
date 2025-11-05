package com.davidgrath.expensetracker.repositories

import android.net.Uri
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
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
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
import org.slf4j.LoggerFactory
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import javax.inject.Inject
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
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
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
    private val evidenceDao: EvidenceDao,
    private val transactionItemDao: TransactionItemDao,
    private val accountRepository: AccountRepository
) {

    private var incrementId = 0
    private var draft = AddEditDetailedTransactionDraft(emptyList(), -1)
    private val _draftLiveData = MutableLiveData<Triple<AddEditDetailedTransactionDraft, TransactionItemsEvent, Int>>(Triple(draft, TransactionItemsEvent.None, -1))
    private val draftLiveData: LiveData<Triple<AddEditDetailedTransactionDraft, TransactionItemsEvent, Int>> = _draftLiveData
    private var currentMode = "add"
    private var transactionId: Long? = null
    private var profileId: Long? = null

    fun setMode(mode: String) {
        currentMode = mode
        LOGGER.info("Current mode set to {}", mode)
    }

    fun setProfile(profileId: Long) {
        this.profileId = profileId
        LOGGER.info("Changed profileId")
    }

    fun initializeDraft(accountId: Long, initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        val category = if(initialCategoryId != null) {
            categoryDao.findById(initialCategoryId)
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
        } else {
            categoryDao.findByProfileIdAndStringId(profileId!!, "miscellaneous")
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
        }
        val draft = AddEditDetailedTransactionDraft(listOf(AddTransactionItem(incrementId++, null, categoryDbToCategoryUi(category), initialAmount, initialDescription)), accountId)
        LOGGER.info("Created draft with initial values")
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.All, -1))
        fileHandler.saveDraft(draft).subscribe()
    }

    fun initializeEdit(transactionId: Long): Single<Unit> {
        this.transactionId = transactionId
        return transactionRepository.getTransactionByIdSingle(transactionId)
            .map { transaction ->
                val existingDraft = fileHandler.getDraft().blockingGet()?: AddEditDetailedTransactionDraft(emptyList(), -1)
                val transactionItems = transactionItemRepository.getTransactionItemsSingle(transactionId).blockingGet()
                draft = AddEditDetailedTransactionDraft(emptyList(), transaction.accountId, transaction.debitOrCredit)
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
                    AddTransactionItem(incrementId++, it.id!!, categoryDbToCategoryUi(category),
                        it.amount, it.description, false, it.brand, it.variation, it.referenceNumber, it.quantity, it.isReduction, it.ordinal, imagesDraft)
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
                //Set Date and Time
                val localDate = LocalDate.parse(transaction.datedAt)
                val localTime = if(transaction.datedAtTime != null) {
                    LocalTime.parse(transaction.datedAtTime)
                } else {
                    null
                }

                draft = draft.copy(dbOriginalDate = localDate, dbOriginalTime = localTime) // TODO Think about ordinals later

                LOGGER.info("Initialized existing transaction {}", transactionId)
                _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.All, -1))
            }.subscribeOn(Schedulers.io())
    }

    fun moveToTopOfDraft(initialAmount: BigDecimal?, initialDescription: String?, initialCategoryId: Long?) {
        if(currentMode != "add") {
            LOGGER.info("moveToTopOfDraft: Mode not add. Doing nothing")
            return
        }
        val category = if(initialCategoryId != null) {
            categoryDao.findById(initialCategoryId)
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
        } else {
            categoryDao.findByProfileIdAndStringId(profileId!!, "miscellaneous")
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
                _draftLiveData.postValue(Triple(newDraft, TransactionItemsEvent.Insert, 0))
                fileHandler.saveDraft(newDraft).blockingSubscribe()
            }
            LOGGER.info("Moved initial details to top of existing draft")
        }.blockingGet()

    }

    fun addItem(): Boolean {
        val currentList = draft.items
        if(currentList.size + 1 <= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE) {
            val category = categoryDao.findByProfileIdAndStringId(profileId!!, "miscellaneous")
                .subscribeOn(Schedulers.io())
                .blockingGet()!!
            val maxId = currentList.maxOfOrNull { it.id }?: -1
            incrementId = maxId + 1
            val newItems = currentList + AddTransactionItem(incrementId++, null, categoryDbToCategoryUi(category))
            draft = draft.copy(items = newItems)
            LOGGER.info("addItem: New list size: {}", newItems.size)
            _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.Insert, currentList.size))
            if (currentMode == "add") {
                fileHandler.saveDraft(draft).subscribe()
            }
            return true
        }
        LOGGER.info("addItem: Items at max length. Doing nothing")
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
                    LOGGER.info("deleteItem: Moved existing DB item to deleted list")
                }
            }
            val newItems = currentList.toMutableList().apply {
                removeAt(position)
            }
            draft = draft.copy(items = newItems)
            _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.Delete, position))
            LOGGER.info("deleteItem: Deleted item at position {}", position)
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
                    LOGGER.info("deleteItemImage: Moved existing DB image to deleted list")
                }
            }
        }
        images.removeAt(imagePosition)
        items[position] = currentItem.copy(images = images)
        draft = draft.copy(items = items)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.Delete, position))
        LOGGER.info("Removed image at position {} from item {}", imagePosition, position)
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    //TODO It should be okay to add the same evidence to the device for different transactions since
    // it's not likely that that will happen in the first place, unlike item images.
    // Rework this flow
    private fun evidenceHashInDb(transactionId: Long, sha256: String): Single<Boolean> {
        return evidenceDao.doesHashExist(transactionId, sha256)
    }

    private fun getDbEvidenceByHash(transactionId: Long, sha256: String): Maybe<EvidenceDb> {
        return evidenceDao.findByTransactionIdAndSha256(transactionId, sha256)
    }


    /**
     * Creates a local image file for the `externalUri` if it doesn't exist and adds it to the item
     * if it wasn't already in the item's list.
     * Assumed to be called from Schedulers.io
     */
    private fun createImageForItem(item: AddTransactionItem, uriHash: String, fileUri: Uri?, externalUri: Uri, mimeType: String): Pair<List<AddEditTransactionFile>, Int> {
        var xUri = fileUri
        if(xUri == null) {
            val file: AddEditTransactionFile
            if(imageDao.doesHashExist(profileId!!, uriHash).blockingGet()) {
                LOGGER.info("createImageForItem: File already exists in DB for image")
                val im = imageDao.findBySha256(profileId!!, uriHash).blockingGet()!!
                xUri = Uri.parse(im.uri)
                val size = xUri.toFile().length()
                file = AddEditTransactionFile(im.id, xUri, mimeType, uriHash, size)
            } else {
                LOGGER.info("createImageForItem: File does not exist for image")
                xUri = fileHandler.copyUriToDraft(externalUri, mimeType, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val size = xUri.toFile().length()
                file = AddEditTransactionFile(null, xUri, mimeType, uriHash, size)
            }
            return ((item.images + file) to item.images.size)
        } else {
            val index = item.images.indexOfFirst { it.sha256 == uriHash }
            if(index == -1) {
                LOGGER.info("createImageForItem: Image already exists in draft. Adding to item")
                if(imageDao.doesHashExist(profileId!!, uriHash).blockingGet()) {
                    LOGGER.info("createImageForItem: File already exists in DB for image")
                    val im = imageDao.findBySha256(profileId!!, uriHash).blockingGet()!!
                    val size = xUri.toFile().length()
                    return (item.images + AddEditTransactionFile(im.id!!, xUri, mimeType, uriHash, size)) to item.images.size
                } else {
                    val size = xUri.toFile().length()
                    return (item.images + AddEditTransactionFile(null, xUri, mimeType, uriHash, size)) to item.images.size
                }
            } else {
                LOGGER.info("createImageForItem: Image already exists in draft and is attached to item")
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
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.Change, position))
        LOGGER.debug("changeItem: {}", item)
        LOGGER.info("Changed item at position {}", position)
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
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.ChangeInvalidate, position))
        LOGGER.info("Changed item at position {}", position)
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun addImageToItem(itemId: Int, externalUri: Uri, mimeType: String): Single<Unit> {
        return Single.fromCallable {
            val currentList = draft.items
            
            val item = currentList.find { it.id == itemId }
            if (item == null) {
                LOGGER.info("addImageToItem: Item {} not found", itemId)
                return@fromCallable Unit
            }

            val uriHash = fileHandler.getFileHash(externalUri).blockingGet()
            val image = item.images.find { it.sha256 == uriHash }
            if (image != null) {
                LOGGER.info("addImageToItem: Image already exists for item {}", itemId)
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
                        LOGGER.info("addImageToItem: DB Image was in item {} deleted list. Restored", itemId)
                    } else {
                        var xUri = draft.imageHashes[uriHash]
                        val (list, idex) = createImageForItem(item, uriHash, xUri, externalUri, mimeType)
                        uri = list[idex].uri
                        newItem = item.copy(images = list)
                        LOGGER.info("addImageToItem: DB Image added to item {} list", itemId)
                    }
                } else {
                    var xUri = draft.imageHashes[uriHash]
                    val (list, idx) = createImageForItem(item, uriHash, xUri, externalUri, mimeType)
                    uri = list[idx].uri
                    newItem = item.copy(images = list)
                    LOGGER.info("addImageToItem: New Image added to item {} list", itemId)
                }

            } else {
                var xUri = draft.imageHashes[uriHash]
                val (list, idx) = createImageForItem(item, uriHash, xUri, externalUri, mimeType)
                uri = list[idx].uri
                newItem = item.copy(images = list)
                LOGGER.info("addImageToItem: Image added to list of item with ID {}", itemId)
            }
            val newItems = currentList.toMutableList().also {
                it[index] = newItem
            }
            val existingUri = draft.imageHashes[uriHash]
            val newHashes = if (existingUri == null) {
                LOGGER.info("Image Uri added to HashMap")
                draft.imageHashes + (uriHash to uri)
            } else {
                draft.imageHashes
            }
            draft = draft.copy(items = newItems, imageHashes = newHashes)
            _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.ChangeInvalidate, index))
            LOGGER.info("addImageToItem: {}: Done", itemId)
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
                            LOGGER.info("addEvidence: Transaction {}: File already exists in DB for evidence", transactionId)
                            val evidence = getDbEvidenceByHash(transactionId!!, uriHash).blockingGet()!!
                            xUri = Uri.parse(evidence.uri)
                            val size = xUri.toFile().length()
                            file = AddEditTransactionFile(evidence.id, xUri, mimeType, uriHash, size, true)
                        } else {
                            // val subFolder = file(Constants.SUBFOLDER_NAME_DOCUMENTS, year, month, day) //On the chance that someone edits a draft across more than one day, this might cause problems
                            LOGGER.info("addEvidence: Transaction {}: File does not exist in DB for evidence", transactionId)
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
                _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
                LOGGER.info("addEvidence: Transaction {}: Done", transactionId)
                if (currentMode == "add") {
                    fileHandler.saveDraft(draft).subscribe()
                }
                return@fromCallable uri
            } else {
                var uri = newHashes[uriHash]
                if (uri == null) {
                    LOGGER.info("addEvidence: File does not exist in draft for evidence")
                    // val subFolder = file(Constants.SUBFOLDER_NAME_DOCUMENTS, year, month, day) //On the chance that someone edits a draft across more than one day, this might cause problems
                    uri = fileHandler.copyUriToDraft(externalUri, mimeType, Constants.SUBFOLDER_NAME_DOCUMENTS).blockingGet()
                    val size = uri.toFile().length()
                    val file = AddEditTransactionFile(null, uri, mimeType, uriHash, size)
                    
                    newEvidence += file
                    newHashes += (uriHash to uri)
                } else {
                    LOGGER.info("addEvidence: File already exists in draft for evidence")
                }
                draft = draft.copy(evidenceHashes = newHashes, evidence = newEvidence)
                _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
                LOGGER.info("addEvidence: Done")
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
        LOGGER.info("removeEvidence: Removed evidence at position {}", position)
        if(currentMode == "edit") {
            if(evidence.dbId != null) {
                val currentDeletedEvidence = draft.deletedDbEvidence.toMutableList()
                if(currentDeletedEvidence.find { it.dbId == evidence.dbId } == null) {
                    LOGGER.info("removeEvidence: Evidence exists in DB for transaction {}. Marking as deleted", transactionId)
                    currentDeletedEvidence += evidence
                    draft = draft.copy(deletedDbEvidence = currentDeletedEvidence)
                }
            }
        }
        draft = draft.copy(evidence = evidenceList)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
        LOGGER.info("removeEvidence: Done")
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun getDraft(): LiveData<Triple<AddEditDetailedTransactionDraft, TransactionItemsEvent, Int>> {
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
                    _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.All, -1))
                    bool
                }
            }
    }

    fun restoreDraft(): Single<Unit> {
        return fileHandler.getDraft().toSingle()
            .map { draft ->
                //Account for potential changes made to old accounts
                var accountId = draft.accountId
                val account = accountRepository.getAccountByIdSingle(accountId).blockingGet() //TODO Validate by profile ID, too
                if(account == null) {
                    LOGGER.info("restoreDraft: Account {} no longer exists, falling back to default", accountId)
                    val defaultAccount = accountRepository.getAccountsForProfileSingle(1).blockingGet()[0] //TODO This is a hack. Fix later
                    accountId = defaultAccount.id!!
                }
                //Account for potential changes to images made with previous edits
                val imageHashes = draft.imageHashes
                val newImageHashes = imageHashes.toMutableMap()
                val idMap = mutableMapOf<String, Long>()
                for((hash, _) in imageHashes) {
                    val image = imageDao.findBySha256(profileId!!, hash).blockingGet()
                    if(image != null) {
                        newImageHashes[hash] = Uri.parse(image.uri)
                        idMap[hash] = image.id!!
                    }
                }
                val count = idMap.size
                val newItems = draft.items.toMutableList()
                if(count != 0) {
                    LOGGER.info(
                        "restoreDraft: Found {} existing images that have been moved to the DB. Updating draft",
                        count
                    )
                    var total = 0
                    draft.items.forEachIndexed { i, item ->
                        val newImages = item.images.toMutableList()
                        item.images.forEachIndexed { j, image ->
                            val id = idMap[image.sha256]
                            if (id != null) {
                                total++
                                newImages[j] =
                                    image.copy(dbId = id, uri = newImageHashes[image.sha256]!!)
                            }
                        }
                        newItems[i] = item.copy(images = newImages)
                    }
                    LOGGER.info("restoreDraft: Updated {} total item images", total)
                }
                val newDraft = draft.copy(items = newItems, imageHashes = newImageHashes, accountId = accountId)
                this.draft = newDraft
                _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.All, -1))
                LOGGER.info("restoreDraft: Done")
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
            val total = draft.items.map {
                if(it.isReduction) {
                    it.amount!!.times(BigDecimal(-1))
                } else {
                    it.amount!!
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            //TODO Dates, Times, and Ordinals
            val (datedDate, datedTime) = customDateTimeForDraft(draft)
            val date = ZonedDateTime.now(timeAndLocaleHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
            val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

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
                val imageDb = ImageDb(null, profileId!!, length, hash, mimeType, finalUri.toString(), dateTimeString, offset, zone)
                val imageId = imageDao.insertImage(imageDb).blockingGet()
                imagesMap[image.sha256] = imageId
            }
            if(actuallyUsedNewImages.isNotEmpty()) {
                LOGGER.info("saveDraft: Saved {} new images", actuallyUsedNewImages.size)
            }
            val note = if(draft.note.isNullOrBlank()) {
                null
            } else {
                draft.note
            }
            LOGGER.debug("AccountID: {}, Draft: {}", draft.accountId, draft)
            val account = accountRepository.getAccountByIdSingle(draft.accountId).blockingGet()!!
            val maxOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(draft.accountId, datedDate).blockingGet()?: 0
            val ordinal = maxOrdinal + 1
            val transaction = TransactionDb(null, account.id!!, total, account.currencyCode, null, draft.debitOrCredit, draft.mode, note, null, null, dateTimeString, offset, zone, ordinal, datedDate, datedTime)

            transactionRepository.addTransaction(transaction).flatMap { id ->
                LOGGER.info("saveDraft: Created new transaction")
                draft.evidence.map { evidence ->
                    val file = evidence.uri.toFile()
                    val localDate = LocalDate.now(timeAndLocaleHandler.getClock())
                    val year = String.format("%04d", localDate.year)
                    val month = String.format("%02d", localDate.monthValue)
                    val day = String.format("%02d", localDate.dayOfMonth)
                    val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_DOCUMENTS + File.separator + year + File.separator + month + File.separator + day).blockingGet()
                    val finalUri = mainFile.toUri()

                    val length = mainFile.length()
                    val evidence = EvidenceDb(null, id, length, evidence.sha256, evidence.mimeType, finalUri.toString(), dateTimeString, offset, zone)
                    evidenceDao.insertEvidence(evidence).blockingGet()
                }
                if(draft.evidence.isNotEmpty()) {
                    LOGGER.info("saveDraft: Saved {} evidence to transaction", draft.evidence.size)
                }
                var itemOrdinal = 1
                val singles = draft.items.map { draftItem ->

                    val item = if(draftItem.isReduction) {
                        TransactionItemDb(null, id, draftItem.amount!!, null, 1,
                            draftItem.description!!, "", null,
                            draftItem.category.id, draftItem.isReduction, itemOrdinal++, dateTimeString, offset, zone)
                    } else {
                        TransactionItemDb(null, id, draftItem.amount!!, draftItem.brand, draftItem.quantity,
                            draftItem.description!!, draftItem.variation, draftItem.referenceNumber,
                            draftItem.category.id, draftItem.isReduction, itemOrdinal++, dateTimeString, offset, zone)
                    }
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
                LOGGER.info("saveDraft: Saved {} items to transaction", it.size)
                incrementId = 0
                draft = AddEditDetailedTransactionDraft(emptyList(), -1)
                _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.All, -1))
                fileHandler.deleteDraftFiles()
            }.blockingGet()
            Unit
        }.timeInterval().map {
            val time = it.time(TimeUnit.MILLISECONDS)
            LOGGER.info("saveDraft: took {} ms", time)
            it.value()
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

            
            val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())

            val actuallyUsedNewImages = draft.items.map { it.images }.fold(emptyList<AddEditTransactionFile>()) { acc, list -> acc + list }.filter { it.dbId == null }.toSet()
            val imagesMap = mutableMapOf<String, Long>()
            for(image in actuallyUsedNewImages) {
                LOGGER.debug("actuallyUsedNewImages: {}", actuallyUsedNewImages)
                if(imagesMap[image.sha256] != null) {
                    continue
                }
                val file = image.uri.toFile()
                val hash = image.sha256
                val mimeType = image.mimeType
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val imageDb = ImageDb(null, profileId!!, length, hash, mimeType, finalUri.toString(), dateTime, offset, zone)
                val imageId = imageDao.insertImage(imageDb).blockingGet()
                imagesMap[image.sha256] = imageId
            }
            LOGGER.info("saveEdit: Saved {} new images", imagesMap.size)

            var newCount = 0
            var updatedCount = 0
            var unchangedCount = 0

            var total  = BigDecimal.ZERO
            var maxOrdinal = ((draft.items.filter { it.dbId != null } + draft.deletedDbItems).map { it.ordinal }.maxOrNull()?: 0) + 1
            val ordinals = draft.items.filter { it.dbId != null }.map { it.ordinal }
            LOGGER.debug("ordinals: {}, maxOrdinal: {}", ordinals, maxOrdinal)
            for(item in draft.items) {
                val itemId: Long
                if(item.dbId != null) {
                    itemId = item.dbId
                    if(item.isReduction) {
                        val amountToAdd = item.amount!!.times(BigDecimal(-1))
                        total = total.plus(amountToAdd)
                        val result = transactionItemDao.updateTransactionItem(
                            item.dbId, item.amount!!, null, 1, item.description!!, item.category.id
                        ).blockingGet()
                        if(result > 0) {
                            updatedCount += result
                        } else {
                            unchangedCount += 1
                        }
                    } else {
                        val amountToAdd = item.amount!!

                        total = total.plus(amountToAdd)
                        val result = transactionItemDao.updateTransactionItem(
                            item.dbId,
                            item.amount!!,
                            item.brand,
                            1,
                            item.description!!,
                            item.category.id
                        ).blockingGet()
                        if (result > 0) {
                            updatedCount += result
                        } else {
                            unchangedCount += 1
                        }
                    }

                } else {
                    if(item.isReduction) {
                        val itemDb = TransactionItemDb(
                            null,
                            transactionId!!,
                            item.amount!!,
                            null,
                            1,
                            item.description!!,
                            "",
                            null,
                            item.category.id,
                            item.isReduction, maxOrdinal++,
                            dateTime,
                            offset,
                            zone
                        )
                        itemId = transactionItemRepository.addTransactionItem(itemDb).blockingGet()
                        val amountToAdd = item.amount!!.times(BigDecimal(-1))
                        total = total.plus(amountToAdd)
                    } else {
                        val itemDb = TransactionItemDb(
                            null,
                            transactionId!!,
                            item.amount!!,
                            item.brand,
                            item.quantity,
                            item.description!!,
                            item.variation,
                            item.referenceNumber,
                            item.category.id,
                            item.isReduction, maxOrdinal++,
                            dateTime,
                            offset,
                            zone
                        )
                        itemId = transactionItemRepository.addTransactionItem(itemDb).blockingGet()
                        val amountToAdd = item.amount!!
                        total = total.plus(amountToAdd)
                    }
                    newCount++
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
            val transaction = transactionRepository.getTransactionByIdSingle(transactionId!!).blockingGet()
            val account = accountRepository.getAccountByIdSingle(draft.accountId).blockingGet()
            var updatedTransaction = transaction.copy(
                amount = total,
                note = draft.note,
                accountId = draft.accountId,
                debitOrCredit = draft.debitOrCredit,
                currencyCode = account!!.currencyCode
            )


            val customDate = draft.customDate ?: draft.dbOriginalDate!!
            val customTime = draft.customTime ?: draft.dbOriginalTime!!
            LOGGER.debug("original: {}", draft.dbOriginalTime)
            val customDateTime = ZonedDateTime.of(customDate.atTime(customTime), timeAndLocaleHandler.getZone()).withZoneSameInstant(ZoneId.of("UTC"))

            val (datedDate, datedTime) =
                customDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) to customDateTime.format(
                    DateTimeFormatter.ISO_LOCAL_TIME
                )

            updatedTransaction =
                updatedTransaction.copy(datedAt = datedDate, datedAtTime = datedTime)
            LOGGER.info("saveEdit: Transaction {}: set custom dateTime", transactionId)

            transactionRepository.updateTransaction(updatedTransaction).blockingSubscribe()
            LOGGER.info("Transaction {}: Updated basic details", transactionId)
            LOGGER.info("Transaction {}: New Item Count: {}; Updated Item Count: {}; Unmodified Item Count: {}", transactionId, newCount, updatedCount, unchangedCount)
            for(item in draft.items) {
                
                for(image in item.deletedDbImages) {
                    var set = imageToItemsMap[image.sha256] ?: emptySet()
                    if (image.dbIsLinked) {
                        set -= item.dbId!!
                        imageToItemsMap[image.sha256] = set
                        itemImagesDao.deleteByItemAndImageId(item.dbId!!, image.dbId!!)
                            .blockingSubscribe()
                        LOGGER.info("Unlinked image {} from transaction item {}", image.dbId, item.dbId)
                    }
                        
                        val imageAttachedToOtherImagesInSameTransaction = set.isNotEmpty()
                        val imageAttachedToOtherImagesOutsideTransaction =
                            itemImagesDao.countItemImagesForOtherTransactions(transactionId!!)
                                .blockingGet() > 0
                        
                        if (!imageAttachedToOtherImagesInSameTransaction && !imageAttachedToOtherImagesOutsideTransaction) {
                            //TODO handle Glide resource release first
                            val file = image.uri.toFile()
                            if (file.delete()) {
                                LOGGER.info("Image {} file {} no longer needed. Deleted", image.dbId, file)
                                
                            } else {
                                LOGGER.info("Image {} file {} no longer needed, but failed to delete", image.dbId, file)
                            }
                            imageDao.deleteByIdSingle(image.dbId!!).blockingSubscribe()
                            LOGGER.info("Image {} deleted", image.dbId)
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
                                    LOGGER.info("Image {} file {} no longer needed. Deleted", image.dbId, file)
                                } else {
                                    LOGGER.info("Image {} file {} no longer needed, but failed to delete", image.dbId, file)
                                }
                                imageDao.deleteByIdSingle(image.dbId!!).blockingSubscribe()
                                LOGGER.info("Image {} deleted", image.dbId)
                            }

                    }
                }
                transactionItemDao.deleteById(item.dbId!!).blockingSubscribe()
            }
            LOGGER.info("Transaction {}: Deleted {} items", transactionId, draft.deletedDbItems.size)
            for(evidence in draft.deletedDbEvidence) {
                val file = evidence.uri.toFile()
                if (file.delete()) {
                    LOGGER.info("Evidence {} file {} no longer needed. Deleted", evidence.dbId, file)
                    
                } else {
                    LOGGER.info("Evidence {} file {} no longer needed, but failed to delete", evidence.dbId, file)
                }
                evidenceDao.deleteByIdSingle(evidence.dbId!!).blockingSubscribe()
                LOGGER.info("Evidence {} deleted", evidence.dbId)
            }
            LOGGER.info("Transaction {}: Deleted {} items", transactionId, draft.deletedDbEvidence.size)
            var addedEvidenceCount = 0
            for(evidence in draft.evidence) {
                if(evidence.dbId != null) {
                    continue
                }
                val file = evidence.uri.toFile()
                val localDate = LocalDate.now(timeAndLocaleHandler.getClock())
                val year = String.format("%04d", localDate.year)
                val month = String.format("%02d", localDate.monthValue)
                val day = String.format("%02d", localDate.dayOfMonth)
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_DOCUMENTS + File.separator + year + File.separator + month + File.separator + day).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val evidence = EvidenceDb(null, transactionId!!, length, evidence.sha256, evidence.mimeType, finalUri.toString(), dateTime, offset, zone)
                evidenceDao.insertEvidence(evidence).blockingGet()
                addedEvidenceCount++
            }
            LOGGER.info("Transaction {}: Evidence: Created: {};", transactionId, addedEvidenceCount)
        }.timeInterval().map {
            val time = it.time(TimeUnit.MILLISECONDS)
            LOGGER.info("saveEdit: took {} ms", time)
        }.subscribeOn(Schedulers.io())
    }

    fun deleteDraft() {
        incrementId = 0
        if(currentMode == "add") {
            draft = AddEditDetailedTransactionDraft(emptyList(), -1)
            _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.All, -1))
            fileHandler.deleteDraftFiles().blockingGet()
        } else if(currentMode == "edit") {

        }
    }

    fun setNote(note: String) {
        draft = draft.copy(note = note)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
        //TODO Debounce
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun validateDraft(): Boolean {
        val items = draft.items
        var valid = true
        if(items.isEmpty()) {
            LOGGER.info("validateDraft: Draft is empty, nothing to save")
            return false
        }
        for (item in items) {
            if (item.amount == null) {
                LOGGER.info("validateDraft: An item has a null amount")
                valid = false
                break
            }
            if (item.amount.compareTo(BigDecimal.ZERO) == 0) {
                LOGGER.info("validateDraft: An item has a zero amount")
                valid = false
                break
            }
            if (item.description == null) {
                LOGGER.info("validateDraft: An item has a null description")
                valid = false
                break
            }
            if (item.description.isBlank()) {
                LOGGER.info("validateDraft: An item has a blank description")
                valid = false
                break
            }
        }
        if(valid) {
            val reductionSum = draft.items.map {
                if(it.isReduction) {
                    it.amount!!
                } else {
                    BigDecimal.ZERO //This implicitly covers the case of only reductions
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            val regularSum = draft.items.map {
                if(!it.isReduction) {
                    it.amount!!
                } else {
                    BigDecimal.ZERO
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            if (regularSum.compareTo(reductionSum) <= 0) {
                LOGGER.info("Sum of reductions cannot be greater than or equal to the sum of regular items")
                valid = false
            }
        }
        return valid
    }

    fun isDraftEmpty(): Boolean {
        if(draft.evidence.isNotEmpty()) {
            return false
        }
        if(draft.customDate != null || draft.customTime != null) {
            return false
        }
        if(draft.note != null && draft.note!!.isNotBlank()) {
            return false
        }
        for(item in draft.items) {
            if(item.images.isNotEmpty()) {
                return false
            }
            if(item.amount != null && item.amount.compareTo(BigDecimal.ZERO) != 0) {
                return false
            }
            if(item.description != null && item.description.isNotBlank()) {
                return false
            }

            if(item.brand != null && item.brand.isNotBlank()) {
                return false
            }

            if(item.variation.isNotBlank()) {
                return false
            }

            if(item.referenceNumber != null && item.referenceNumber.isNotBlank()) {
                return false
            }
        }
        return true
    }

    fun setDate(localDate: LocalDate?) {
        draft = draft.copy(customDate = localDate)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
        if(localDate == null) {
            LOGGER.info("Set custom date to null")
        } else {
            LOGGER.info("Set custom date to a non-null value")
        }
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun setTime(localTime: LocalTime?) {
        draft = draft.copy(customTime = localTime)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
        if(localTime == null) {
            LOGGER.info("Set custom time to null")
        } else {
            LOGGER.info("Set custom time to a non-null value")
        }
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun setAccount(accountId: Long) {
        draft = draft.copy(accountId = accountId)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
        LOGGER.info("Changed account ID")
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun toggleDebitOrCredit() {
        val debitOrCredit = draft.debitOrCredit
        draft = draft.copy(debitOrCredit = !debitOrCredit)
        _draftLiveData.postValue(Triple(draft, TransactionItemsEvent.None, -1))
        LOGGER.info("Toggled debitOrCredit")
        if (currentMode == "add") {
            fileHandler.saveDraft(draft).subscribe()
        }
    }

    fun customDateTimeForDraft(draft: AddEditDetailedTransactionDraft): Pair<String, String?> {

        val customDate = draft.customDate ?: LocalDate.now(timeAndLocaleHandler.getClock())
        val customTime: LocalTime?
        if(customDate.isBefore(LocalDate.now(timeAndLocaleHandler.getClock()))) {
            customTime = draft.customTime
        } else {
            customTime = draft.customTime ?: LocalTime.now(timeAndLocaleHandler.getClock())
        }
        return customDate.toString() to customTime?.toString()
    }

    /**
     * Mainly meant for the `EditText`s to help avoid infinite loops with the `TextWatcher`s
     */
    enum class TransactionItemsEvent {
        Delete,
        Insert,
        All,
        Change,
        ChangeInvalidate,
        None
    }
    
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionRepository::class.java)
    }
}