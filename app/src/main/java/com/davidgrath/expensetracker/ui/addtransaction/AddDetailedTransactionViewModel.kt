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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class AddDetailedTransactionViewModel(
    private val application: Application,
    private val mode: String,
    private val addDetailedTransactionRepository: AddDetailedTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
    private val transactionId: Long?,
    private val initialAmount: BigDecimal?,
    private val initialDescription: String?,
    private val initialCategoryId: Long?
) : AndroidViewModel(application) {


    var getImageItemId = -1
    private var rendererHashMap = emptyMap<Uri, PdfRenderer>()
    private val _rendererLiveData = MutableLiveData<Map<Uri, PdfRenderer>>(rendererHashMap)
    var rendererLiveData: LiveData<Map<Uri, PdfRenderer>> = _rendererLiveData

    init {
        if(mode == "add") {
            if (!addDetailedTransactionRepository.draftExists()) {
                if (initialAmount != null || initialDescription != null || initialCategoryId != null) {
                    addDetailedTransactionRepository.createDraft().blockingGet()
                    addDetailedTransactionRepository.initializeDraft(
                        initialAmount,
                        initialDescription,
                        initialCategoryId
                    )
                } else {
                    addDetailedTransactionRepository.createDraft().blockingGet()
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
                addDetailedTransactionRepository.restoreDraft().blockingSubscribe {
                    Log.i(LOG_TAG, "Restored existing draft")
                }
                var hasPDFs = false
                for (evidence in addDetailedTransactionRepository.getDraftValue().evidence) {
                    if (evidence.mimeType != "application/pdf") {
                        continue
                    }
                    hasPDFs = true
                    val renderer = loadRenderer(evidence).blockingGet()
                    if (renderer != null) {
                        rendererHashMap += evidence.uri to renderer
                    }
                }
                _rendererLiveData.postValue(rendererHashMap)
                if (hasPDFs) {
                    Log.i(LOG_TAG, "Loaded renderers for existing draft PDFs")
                }
            }
        } else if(mode == "edit") {
            addDetailedTransactionRepository.initializeEdit(transactionId!!).blockingSubscribe {
                Log.i(LOG_TAG, "Loaded transaction $transactionId")
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
                Log.i(LOG_TAG, "Document not a PDF")
                return@map PdfState.NOT_PDF
            }
            val validate = validatePdf(uri).blockingGet()
            if (validate.first == PdfState.PASSWORD_PROTECTED || validate.first == PdfState.ZERO_PAGES) {
                return@map validate.first
            }
            rendererHashMap += (uri to validate.second!!)
            _rendererLiveData.postValue(rendererHashMap)
            return@map validate.first
        }.subscribeOn(Schedulers.io()).map {
            getImageItemId = -1
            it
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
                null
            }
            if (pdfRenderer == null) {
                return@fromCallable PdfState.PASSWORD_PROTECTED to null
            }
            if (pdfRenderer.pageCount == 0) {
                return@fromCallable PdfState.ZERO_PAGES to null
            }
            PdfState.ALL_GOOD to pdfRenderer
        }.subscribeOn(Schedulers.io())

    }

    fun setNote(note: String) {
        addDetailedTransactionRepository.setNote(note)
    }

    fun loadRenderer(evidence: AddEditTransactionFile): Maybe<PdfRenderer> {
        return Maybe.fromCallable {
            val file = evidence.uri.toFile()
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer: PdfRenderer? = try {
                PdfRenderer(fd)
            } catch (e: SecurityException) {
                null
            } catch (e: IOException) {
                null
            }
            return@fromCallable pdfRenderer
        }
    }


    enum class PdfState {
        NOT_PDF,
        ALL_GOOD,
        PASSWORD_PROTECTED,
        ZERO_PAGES
    }

    companion object {
        private const val LOG_TAG = "AddDetailTransViewModel"
    }
}