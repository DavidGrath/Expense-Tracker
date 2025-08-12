package com.davidgrath.expensetracker.ui.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.Utils
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionsToTransactionItems
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal

class MainViewModel(private val transactionRepository: TransactionRepository, private val categoryRepository: CategoryRepository): ViewModel() {

    val listLiveData : LiveData<List<GeneralTransactionListItem>>

    init {
        listLiveData = Observable.combineLatest(transactionRepository.getTransactions(), categoryRepository.getCategories()) { transactions, categories ->
            val list = arrayListOf<TransactionUi>()
            for((k,v) in transactions) {
                val utcDateTime = LocalDateTime.parse(k.datedAt)
                val offset = ZoneOffset.of(k.datedAtOffset)
                val offsetDateTime = utcDateTime.atOffset(offset)
                val localDateTime = offsetDateTime.toLocalDateTime()
                val transaction = TransactionUi(k.id!!, k.amount, k.currencyCode, k.cashOrCredit, localDateTime, localDateTime, null, emptyList())
                val items = v.map { item ->
                    val dbCategory = categories.find { it.id == item.primaryCategoryId }!!
                    val category = categoryDbToCategoryUi(dbCategory)
                    val images = transactionRepository.getTransactionItemImages(item.id!!).blockingGet().map {
                        Uri.parse(it.uri)
                    }
                    TransactionItemUi(transaction, item.amount, item.description, category, item.brand, images)
                }
                list.add(transaction.copy(items = items))
            }
            transactionsToTransactionItems(list)
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle()
    }

    fun saveTransaction(amount: BigDecimal, description: String, categoryId: Long) {
        transactionRepository.addTransaction(amount, description, categoryId).subscribe( { id -> }, {})
    }
}