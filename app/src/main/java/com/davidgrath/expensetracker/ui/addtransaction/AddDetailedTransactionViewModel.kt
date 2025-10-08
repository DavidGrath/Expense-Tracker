package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Application
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
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.loadRenderer
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
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
    private val profileDao: ProfileDao,
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
    val profile = profileDao.getByStringId(profileStringId).subscribeOn(Schedulers.io()).blockingGet()

    val accountsLiveData = accountRepository.getAccountsForProfile(profile.id!!).toFlowable(BackpressureStrategy.BUFFER).toLiveData()
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
                it.amount ?: BigDecimal.ZERO
            }.reduceOrNull { acc, bd -> acc.plus(bd) }?.setScale(2, RoundingMode.HALF_UP)
                ?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            Triple(accounts, accountDbToAccountUi(accountDb), total)
        }
    }

    init {
        LOGGER.debug("profile: {}", profile)
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

    fun addItemFile(returnedUri: Uri): LiveData<Unit> {
        val mimeType = application.contentResolver.getType(returnedUri) ?: ""
        return addDetailedTransactionRepository.addImageToItem(getImageItemId, returnedUri, mimeType).subscribeOn(Schedulers.io()).map {
            getImageItemId = -1
        }.subscribeOn(Schedulers.io()).toFlowable().toLiveData()
    }

    fun addEvidence(returnedUri: Uri): LiveData<PdfState> {
        val mimeType = application.contentResolver.getType(returnedUri) ?: ""

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
        return categoryRepository.getCategoriesSingle()
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

    fun setUseCustomDateTime(useCustomDateTime: Boolean) {
        addDetailedTransactionRepository.setUseCustomDateTime(useCustomDateTime)
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