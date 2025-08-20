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
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.TempStatisticsConfig
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.TransactionWithItemAndCategoryUi
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionsToTransactionItems
import com.github.mikephil.charting.data.BarEntry
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.jspecify.annotations.Nullable
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneOffset
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainViewModel
@Inject
constructor(
    private val application: Application, private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository, private val clock: Clock
) : AndroidViewModel(application) {

    val listLiveData: LiveData<List<GeneralTransactionListItem>>
    val statsPastXByCategory: LiveData<List<BarEntry>>
    val statsTotalSpent: LiveData<BigDecimal>
//    val statsTotalByCategory: LiveData<List<BarEntry>>
    val statsTotalByDay: LiveData<List<DateAmountSummary>>
    //TODO For refreshing at midnight
//    private val minuteTicker = Observable.interval(1, TimeUnit.MINUTES)

    var statisticsConfig = TempStatisticsConfig()
        private set
    private val _statisticsConfigLiveData = MutableLiveData<TempStatisticsConfig>(statisticsConfig)
    val statisticsConfigLiveData: LiveData<TempStatisticsConfig> = _statisticsConfigLiveData
    fun setConfig(statisticsConfig: TempStatisticsConfig) {
        this.statisticsConfig = statisticsConfig
        _statisticsConfigLiveData.postValue(statisticsConfig)
    }

    init {
        listLiveData = transactionRepository.getTransactions().map{ transactionsAndItems ->
            val list = arrayListOf<TransactionWithItemAndCategoryUi>()
            for (item in transactionsAndItems) {
                val createdDateTime = getCreatedLocalDateTime(item)
                val datedDate = LocalDate.parse(item.transactionDatedAt)
                val datedDateTime = getLocalDateTime(item)
                val category = categoryDbToCategoryUi(item)
                val images =
                    transactionRepository.getTransactionItemImages(item.itemId).blockingGet()
                        .map {
                            Uri.parse(it.uri)
                        }
                val view = TransactionWithItemAndCategoryUi(item.transactionId, item.itemId, item.accountId, item.transactionTotal, item.itemAmount, item.currencyCode, item.cashOrCredit,
                    item.description, createdDateTime, datedDate, datedDateTime?.toLocalTime(), category, images)
                list.add(view)
            }
            transactionsToTransactionItems(list)
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsPastXByCategory = transactionRepository.getTotalSpentByCategory().map { list ->
            val mapped = list.mapIndexed { i, it ->
                val cat = categoryDbToCategoryUi(it)
//                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
                BarEntry(
                    i.toFloat(),
                    it.sum.toFloat(),
                    ResourcesCompat.getDrawable(application.resources, cat.iconId, null)
                )
            }
            mapped
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalSpent =
            transactionRepository.getTotalSpent().toFlowable(BackpressureStrategy.BUFFER)
                .toLiveData()
        /*statsTotalByCategory = transactionRepository.getTotalSpentByCategory().map { list ->
            val mapped = list.mapIndexed { i, it ->
                val cat = categoryDbToCategoryUi(it.first)
//                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
                BarEntry(
                    i.toFloat(),
                    it.second.toFloat(),
                    ResourcesCompat.getDrawable(application.resources, cat.iconId, null)
                )
            }
            mapped
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()*/
        statsTotalByDay = transactionRepository.getTotalSpentByDate(LocalDate.now(clock).toString())
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle()
    }

    fun saveTransaction(amount: BigDecimal, description: String, categoryId: Long) {
        transactionRepository.addTransaction(amount, description, categoryId)
            .subscribe({ id -> }, {})
    }

    fun getCreatedLocalDateTime(transactionWithItemAndCategory: TransactionWithItemAndCategory): LocalDateTime {
        val utcDateTime = LocalDateTime.parse(transactionWithItemAndCategory.transactionCreatedAt)
        val offset = ZoneOffset.of(transactionWithItemAndCategory.transactionCreatedAtOffset)
        val offsetDateTime = utcDateTime.atOffset(offset)
        val localDateTime = offsetDateTime.toLocalDateTime()
        return localDateTime
    }

    fun getLocalDateTime(transactionWithItemAndCategory: TransactionWithItemAndCategory): LocalDateTime? {
        if (transactionWithItemAndCategory.transactionDatedAtTime == null) {
            return null
        }
        val utcDate = LocalDate.parse(transactionWithItemAndCategory.transactionDatedAt)
        val utcTime = LocalTime.parse(transactionWithItemAndCategory.transactionDatedAtTime)
        val offset = ZoneOffset.of(transactionWithItemAndCategory.transactionDatedAtOffset)
        val utcDateTime = utcDate.atTime(utcTime)
        val offsetDateTime = utcDateTime.atOffset(offset)
        val localDateTime = offsetDateTime.toLocalDateTime()
        return localDateTime
    }
}