package com.davidgrath.expensetracker.ui.main.statistics

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DayOfWeekGsonAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.LocalDateGsonAdapter
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.entities.ui.StatisticsFilter
import com.davidgrath.expensetracker.entities.ui.TransactionWithItemAndCategoryUi
import com.davidgrath.expensetracker.getFilteredWeekDays
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionWithCategoryToCategoryUi
import com.davidgrath.expensetracker.transactionsToTransactionItems
import com.davidgrath.expensetracker.ui.main.MainViewModel
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.io.File
import javax.inject.Inject

class FilteredTransactionsViewModel
@Inject
    constructor(
    private val application: Application,
        private val transactionRepository: TransactionRepository,
        private val transactionItemRepository: TransactionItemRepository,
        private val imageRepository: ImageRepository,
        private val timeAndLocaleHandler: TimeAndLocaleHandler
        ): AndroidViewModel(application) {

    private var statisticsFilter: StatisticsFilter
    private val gson = GsonBuilder()
        .registerTypeAdapter(DayOfWeek::class.java, DayOfWeekGsonAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateGsonAdapter())
        .create()

    var transactionsAndItems: LiveData<List<GeneralTransactionListItem>>
    val statsTransactionAndItemCount: LiveData<TransactionAndItemCount>

        init {
            val file = File(application.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
            if(file.exists()) {
                val size = file.length()
                val reader = file.bufferedReader()
                statisticsFilter = gson.fromJson(reader, StatisticsFilter::class.java)
                LOGGER.info("Read {} bytes from existing statistics JSON file", size)
            } else {
                statisticsFilter = StatisticsFilter()
                LOGGER.info("No statistics JSON file found")
            }
            val app = application as ExpenseTracker
            val profile = app.profileObservable.blockingFirst()
            val dates = getFilteredWeekDays(profile.id!!, statisticsFilter, StatisticsConfig.DateMode.All, timeAndLocaleHandler, transactionRepository)
            transactionsAndItems = transactionRepository.getTransactionsFiltered(profile.id!!, statisticsFilter.startDay?.toString(), statisticsFilter.endDay?.toString(), statisticsFilter.accountIds, dates, statisticsFilter.categories, statisticsFilter.modes, statisticsFilter.sellerIds).map {
                    transactionsAndItems ->
                val list = arrayListOf<TransactionWithItemAndCategoryUi>()
                for (item in transactionsAndItems) {

                    val datedDate = LocalDate.parse(item.transactionDatedAt)
                    val datedTime = if(item.transactionDatedAtTime == null) {
                        null
                    } else {
                        LocalTime.parse(item.transactionDatedAtTime)
                    }
                    val category = transactionWithCategoryToCategoryUi(item)
                    val images =
                        imageRepository.getTransactionItemImages(item.itemId).blockingGet()
                            .map { image ->
                                Uri.parse(image.uri)
                            }
                    val view = TransactionWithItemAndCategoryUi(
                        item.transactionId,
                        item.itemId,
                        item.accountId,
                        item.transactionTotal,
                        item.itemAmount,
                        item.currencyCode,
                        item.debitOrCredit,
                        item.description,
                        LocalDateTime.parse(item.transactionCreatedAt),
                        datedDate,
                        datedTime,
                        category,
                        images
                    )
                    list.add(view)
                }
                transactionsToTransactionItems(list)
            }.toFlowable().toLiveData()

            statsTransactionAndItemCount = transactionItemRepository.getTransactionItemCountSingle(profile.id!!, statisticsFilter.startDay?.toString(), statisticsFilter.endDay?.toString(), statisticsFilter.accountIds, dates, statisticsFilter.categories, statisticsFilter.modes, statisticsFilter.sellerIds)
                .toFlowable().toLiveData()
        }

    override fun onCleared() {
        val file = File(application.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
        if(file.exists()) {
            val b = file.delete()
            LOGGER.info("Delete statistics JSON file: {}", b)
        }
        super.onCleared()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FilteredTransactionsViewModel::class.java)
    }
}