package com.davidgrath.expensetracker.ui.main

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.accountWithStatsDbToAccountWithStatsUi
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.itemSumToCategoryUi
import com.davidgrath.expensetracker.transactionWithCategoryToCategoryUi
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AccountWithStatsUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.entities.ui.TransactionWithItemAndCategoryUi
import com.davidgrath.expensetracker.offsetTimeToLocalTime
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionsToTransactionItems
import com.github.mikephil.charting.data.BarEntry
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.math.BigDecimal
import javax.inject.Inject

class MainViewModel
@Inject
constructor(
    private val application: Application,
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val imageRepository: ImageRepository,
    private val categoryRepository: CategoryRepository,
    private val timeHandler: TimeHandler,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository
) : AndroidViewModel(application) {

    val listLiveData: LiveData<List<GeneralTransactionListItem>>
    val statsPastXByCategory: LiveData<List<BarEntry>>
    val statsTotalIncome: LiveData<BigDecimal>
    val statsTotalExpense: LiveData<BigDecimal>
//    val statsTotalByCategory: LiveData<List<BarEntry>>
    val statsTotalIncomeByDay: LiveData<List<DateAmountSummary>>
    val statsTotalExpensesByDay: LiveData<List<DateAmountSummary>>
    val statsTransactionAndItemCount: LiveData<TransactionAndItemCount>
    //TODO For refreshing at midnight
//    private val minuteTicker = Observable.interval(1, TimeUnit.MINUTES)

    var statisticsConfig = StatisticsConfig()
        private set
    private val _statisticsConfigLiveData = MutableLiveData<StatisticsConfig>(statisticsConfig)
    val statisticsConfigLiveData: LiveData<StatisticsConfig> = _statisticsConfigLiveData
    val accountsLiveData: LiveData<List<AccountWithStatsUi>>
    private var currentProfile = -1L

    fun setConfig(statisticsConfig: StatisticsConfig) {
        this.statisticsConfig = statisticsConfig
        _statisticsConfigLiveData.postValue(statisticsConfig)
    }

    init {
        listLiveData = transactionRepository.getTransactions().map{ transactionsAndItems ->
            val list = arrayListOf<TransactionWithItemAndCategoryUi>()
            for (item in transactionsAndItems) {
                val createdDateTime = offsetTimeToLocalTime(timeHandler, item.transactionCreatedAt, item.transactionCreatedAtOffset)
                val datedDate = LocalDate.parse(item.transactionDatedAt)
                val datedDateTime = getLocalDateTime(item)
                val category = transactionWithCategoryToCategoryUi(item)
                val images =
                    imageRepository.getTransactionItemImages(item.itemId).blockingGet()
                        .map { image ->
                            Uri.parse(image.uri)
                        }
                val view = TransactionWithItemAndCategoryUi(item.transactionId, item.itemId, item.accountId, item.transactionTotal, item.itemAmount, item.currencyCode, item.debitOrCredit,
                    item.description, createdDateTime, datedDate, datedDateTime?.toLocalTime(), category, images)
                list.add(view)
            }
            transactionsToTransactionItems(list)
        }.toLiveData()
        statsPastXByCategory = transactionItemRepository.getTotalSpentByCategory().map { list ->
            val mapped = list.mapIndexed { i, it ->
                val cat = itemSumToCategoryUi(it)
//                BarEntry(i.toFloat(), it.second.toFloat(), ResourcesCompat.getDrawable(application.resources, cat.iconId, null))
                BarEntry(
                    i.toFloat(),
                    it.sum.toFloat(),
                    ResourcesCompat.getDrawable(application.resources, cat.iconId, null)
                )
            }
            mapped
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalIncome = transactionRepository.getTotalIncome()
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalExpense = transactionRepository.getTotalExpense()
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
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
        statsTotalExpensesByDay = transactionRepository.getTotalAmountByDate(true, LocalDate.now(timeHandler.getClock()).toString())
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalIncomeByDay = transactionRepository.getTotalAmountByDate(false, LocalDate.now(timeHandler.getClock()).toString())
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTransactionAndItemCount = statisticsConfigLiveData.switchMap {
            val accountIds = statisticsConfig.filter.accountIds
            transactionItemRepository.getTransactionItemCount(LocalDate.now(timeHandler.getClock()).toString(), null, accountIds)
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }

        val preferences = application.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE) //TODO Create profile Observable in Application
        val currentProfileId = preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null)
        val profile = profileRepository.getByStringId(currentProfileId!!).blockingGet()
        currentProfile = profile.id!!
        accountsLiveData = accountRepository.getAccountsWithStatsForProfile(currentProfile).map { list ->
            list.map { accountWithStatsDbToAccountWithStatsUi(it) }
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle()
    }

    fun getAccounts(): Single<List<AccountUi>> {
        return accountRepository.getAccountsForProfileSingle(currentProfile).map { accounts ->
            accounts.map { accountDbToAccountUi(it) }
        }
    }

    fun saveTransaction(accountId: Long, amount: BigDecimal, description: String, categoryId: Long) {
        transactionRepository.addTransaction(accountId, amount, description, categoryId)
            .subscribe({ id -> }, {})
    }

    fun addAccount(name: String, currencyCode: String) {
        val id = accountRepository.createAccount(currentProfile, name, currencyCode).blockingGet()
        LOGGER.info("Created account {} for profile {}", id, currentProfile)
    }

    fun editAccount(accountId: Long, name: String) {
        val count = accountRepository.editAccountName(accountId, name).blockingGet()
//        LOGGER.info("Updated account {} for profile {}", id, currentProfile)
        if(count > 0) {
            LOGGER.info("Updated account {}", accountId)
        } else {
            LOGGER.info("No updates for account {}", accountId)
        }
    }


    fun getLocalDateTime(transactionWithItemAndCategory: TransactionWithItemAndCategory): LocalDateTime? {
        if (transactionWithItemAndCategory.transactionDatedAtTime == null) {
            return null
        }
        val utcDate = LocalDate.parse(transactionWithItemAndCategory.transactionDatedAt)
        val utcTime = LocalTime.parse(transactionWithItemAndCategory.transactionDatedAtTime)
        val utcDateTime = utcDate.atTime(utcTime)
        return offsetTimeToLocalTime(timeHandler, utcDateTime.toString(), transactionWithItemAndCategory.transactionDatedAtOffset!!)
    }

    fun setAccountFilter(accountId: Long) {
        val filter = this.statisticsConfig.filter.copy(accountIds = listOf(accountId))
        this.statisticsConfig = statisticsConfig.copy(filter = filter)
        _statisticsConfigLiveData.postValue(this.statisticsConfig)
        LOGGER.info("setAccountFilter: Used single account ID")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainViewModel::class.java)
    }
}