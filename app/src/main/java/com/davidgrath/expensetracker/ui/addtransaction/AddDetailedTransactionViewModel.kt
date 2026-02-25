package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.entities.ui.ImageAddResult
import com.davidgrath.expensetracker.entities.ui.ImageModificationDetails
import com.davidgrath.expensetracker.entities.ui.SellerLocationUi
import com.davidgrath.expensetracker.entities.ui.SellerUi
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getLocationData
import com.davidgrath.expensetracker.getResizeDimensions
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.loadRenderer
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.SellerRepository
import com.davidgrath.expensetracker.sellerDbToSellerUi
import com.davidgrath.expensetracker.sellerLocationDbToSellerLocationUi
import com.davidgrath.expensetracker.ui.main.MainViewModel
import com.davidgrath.expensetracker.utils.ImageHelper
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class AddDetailedTransactionViewModel(
    private val application: Application,
    val mode: String,
    private val addDetailedTransactionRepository: AddDetailedTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val imageRepository: ImageRepository,
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
    private val sellerRepository: SellerRepository,
    private val imageHelper: ImageHelper,
    private val profileStringId: String,
    private val transactionId: Long?,
    private val initialAccountId: Long?,
    private val initialAmount: BigDecimal?,
    private val initialDescription: String?,
    private val initialCategoryId: Long?
) : AndroidViewModel(application) {


    var getImageItemId = -1
    private var rendererHashMap = emptyMap<Uri, PdfRenderer>()
    private val _rendererLiveData = MutableLiveData<Map<Uri, PdfRenderer>>(rendererHashMap)
    var rendererLiveData: LiveData<Map<Uri, PdfRenderer>> = _rendererLiveData
    val profile = profileRepository.getByStringId(profileStringId).subscribeOn(Schedulers.io()).blockingGet()

    val accountsLiveData = accountRepository.getAccountsForProfile(profile.id!!).toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    val sellersMediatorLiveData = MediatorLiveData<Pair<Long?, List<SellerUi>>>()
    private val sellersLiveData: LiveData<List<SellerUi>> = sellerRepository.getSellers(profile.id!!).map { list -> listOf(SellerUi(-1, "test")) + list.map { sellerDbToSellerUi(it) } } .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    val sellerLocationsMediatorLiveData = MediatorLiveData<Pair<Long?, List<SellerLocationUi>>>()
    private val sellerLocationsLiveData: LiveData<List<SellerLocationUi>> = addDetailedTransactionRepository.getDraft().switchMap {
        val initialEmpty = listOf(SellerLocationUi(-1, -1, "", false, null, null, null))
        val sellerId = it.first.sellerId
        if (sellerId == null) {
            val liveData = MutableLiveData<List<SellerLocationUi>>()
            liveData.postValue(initialEmpty)
            liveData
        } else {
            sellerRepository.getSellerLocations(sellerId).map { list -> initialEmpty + list.map { sellerLocationDbToSellerLocationUi(it) } } .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }

    }
    val mediator = MediatorLiveData<Triple<List<AccountDb>, AccountUi, BigDecimal>>()

    val transactionItemsLiveData = addDetailedTransactionRepository.getDraft()
    val currentAccount = addDetailedTransactionRepository.getDraft().map {
        val accounts = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet()
        val draft = it.first
        val accountId = it.first.accountId
        val accountDb = accounts.find { it.id == accountId }
        if(accountDb == null) {
            Triple(accounts, AccountUi(-1, -1, "AAA", "Error", "Error"), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        } else {
            val total = draft.items.map {
                if(it.isReduction) {
                    if(it.amount != null) {
                        it.amount.times(BigDecimal(-1))
                    } else {
                        it.amount ?: BigDecimal.ZERO
                    }
                } else {
                    it.amount ?: BigDecimal.ZERO
                }
            }.reduceOrNull { acc, bd -> acc.plus(bd) }?.setScale(2, RoundingMode.HALF_UP)
                ?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            Triple(accounts, accountDbToAccountUi(accountDb, timeAndLocaleHandler.getLocale()), total)
        }
    }

    init {
        addDetailedTransactionRepository.setProfile(profile.id!!)
        addDetailedTransactionRepository.setMode(mode)
        mediator.addSource(currentAccount) {
            mediator.postValue(it)
        }
        mediator.addSource(accountsLiveData) {
            if(currentAccount.value != null) {
                mediator.postValue(
                    Triple(
                        it,
                        currentAccount.value.second,
                        currentAccount.value.third
                    )
                )
            }
        }
        sellersMediatorLiveData.addSource(addDetailedTransactionRepository.getDraft().map { it.first.sellerId }) {
            sellersMediatorLiveData.postValue(it to (sellersLiveData.value?: emptyList()))
        }
        sellersMediatorLiveData.addSource(sellersLiveData) {
            sellersMediatorLiveData.postValue(sellersMediatorLiveData.value?.first to it)
        }

        sellerLocationsMediatorLiveData.addSource(addDetailedTransactionRepository.getDraft().map { it.first.sellerLocation?.id }) {
            sellerLocationsMediatorLiveData.postValue(it to (sellerLocationsLiveData.value?: emptyList()))
        }
        sellerLocationsMediatorLiveData.addSource(sellerLocationsLiveData) {
            sellerLocationsMediatorLiveData.postValue(sellerLocationsMediatorLiveData.value?.first to it)
        }

        if(mode == "add") {
            if (!addDetailedTransactionRepository.draftExists()) {
                if (initialAmount != null || initialDescription != null || initialCategoryId != null) {
                    addDetailedTransactionRepository.createDraft().blockingGet()
                    addDetailedTransactionRepository.initializeDraft(
                        initialAccountId!!,
                        initialAmount,
                        initialDescription,
                        initialCategoryId
                    )
                } else {
                    addDetailedTransactionRepository.createDraft().blockingGet()
                    val account = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet()[0]
                    addDetailedTransactionRepository.setAccount(account.id!!)
                    LOGGER.info("Set default account {} for draft", account.id)
                    addDetailedTransactionRepository.addItem()
                    LOGGER.info("Added blank item to draft")
                }
            } else {
                if (initialAmount != null || initialDescription != null || initialCategoryId != null) {
                    addDetailedTransactionRepository.moveToTopOfDraft(
//                        initialAccountId!!,
                        initialAmount,
                        initialDescription,
                        initialCategoryId
                    )
                }
                addDetailedTransactionRepository.restoreDraft().blockingSubscribe()
                loadRenderers()
            }
        } else if(mode == "edit") {
            addDetailedTransactionRepository.initializeEdit(transactionId!!).blockingSubscribe {
                loadRenderers()
                LOGGER.info("Loaded transaction $transactionId")
            }
        }
    }

    private fun loadRenderers() {
        var hasPDFs = false
        for (evidence in addDetailedTransactionRepository.getDraftValue().evidence) {
            if (evidence.mimeType != "application/pdf") {
                continue
            }
            hasPDFs = true
            val renderer = loadRenderer(evidence.uri).blockingGet()
            if (renderer != null) {
                rendererHashMap += evidence.uri to renderer
            }
        }
        _rendererLiveData.postValue(rendererHashMap)
        if (hasPDFs) {
            LOGGER.info("Loaded renderers for existing draft PDFs")
        }
    }

    fun addItem(): Boolean {
        return addDetailedTransactionRepository.addItem()
    }

    fun onItemChanged(position: Int, item: AddTransactionItem) {
        addDetailedTransactionRepository.changeItem(position, item)
    }

    fun onItemChangedInvalidate(position: Int, item: AddTransactionItem) {
        addDetailedTransactionRepository.changeItemInvalidate(position, item)
    }

    /**
     * @return If `actionNeeded`, then a dialog is needed to let the user modify the image
     */
    fun addItemFile(returnedUri: Uri): LiveData<ImageAddResult> {
        val mimeType = if(returnedUri.scheme == ContentResolver.SCHEME_CONTENT) {
            application.contentResolver.getType(returnedUri) ?: ""
        } else if(returnedUri.scheme == ContentResolver.SCHEME_FILE) {
            Constants.MimeTypes.JPEG.type //Hardcoding because I'm not sure how to determine what the native camera returns
        } else {
            ""
        }
        val single: Single<ImageAddResult> =
        if(mimeType == Constants.MimeTypes.JPEG.type ) { //TODO this is just for JPEG. I need to consider if there are Android vendors out there that for whatever reason chose another format for their cameras
            LOGGER.info("Type is JPEG. Checking for location and compressibility")
            Single.fromCallable {
                val hashInputStream = application.contentResolver.openInputStream(returnedUri)!!
                val hash = getSha256(hashInputStream).blockingGet()
                hashInputStream.close()
                val existingOriginalImage = addDetailedTransactionRepository.getDraftValue().sourceImageHashes[hash]
                val existingImage = addDetailedTransactionRepository.getDraftValue().imageHashes[hash]
                if(existingImage != null || existingOriginalImage != null) {
                    LOGGER.info("Image already exists in draft. Don't bother checking again")
                    addDetailedTransactionRepository.addImageToItem(getImageItemId, returnedUri, mimeType).blockingSubscribe()
                    return@fromCallable ImageAddResult(false, null)
                }

                val folder = file(application.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGE_MODIFICATION)
                if(folder.exists()) {
                    LOGGER.info("Existing image mod folder found. Deleting")
                    val del = folder.deleteRecursively()
                    LOGGER.info("Delete image mode folder: {}", del)
                }
                folder.mkdirs()
                val file = File(folder, Constants.FILE_NAME_SELECTED_IMAGE)
                val inStream = application.contentResolver.openInputStream(returnedUri)!!
                val outStream = file.outputStream()
                inStream.copyTo(outStream)
                inStream.close()
                outStream.close()

                val size = file.length()
                var widthHeight: Pair<Int, Int>? = null
                var targetDimensions: Triple<Int, Int, Int>? = null
                val tooLarge = size > Constants.IMAGE_SIZE_THRESHOLD
                var compressedSize: Long? = null
                if(tooLarge) {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }

                    val bitmapStream = file.inputStream()
                    val bufBitmapStream = BufferedInputStream(bitmapStream)
                    BitmapFactory.decodeStream(bufBitmapStream, null, options)
                    bufBitmapStream.close()
                    bitmapStream.close()
                    widthHeight = options.outWidth to options.outHeight
                    targetDimensions = getResizeDimensions(widthHeight)
                    val resizedFile = File(folder, "resized-" + file.name)
                    compressedSize = imageHelper.compressImage(file, resizedFile, targetDimensions.third).blockingGet()
                }
                val locationData = getLocationData(file).blockingGet()
                val hasLocationData = locationData != null
                val actionNeeded = tooLarge || hasLocationData
                LOGGER.debug("TooLarge: {}, WidthHeight: {}", tooLarge, widthHeight)
                LOGGER.debug("TargetDimension: {}", targetDimensions)
                if(!actionNeeded) {
                    addDetailedTransactionRepository.addImageToItem(getImageItemId, returnedUri, mimeType).blockingSubscribe()
                }
                ImageAddResult(actionNeeded, ImageModificationDetails(size, getImageItemId, hash, mimeType, compressedSize, widthHeight, if(targetDimensions != null) (targetDimensions!!.first to targetDimensions!!.second) else null, locationData))
            }

        } else {
            addDetailedTransactionRepository.addImageToItem(getImageItemId, returnedUri, mimeType).map {
                ImageAddResult(false, null)
            }
        }
        return single.subscribeOn(Schedulers.io()).doOnTerminate {
            getImageItemId = -1
        }.subscribeOn(Schedulers.io()).toFlowable().toLiveData()
    }

    fun addEvidence(returnedUri: Uri): LiveData<PdfState> {
        val mimeType = if(returnedUri.scheme == ContentResolver.SCHEME_CONTENT) {
            application.contentResolver.getType(returnedUri) ?: ""
        } else if(returnedUri.scheme == ContentResolver.SCHEME_FILE) {
            "image/jpeg" //Hardcoding because I'm not sure how to determine what the native camera returns
        } else {
            ""
        }

        return addDetailedTransactionRepository.addEvidence(returnedUri, mimeType).map { uri ->
            if (mimeType != "application/pdf") {
                LOGGER.info("Document not a PDF")
                return@map PdfState.NOT_PDF
            }
            val validate = validatePdf(uri).blockingGet()
            if (validate.first == PdfState.PASSWORD_PROTECTED || validate.first == PdfState.ZERO_PAGES) {
                return@map validate.first
            }
            rendererHashMap += (uri to validate.second!!)
            _rendererLiveData.postValue(rendererHashMap)
            LOGGER.info("Updated PDF renderer hashMap")
            return@map validate.first
        }.subscribeOn(Schedulers.io()).map {
            getImageItemId = -1
            it
        }.subscribeOn(Schedulers.io()).toFlowable().toLiveData()
    }

    fun onItemDeleted(position: Int) {
        addDetailedTransactionRepository.deleteItem(position)
    }

    fun onItemImageDeleted(itemPosition: Int, imagePosition: Int) {
        addDetailedTransactionRepository.deleteItemImage(itemPosition, imagePosition)
    }

    fun finishDraft(): LiveData<Unit> {
        return addDetailedTransactionRepository.finishTransaction().toFlowable().toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle(profile.id!!)
    }

    fun validateDraft(): Boolean {
        return addDetailedTransactionRepository.validateDraft()
    }

    fun validatePdf(uri: Uri): Single<Pair<PdfState, PdfRenderer?>> {
        return Single.fromCallable {
            val file = uri.toFile()
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer: PdfRenderer? = try {
                PdfRenderer(fd)
                //TODO Handle IO Exception
            } catch (e: SecurityException) {
                LOGGER.error("validatePdf", e)
                null
            }
            if (pdfRenderer == null) {
                val state = PdfState.PASSWORD_PROTECTED
                LOGGER.info("validatePdf: {}", state)
                return@fromCallable state to null
            }
            if (pdfRenderer.pageCount == 0) {
                val state = PdfState.ZERO_PAGES
                LOGGER.info("validatePdf: {}", state)
                return@fromCallable state to null
            }
            val state = PdfState.ALL_GOOD
            LOGGER.info("validatePdf: {}", state)
            state to pdfRenderer
        }.subscribeOn(Schedulers.io())

    }

    fun setNote(note: String) {
        addDetailedTransactionRepository.setNote(note)
    }

    fun setCustomDate(localDate: LocalDate?) {
        addDetailedTransactionRepository.setDate(localDate)
    }

    fun setCustomTime(localTime: LocalTime?) {
        addDetailedTransactionRepository.setTime(localTime)
    }

    fun setAccountId(accountId: Long) {
        addDetailedTransactionRepository.setAccount(accountId)
    }

    fun toggleDebitOrCredit() {
        addDetailedTransactionRepository.toggleDebitOrCredit()
    }

    fun onDeleteEvidence(position: Int, uri: Uri) {
        val mutable = rendererHashMap.toMutableMap()

        val renderer = mutable.remove(uri)
        if(renderer != null) {
            LOGGER.info("onDeleteEvidence: Removed PDF Renderer for evidence")
            renderer.close()
            LOGGER.info("onDeleteEvidence: Closed PDF Renderer")
        } else {
            LOGGER.info("No PDF renderer for evidence")
        }
        rendererHashMap = mutable.toMap()
        _rendererLiveData.postValue(rendererHashMap)
        LOGGER.info("onDeleteEvidence: Updated renderer hash map")
        addDetailedTransactionRepository.removeEvidence(position)
    }

    fun getImageCount(): Single<Long> {
        return imageRepository.getImageCountSingle(profile.id!!)
    }

    fun addSeller(name: String) {
        val id = sellerRepository.createSeller(profile.id!!, name).blockingGet()
        addDetailedTransactionRepository.setSeller(id)
        LOGGER.info("Created seller {} for profile {}", id, profile.id)
    }

    fun addSellerLocation(location: String, sellerId: Long) {
        val id = sellerRepository.createSellerLocation(location, sellerId).blockingGet()
        addDetailedTransactionRepository.setSellerLocation(SellerLocationUi(id, sellerId, location, false, null, null, null))
        LOGGER.info("Created seller location {} for profile {}", id, profile.id)
    }

    fun setSeller(sellerId: Long?) {
        addDetailedTransactionRepository.setSeller(sellerId)
    }
    fun setSellerLocation(sellerLocation: SellerLocationUi?) {
        addDetailedTransactionRepository.setSellerLocation(sellerLocation)
    }

    fun setTransactionMode(transactionMode: TransactionMode) {
        addDetailedTransactionRepository.setTransactionMode(transactionMode)
    }

    fun getSellerId(): Long? {
        return addDetailedTransactionRepository.getDraftValue().sellerId
    }

    fun addSelectedImage(sourceHash: String, mimeType: String, itemId: Int?, reduceSize: Boolean, removeGpsData: Boolean): Single<Unit> {
        return Single.fromCallable {


            val folder = file(
                application.filesDir.absolutePath,
                Constants.FOLDER_NAME_DRAFT,
                Constants.SUBFOLDER_NAME_IMAGE_MODIFICATION
            )
            var file = if (reduceSize) {
                File(folder, "resized-${Constants.FILE_NAME_SELECTED_IMAGE}")
            } else {
                File(folder, Constants.FILE_NAME_SELECTED_IMAGE)
            }
            if (!file.exists()) {
                LOGGER.warn("File does not exist")
                return@fromCallable
            }
            if (removeGpsData) {
                imageHelper.removeLocationData(folder, file).blockingSubscribe()
                val locationLessFile = File(folder, "nogps-${file.name}")
                file = locationLessFile
            }
            val fileUri = file.toUri()
            if(reduceSize || removeGpsData) {
                addDetailedTransactionRepository.addImageToItem(itemId!!, fileUri, mimeType, sourceHash).blockingSubscribe() //TODO ItemId should be null for documents
            } else {
                addDetailedTransactionRepository.addImageToItem(itemId!!, fileUri, mimeType, null).blockingSubscribe() //TODO ItemId should be null for documents
            }
        }
    }

    enum class PdfState {
        NOT_PDF,
        ALL_GOOD,
        PASSWORD_PROTECTED,
        ZERO_PAGES
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionViewModel::class.java)
    }
}