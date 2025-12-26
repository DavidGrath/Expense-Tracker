package com.davidgrath.expensetracker.ui.transactiondetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.entities.ui.TransactionDetailItemUi
import com.davidgrath.expensetracker.entities.ui.TransactionDetailsUi
import com.davidgrath.expensetracker.evidenceDbToEvidenceUi
import com.davidgrath.expensetracker.imageDbToImageUi
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.SellerRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionDbToTransactionDetailedUi
import io.reactivex.rxjava3.core.BackpressureStrategy

class TransactionDetailsViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val imageRepository: ImageRepository,
    private val categoryRepository: CategoryRepository,
    private val evidenceRepository: EvidenceRepository,
    private val accountRepository: AccountRepository,
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
    private val sellerRepository: SellerRepository,
    private val application: Application
) : AndroidViewModel(application) {

    val transaction: LiveData<TransactionDetailsUi>
    val items: LiveData<List<TransactionDetailItemUi>>
    val evidence: LiveData<List<EvidenceUi>>

    init {
        transaction = transactionRepository.getTransactionById(transactionId) //TODO Replace with Join query
            .map {
                val account = accountRepository.getAccountByIdSingle(it.accountId).blockingGet()!!
                val seller = if(it.sellerId == null) {
                    null
                } else {
                    sellerRepository.getSellerByIdSingle(it.sellerId).blockingGet()
                }
                val sellerLocation = if(it.sellerLocationId == null) {
                    null
                } else {
                    sellerRepository.getSellerLocationByIdSingle(it.sellerLocationId).blockingGet()
                }

                transactionDbToTransactionDetailedUi(it, account, seller, sellerLocation)
            }
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        items = transactionItemRepository.getTransactionItems(transactionId)
            .map { list ->
                val t = transactionRepository.getTransactionByIdSingle(transactionId).blockingGet()
                val account = accountRepository.getAccountByIdSingle(t.accountId).blockingGet()!!
                list.map { item ->
                    val images = imageRepository.getTransactionItemImages(item.id!!).blockingGet()
                        .map { image -> imageDbToImageUi(image) }
                    val category = categoryDbToCategoryUi(application, categoryRepository.getById(item.primaryCategoryId).blockingGet())
                    val secondaryCategories = categoryRepository.getOtherItemCategories(item.id).blockingGet().map { categoryDbToCategoryUi(application, it) }
                    TransactionDetailItemUi(item.id!!, item.transactionId, item.amount, account.currencyCode, item.description, category, secondaryCategories, item.brand, item.quantity, item.referenceNumber, item.variation, item.isReduction, images)
                }
            }
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        evidence = evidenceRepository.getByTransactionId(transactionId)
            .map { list ->
                list.map {
                    evidenceDbToEvidenceUi(it)
                }
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }
}