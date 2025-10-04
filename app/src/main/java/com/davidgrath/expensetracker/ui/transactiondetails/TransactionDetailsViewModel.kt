package com.davidgrath.expensetracker.ui.transactiondetails

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.entities.ui.TransactionDetailItemUi
import com.davidgrath.expensetracker.entities.ui.TransactionDetailsUi
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.evidenceDbToEvidenceUi
import com.davidgrath.expensetracker.imageDbToImageUi
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionDbToTransactionDetailedUi
import com.davidgrath.expensetracker.transactionDbToTransactionUi
import io.reactivex.rxjava3.core.BackpressureStrategy
import javax.inject.Inject

class TransactionDetailsViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val imageRepository: ImageRepository,
    private val categoryRepository: CategoryRepository,
    private val evidenceRepository: EvidenceRepository,
    private val accountRepository: AccountRepository,
    private val timeHandler: TimeHandler
) : ViewModel() {

    val transaction: LiveData<TransactionDetailsUi>
    val items: LiveData<List<TransactionDetailItemUi>>
    val evidence: LiveData<List<EvidenceUi>>

    init {
        transaction = transactionRepository.getTransactionById(transactionId)
            .map {
                //TODO Accounts in Add screen
                val account = accountRepository.getAccountByIdSingle(1).blockingGet()
                transactionDbToTransactionDetailedUi(timeHandler, it, account)
            }
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        items = transactionItemRepository.getTransactionItems(transactionId)
            .map { list ->
                list.map { item ->
                    val images = imageRepository.getTransactionItemImages(item.id!!).blockingGet()
                        .map { image -> imageDbToImageUi(image) }
                    val category = categoryDbToCategoryUi(categoryRepository.getById(item.primaryCategoryId).blockingGet())
                    val secondaryCategories = categoryRepository.getOtherItemCategories(item.id).blockingGet().map { categoryDbToCategoryUi(it) }
                    TransactionDetailItemUi(item.id!!, item.transactionId, item.amount, item.description, category, secondaryCategories, item.brand, images)
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