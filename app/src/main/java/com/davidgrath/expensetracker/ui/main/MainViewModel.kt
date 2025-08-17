package com.davidgrath.expensetracker.ui.main

import android.app.Application
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.TempStatisticsConfig
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionsToTransactionItems
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.math.BigDecimal

class MainViewModel(private val application: Application, private val transactionRepository: TransactionRepository, private val categoryRepository: CategoryRepository): AndroidViewModel(application) {

    val listLiveData : LiveData<List<GeneralTransactionListItem>>
    val statsPastXByCategory: LiveData<List<BarEntry>>
    val statsTotalSpent: LiveData<BigDecimal>
    val statsTotalByCategory: LiveData<List<BarEntry>>
    val statsTotalByDay: LiveData<List<Pair<LocalDate, BigDecimal>>>

    var statisticsConfig = TempStatisticsConfig()
    private set
    private val _statisticsConfigLiveData = MutableLiveData<TempStatisticsConfig>(statisticsConfig)
    val statisticsConfigLiveData: LiveData<TempStatisticsConfig> = _statisticsConfigLiveData
    fun setConfig(statisticsConfig: TempStatisticsConfig) {
        this.statisticsConfig = statisticsConfig
        _statisticsConfigLiveData.postValue(statisticsConfig)
    }

    init {
        listLiveData = Observable.combineLatest(transactionRepository.getTransactions(), categoryRepository.getCategories()) { transactions, categories ->
            val list = arrayListOf<TransactionUi>()
            for((k,v) in transactions) {
                val localDateTime = k.getLocalDateTime()
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
        statsPastXByCategory = transactionRepository.getTransaction7DaySummary().map { list ->
            val mapped = list.mapIndexed { i, it ->
                val cat = categoryDbToCategoryUi(it.first)
//                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
            }
            mapped
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalSpent = transactionRepository.getTotalSpent().toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalByCategory = transactionRepository.getTotalSpentByCategory().map { list ->
            val mapped = list.mapIndexed { i, it ->
                val cat = categoryDbToCategoryUi(it.first)
//                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
            }
            mapped
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalByDay = transactionRepository.getTotalSpentByDate().toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle()
    }

    fun saveTransaction(amount: BigDecimal, description: String, categoryId: Long) {
        transactionRepository.addTransaction(amount, description, categoryId).subscribe( { id -> }, {})
    }
}