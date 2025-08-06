package com.davidgrath.expensetracker.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionsToTransactionItems
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

class MainViewModel(private val transactionRepository: TransactionRepository): ViewModel() {

    val listLiveData : LiveData<List<GeneralTransactionListItem>>

    init {
        listLiveData = transactionRepository.getTransactions().map {
            val list = arrayListOf<TransactionUi>()
            for((k,v) in it) {
                val utcDateTime = LocalDateTime.parse(k.datedAt)
                val offset = ZoneOffset.of(k.datedAtOffset)
                val offsetDateTime = OffsetDateTime.of(utcDateTime, offset)
                val localDateTime = offsetDateTime.toLocalDateTime()
                val transaction = TransactionUi(k.id!!, k.amount, k.currencyCode, k.cashOrCredit, localDateTime, localDateTime, null, emptyList())
                val items = v.map { item -> TransactionItemUi(transaction, item.amount, item.description, CategoryUi.TEMP_DEFAULT_CATEGORIES.find { it.stringId == "fitness" }!!, item.brand) }
                list.add(transaction.copy(items = items))
            }
            transactionsToTransactionItems(list)
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }
}