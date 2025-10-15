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
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
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
import org.threeten.bp.MonthDay
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.WeekFields
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
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
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

    var statisticsConfig = StatisticsConfig(timeAndLocaleHandler.getLocale())
        private set
    private var pastXLyMode: StatisticsConfig.DateMode? = null
    private val _statisticsConfigLiveData = MutableLiveData<StatisticsConfig>(statisticsConfig)
    val statisticsConfigLiveData: LiveData<StatisticsConfig> = _statisticsConfigLiveData
    val accountsLiveData: LiveData<List<AccountWithStatsUi>>
    private var currentProfile = -1L

    init {
        listLiveData = transactionRepository.getTransactions().map{ transactionsAndItems ->
            val list = arrayListOf<TransactionWithItemAndCategoryUi>()
            for (item in transactionsAndItems) {
                val createdDateTime = offsetTimeToLocalTime(timeAndLocaleHandler, item.transactionCreatedAt, item.transactionCreatedAtOffset)
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
        statsTotalIncome = transactionRepository.getTotalIncome(statisticsConfig.rangeStartDay?.toString(), statisticsConfig.rangeEndDay?.toString())
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalExpense = transactionRepository.getTotalExpense(statisticsConfig.rangeStartDay?.toString(), statisticsConfig.rangeEndDay?.toString())
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
        statsTotalExpensesByDay = transactionRepository.getTotalAmountByDate(true, statisticsConfig.rangeStartDay?.toString()?: LocalDate.now(timeAndLocaleHandler.getClock()).toString(), statisticsConfig.rangeEndDay?.toString())
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTotalIncomeByDay = transactionRepository.getTotalAmountByDate(false, statisticsConfig.rangeStartDay?.toString()?: LocalDate.now(timeAndLocaleHandler.getClock()).toString(), statisticsConfig.rangeEndDay?.toString())
            .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        statsTransactionAndItemCount = statisticsConfigLiveData.switchMap {
            val accountIds = statisticsConfig.filter.accountIds
            transactionItemRepository.getTransactionItemCount(statisticsConfig.rangeStartDay?.toString(), statisticsConfig.rangeEndDay?.toString(), accountIds)
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }

        val preferences = application.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE) //TODO Create profile Observable in Application
        val currentProfileId = preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null)
        val profile = profileRepository.getByStringId(currentProfileId!!).blockingGet()
        currentProfile = profile.id!!
        accountsLiveData = accountRepository.getAccountsWithStatsForProfile(currentProfile).map { list ->
            list.map { accountWithStatsDbToAccountWithStatsUi(it, timeAndLocaleHandler.getLocale()) }
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle()
    }

    fun getAccounts(): Single<List<AccountUi>> {
        return accountRepository.getAccountsForProfileSingle(currentProfile).map { accounts ->
            accounts.map { accountDbToAccountUi(it, timeAndLocaleHandler.getLocale()) }
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
        return offsetTimeToLocalTime(timeAndLocaleHandler, utcDateTime.toString(), transactionWithItemAndCategory.transactionDatedAtOffset!!)
    }

    fun setAccountFilter(accountId: Long) {
        val filter = this.statisticsConfig.filter.copy(accountIds = listOf(accountId))
        this.statisticsConfig = statisticsConfig.copy(filter = filter)
        _statisticsConfigLiveData.postValue(this.statisticsConfig)
        LOGGER.info("setAccountFilter: Used single account ID")
    }

    fun setConfig(statisticsConfig: StatisticsConfig) {
        this.statisticsConfig = statisticsConfig
        _statisticsConfigLiveData.postValue(statisticsConfig)
    }

    fun setDateMode(dateMode: StatisticsConfig.DateMode) {
        if(statisticsConfig.dateMode in listOf(StatisticsConfig.DateMode.Daily, StatisticsConfig.DateMode.Weekly,
                StatisticsConfig.DateMode.Monthly, StatisticsConfig.DateMode.Yearly )/* && statisticsConfig.dateMode != dateMode*/) {
            pastXLyMode = statisticsConfig.dateMode
        }
        when(dateMode) {
            StatisticsConfig.DateMode.Daily -> {
                if(pastXLyMode != dateMode) {
                    statisticsConfig = statisticsConfig.copy(xLyOffset = 0)
                }
                var daysToSubtract = statisticsConfig.xLyOffset
                if(daysToSubtract > 0) {
                    daysToSubtract = 0 //Don't use the future
                }
                daysToSubtract *= -1
                val startDate = LocalDate.now(timeAndLocaleHandler.getClock()).minusDays(daysToSubtract.toLong())
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock()).minusDays(daysToSubtract.toLong())
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate, xLyOffset = -daysToSubtract)

            }
            StatisticsConfig.DateMode.PastXDays -> {
                val daysToSubtract = statisticsConfig.xDays - 1
                val startDate = LocalDate.now(timeAndLocaleHandler.getClock()).minusDays(daysToSubtract.toLong())
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.PastWeek -> {
                val startDate = LocalDate.now(timeAndLocaleHandler.getClock()).minusDays(6)
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.Weekly -> {
                if(pastXLyMode != dateMode) {
                    statisticsConfig = statisticsConfig.copy(xLyOffset = 0)
                }
                val weeksToSubtract = -statisticsConfig.xLyOffset
                val startDateWeek = LocalDate.now(timeAndLocaleHandler.getClock()).minusWeeks(weeksToSubtract.toLong())
                val firstDayOfWeek = WeekFields.of(timeAndLocaleHandler.getLocale()).firstDayOfWeek
                val startDate = startDateWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

                val lastDayOfWeek = firstDayOfWeek.plus(6)
                var possibleEndDate = startDateWeek.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))
                val today = LocalDate.now(timeAndLocaleHandler.getClock())
                if(possibleEndDate > today) {
                   possibleEndDate = today
                }
                val endDate = possibleEndDate
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.PastMonth -> {
                val startDateMonth = LocalDate.now(timeAndLocaleHandler.getClock()).minusMonths(1)
                val startDate = startDateMonth.plusDays(1)
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.Monthly -> {
                if(pastXLyMode != dateMode && statisticsConfig.dateMode != dateMode) {
                    statisticsConfig = statisticsConfig.copy(xLyOffset = 0)
                }
                val monthsToSubtract = -statisticsConfig.xLyOffset
                val startDateMonth = LocalDate.now(timeAndLocaleHandler.getClock()).minusMonths(monthsToSubtract.toLong())
                val startDate: LocalDate
                val endDate: LocalDate
                val today = LocalDate.now(timeAndLocaleHandler.getClock())
                if(statisticsConfig.monthlyDayOfMonth == 1) {
                    startDate = startDateMonth.with(TemporalAdjusters.firstDayOfMonth())

                    var possibleEndDate = startDateMonth.with(TemporalAdjusters.lastDayOfMonth())
                    if (possibleEndDate > today) {
                        possibleEndDate = today
                    }
                    endDate = possibleEndDate
                } else {
                    startDate = startDateMonth.withDayOfMonth(statisticsConfig.monthlyDayOfMonth)
                    var possibleEndDate = startDate.plusMonths(1).minusDays(1)
                    if(possibleEndDate > today) {
                        possibleEndDate = today
                    }
                    endDate = possibleEndDate
                }
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.PastYear -> {
                val startDateMonth = LocalDate.now(timeAndLocaleHandler.getClock()).minusYears(1)
                val startDate = startDateMonth.plusDays(1)
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.Yearly -> {
                if(pastXLyMode != dateMode) {
                    statisticsConfig = statisticsConfig.copy(xLyOffset = 0)
                }
                val today = LocalDate.now(timeAndLocaleHandler.getClock())
                val yearsToSubtract = -statisticsConfig.xLyOffset
                val startDateYear = LocalDate.now(timeAndLocaleHandler.getClock()).minusYears(yearsToSubtract.toLong())
                val startDate = startDateYear.with(statisticsConfig.monthDayOfYear)
                var possibleEndDate = startDate.plusYears(1).minusDays(1)
                if(possibleEndDate > today) {
                    possibleEndDate = today
                }
                val endDate = possibleEndDate
                statisticsConfig = statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
            }
            StatisticsConfig.DateMode.Range -> {
                // No need to do anything, I guess
            }
            StatisticsConfig.DateMode.All -> {
                statisticsConfig = statisticsConfig.copy(rangeStartDay = null, rangeEndDay = null)
            }
        }
        statisticsConfig = statisticsConfig.copy(dateMode = dateMode)
        _statisticsConfigLiveData.postValue(statisticsConfig)
    }

    fun setXDaysPast(x: Int) {
        val xDays = if(x < 1) {
            LOGGER.info("xDays cannot be less than 1. Changed to 1")
            1
        } else {
            x
        }
        this.statisticsConfig = this.statisticsConfig.copy(xDays = xDays)
        LOGGER.debug("setXDaysPast: statisticsConfig: {}", statisticsConfig)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setXLyOffset(offset: Int) {
        this.statisticsConfig = this.statisticsConfig.copy(xLyOffset = offset)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setMonthlyDayOfMonth(dayOfMonth: Int) {
        this.statisticsConfig = this.statisticsConfig.copy(monthlyDayOfMonth = dayOfMonth)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setMonthDayOfYear(monthDay: MonthDay) {
        this.statisticsConfig = this.statisticsConfig.copy(monthDayOfYear = monthDay)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        this.statisticsConfig = this.statisticsConfig.copy(rangeStartDay = startDate, rangeEndDay = endDate)
        setDateMode(this.statisticsConfig.dateMode)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainViewModel::class.java)
    }
}