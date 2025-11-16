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
import com.davidgrath.expensetracker.DayOfWeekGsonAdapter
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.LocalDateGsonAdapter
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.accountWithStatsDbToAccountWithStatsUi
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.itemSumToCategoryUi
import com.davidgrath.expensetracker.transactionWithCategoryToCategoryUi
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.views.DateAmountSummary
import com.davidgrath.expensetracker.entities.db.views.TransactionAndItemCount
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AccountWithStatsUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.entities.ui.StatisticsFilter
import com.davidgrath.expensetracker.entities.ui.TransactionWithItemAndCategoryUi
import com.davidgrath.expensetracker.getFilteredWeekDays
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.transactionsToTransactionItems
import com.github.mikephil.charting.data.BarEntry
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.Month
import org.threeten.bp.MonthDay
import org.threeten.bp.temporal.TemporalAdjusters
import java.io.File
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
    private val profileRepository: ProfileRepository,
    private val fileHandler: DraftFileHandler
) : AndroidViewModel(application) {

    val statsTotalByCategory: LiveData<Pair<List<BarEntry>, List<BarEntry>>>
    val homeListLiveData: LiveData<List<GeneralTransactionListItem>>
    private lateinit var homeConfig: HomeConfig
    private val _homeConfigLiveData = MutableLiveData<HomeConfig>()
    val homeConfigLiveData : LiveData<HomeConfig> = _homeConfigLiveData
    val homeTotalIncome: LiveData<BigDecimal>
    val homeTotalExpense: LiveData<BigDecimal>
    val statsTotalIncome: LiveData<BigDecimal>
    val statsTotalExpense: LiveData<BigDecimal>
    val statsTotalByDay: LiveData<Pair<List<DateAmountSummary>, List<DateAmountSummary>>>
    val statsTransactionAndItemCount: LiveData<TransactionAndItemCount>
    //TODO For refreshing at midnight
//    private val minuteTicker = Observable.interval(1, TimeUnit.MINUTES)

    var statisticsConfig = StatisticsConfig(timeAndLocaleHandler.getLocale(), filter = StatisticsFilter(startDay = LocalDate.now(timeAndLocaleHandler.getClock()), endDay = LocalDate.now(timeAndLocaleHandler.getClock())))
        private set
    private var pastXLyMode: StatisticsConfig.DateMode? = null
    private val _statisticsConfigLiveData = MutableLiveData<StatisticsConfig>()
    val statisticsConfigLiveData: LiveData<StatisticsConfig> = _statisticsConfigLiveData
    val accountWithStatsLiveData: LiveData<List<AccountWithStatsUi>>
    val accountsLiveData: LiveData<List<AccountUi>>
    private val gson = GsonBuilder()
        .registerTypeAdapter(DayOfWeek::class.java, DayOfWeekGsonAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateGsonAdapter())
        .create()
    var currentProfile = -1L
    private set

    init {
        val app = application as ExpenseTracker
        val profile = app.profileObservable.blockingFirst()
        currentProfile = profile.id!!
        val profilePreferences = application.getSharedPreferences(profile.stringId, Context.MODE_PRIVATE)
        val homeAccountId = profilePreferences.getLong(Constants.PreferenceKeys.Profile.DEFAULT_ACCOUNT_ID, -1) //TODO Use Spinner
        homeConfig = HomeConfig(homeAccountId,
            LocalDate.now(timeAndLocaleHandler.getClock()),
            LocalDate.now(timeAndLocaleHandler.getClock())
        )
        _homeConfigLiveData.postValue(homeConfig)
        statisticsConfig = statisticsConfig.copy(filter = statisticsConfig.filter.copy(accountIds = listOf(homeAccountId)))
        _statisticsConfigLiveData.postValue(statisticsConfig)
        homeListLiveData = homeConfigLiveData.switchMap {
            transactionRepository.getTransactions(
                profile.id, it.accountId, it.startDate.toString(), it.endDate.toString()
//                profile.id, it.accountId, it.startDate.toString(), it.endDate.toString()
            ).map { transactionsAndItems ->
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
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
        statsTotalByCategory = statisticsConfigLiveData.switchMap {
            val accountIds = it.filter.accountIds
            val dates = getFilteredWeekDays(profile.id, it.filter, it.dateMode, timeAndLocaleHandler, transactionRepository)
            val categories = it.filter.categories
            val modes = it.filter.modes
            val sellers = it.filter.sellerIds
            Observable.combineLatest(
                transactionItemRepository.getTotalExpenseByCategory(profile.id!!, it.filter.startDay?.toString(), it.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers),
                transactionItemRepository.getTotalIncomeByCategory(profile.id!!, it.filter.startDay?.toString(), it.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers)
            ) { expenseList, incomeList ->
                val categoryIds =
                    (expenseList.map { it.categoryId } + incomeList.map { it.categoryId })
                        .distinct().sorted()
                val size = categoryIds.size
                LOGGER.info("statsTotalByCategory: using {} distinct categories", size)
                val expenseEntries = ArrayList<BarEntry>(size)
                val incomeEntries = ArrayList<BarEntry>(size)
                categoryIds.forEachIndexed { index, id ->
                    val expenseIndex = expenseList.indexOfFirst { it.categoryId == id }
                    val incomeIndex = incomeList.indexOfFirst { it.categoryId == id }
                    val cat = itemSumToCategoryUi(if(expenseIndex >= 0) {
                        expenseList[expenseIndex]
                    } else {
                        incomeList[incomeIndex]
                    })
                    if(expenseIndex >= 0) {
                        val expenseSum = expenseList[expenseIndex].sum.toFloat()
                        expenseEntries.add(BarEntry(index.toFloat(), expenseSum, ResourcesCompat.getDrawable(application.resources, cat.iconId, null)))
                    }
                    if(incomeIndex >= 0) {
                        val incomeSum = incomeList[incomeIndex].sum.toFloat()
                        incomeEntries.add(BarEntry(index.toFloat(), incomeSum, ResourcesCompat.getDrawable(application.resources, cat.iconId, null)))
                    }
                }
                /*val expenseMapped = expenseList.mapIndexed { i, it ->
                    val cat = itemSumToCategoryUi(it)
                    BarEntry(
                        i.toFloat(),
                        it.sum.toFloat(),
                        ResourcesCompat.getDrawable(application.resources, cat.iconId, null)
                    )
                }
                val incomeMapped = incomeList.mapIndexed { i, it ->
                    val cat = itemSumToCategoryUi(it)
                    BarEntry(
                        i.toFloat(),
                        it.sum.toFloat(),
                        ResourcesCompat.getDrawable(application.resources, cat.iconId, null)
                    )
                }*/
//                expenseMapped to incomeMapped
                (expenseEntries as List<BarEntry>) to (incomeEntries as List<BarEntry>)
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }

        homeTotalIncome = homeConfigLiveData.switchMap {
            val accountIds = listOf(it.accountId)
            transactionRepository.getTotalIncome(profile.id!!,
                it.startDate.toString(),
                it.endDate.toString(),
                accountIds, emptyList(), emptyList(), emptyList(), emptyList()
            )
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
        homeTotalExpense = homeConfigLiveData.switchMap {
            val accountIds = listOf(it.accountId)
            transactionRepository.getTotalExpense(profile.id!!,
                it.startDate.toString(),
                it.endDate.toString(),
                accountIds, emptyList(), emptyList(), emptyList(), emptyList()
            )
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
        statsTotalIncome = statisticsConfigLiveData.switchMap {
            val accountIds = it.filter.accountIds
            val dates = getFilteredWeekDays(profile.id, it.filter, it.dateMode, timeAndLocaleHandler, transactionRepository)
            val categories = it.filter.categories
            val modes = it.filter.modes
            val sellers = it.filter.sellerIds
            transactionRepository.getTotalIncome(profile.id!!,
                it.filter.startDay?.toString(),
                it.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers
            )
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
        statsTotalExpense = statisticsConfigLiveData.switchMap {
            val accountIds = it.filter.accountIds
            val dates = getFilteredWeekDays(profile.id, it.filter, it.dateMode, timeAndLocaleHandler, transactionRepository)
            val categories = it.filter.categories
            val modes = it.filter.modes
            val sellers = it.filter.sellerIds
            transactionRepository.getTotalExpense(profile.id!!,
                it.filter.startDay?.toString(),
                it.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers
            )
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
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
        statsTotalByDay = statisticsConfigLiveData.switchMap {

            val accountIds = it.filter.accountIds
            val dates = getFilteredWeekDays(profile.id, it.filter, it.dateMode, timeAndLocaleHandler, transactionRepository)
            val categories = it.filter.categories
            val modes = it.filter.modes
            val sellers = it.filter.sellerIds
            Observable.combineLatest(
                transactionRepository.getTotalAmountByDate(profile.id, true, statisticsConfig.filter.startDay?.toString(), statisticsConfig.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers),
                transactionRepository.getTotalAmountByDate(profile.id, false, statisticsConfig.filter.startDay?.toString(), statisticsConfig.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers)
            ) { expenses, income ->
                expenses to income
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }
        statsTransactionAndItemCount = statisticsConfigLiveData.switchMap {
            val accountIds = it.filter.accountIds
            val dates = getFilteredWeekDays(profile.id, it.filter, it.dateMode, timeAndLocaleHandler, transactionRepository)
            val categories = it.filter.categories
            val modes = it.filter.modes
            val sellers = it.filter.sellerIds
            transactionItemRepository.getTransactionItemCount(profile.id!!, statisticsConfig.filter.startDay?.toString(), statisticsConfig.filter.endDay?.toString(), accountIds, dates, categories, modes, sellers)
                .toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        }



        accountWithStatsLiveData = accountRepository.getAccountsWithStatsForProfile(currentProfile).map { list ->
            list.map { accountWithStatsDbToAccountWithStatsUi(it, timeAndLocaleHandler.getLocale()) }
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
        accountsLiveData = accountRepository.getAccountsForProfile(currentProfile).map { list ->
            list.map { account ->
                accountDbToAccountUi(account, timeAndLocaleHandler.getLocale())
            }
        }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()
    }

    fun saveTransaction(accountId: Long, debitOrCredit: Boolean, amount: BigDecimal, description: String, categoryId: Long) {
        transactionRepository.addTransaction(accountId, debitOrCredit, amount, description, categoryId)
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

    fun setFilter(statisticsFilter: StatisticsFilter) {
        this.statisticsConfig = statisticsConfig.copy(filter = statisticsFilter)
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
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter, xLyOffset = -daysToSubtract)

            }
            StatisticsConfig.DateMode.PastXDays -> {
                val daysToSubtract = statisticsConfig.xDays - 1
                val startDate = LocalDate.now(timeAndLocaleHandler.getClock()).minusDays(daysToSubtract.toLong())
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
            }
            StatisticsConfig.DateMode.PastWeek -> {
                val startDate = LocalDate.now(timeAndLocaleHandler.getClock()).minusDays(6)
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
            }
            StatisticsConfig.DateMode.Weekly -> {
                if(pastXLyMode != dateMode) {
                    statisticsConfig = statisticsConfig.copy(xLyOffset = 0)
                }
                val weeksToSubtract = -statisticsConfig.xLyOffset
                val startDateWeek = LocalDate.now(timeAndLocaleHandler.getClock()).minusWeeks(weeksToSubtract.toLong())
                val firstDayOfWeek = statisticsConfig.weeklyFirstDay
                val startDate = startDateWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

                val lastDayOfWeek = firstDayOfWeek.plus(6)
                var possibleEndDate = startDateWeek.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))
                val today = LocalDate.now(timeAndLocaleHandler.getClock())
                if(possibleEndDate > today) {
                   possibleEndDate = today
                }
                val endDate = possibleEndDate
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
            }
            StatisticsConfig.DateMode.PastMonth -> {
                val startDateMonth = LocalDate.now(timeAndLocaleHandler.getClock()).minusMonths(1)
                val startDate = startDateMonth.plusDays(1)
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
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
                    val month = startDateMonth.month
                    val maxDayInMonth = month.maxLength()
                    var day = statisticsConfig.monthlyDayOfMonth
                    if(month == Month.FEBRUARY) {
                        if(startDateMonth.isLeapYear) {
                            LOGGER.info("isLeapYear, using 29")
                            day = maxDayInMonth
                        } else {
                            LOGGER.info("isLeapYear false, using 28")
                            day = 28
                        }
                    } else if(day > maxDayInMonth){
                        LOGGER.info("monthlyDayOfMonth {} is greater than current month maximum {}, reducing", day, maxDayInMonth)
                        day = maxDayInMonth
                    }
                    var possibleStartDate = startDateMonth.withDayOfMonth(day)
                    if(possibleStartDate > today) {
                        possibleStartDate = possibleStartDate.minusMonths(1)
                    }
//                    startDate = startDateMonth.withDayOfMonth(day)
                    startDate = possibleStartDate
                    var possibleEndDate = startDate.plusMonths(1).minusDays(1)
                    if(possibleEndDate > today) {
                        possibleEndDate = today
                    }
                    endDate = possibleEndDate
                }
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
            }
            StatisticsConfig.DateMode.PastYear -> {
                val startDateMonth = LocalDate.now(timeAndLocaleHandler.getClock()).minusYears(1)
                val startDate = startDateMonth.plusDays(1)
                val endDate = LocalDate.now(timeAndLocaleHandler.getClock())
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
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
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
            }
            StatisticsConfig.DateMode.Range -> {
                var startDate = statisticsConfig.filter.startDay
                var endDate = statisticsConfig.filter.endDay
                if(startDate != null && startDate == endDate) { //Previous mode was Daily or PastXDays with 1
                    startDate = null
                    endDate = null
                }
                val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
                statisticsConfig = statisticsConfig.copy(filter = filter)
            }
            StatisticsConfig.DateMode.All -> {
                val filter = statisticsConfig.filter.copy(startDay = null, endDay = null)
                statisticsConfig = statisticsConfig.copy(filter = filter)
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
        val day = if(dayOfMonth > 31) {
            LOGGER.info("setMonthlyDayOfMonth: dayOfMonth cannot be greater than 31. Changing")
            31
        } else {
            dayOfMonth
        }
        this.statisticsConfig = this.statisticsConfig.copy(monthlyDayOfMonth = day)
        setDateMode(this.statisticsConfig.dateMode)
        LOGGER.info("setMonthlyDayOfMonth: changed")
    }

    fun setMonthDayOfYear(monthDay: MonthDay) {
        this.statisticsConfig = this.statisticsConfig.copy(monthDayOfYear = monthDay)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        val filter = statisticsConfig.filter.copy(startDay = startDate, endDay = endDate)
        statisticsConfig = statisticsConfig.copy(filter = filter)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setFilterWeekDays(weekdays: List<DayOfWeek>) {
        val filter = this.statisticsConfig.filter.copy(weekdays = weekdays)
        this.statisticsConfig = statisticsConfig.copy(filter = filter)
        _statisticsConfigLiveData.postValue(this.statisticsConfig)
        setDateMode(this.statisticsConfig.dateMode)
        LOGGER.info("setFilterWeekDays")
    }


    fun setCategories(categories: List<Long>) {
        val filter = this.statisticsConfig.filter.copy(categories = categories)
        this.statisticsConfig = statisticsConfig.copy(filter = filter)
        _statisticsConfigLiveData.postValue(this.statisticsConfig)
        setDateMode(this.statisticsConfig.dateMode)
        LOGGER.info("setWeekDays")
    }

    fun saveStatisticsFilterToFile(): Single<Unit> {
        return Single.fromCallable {
            val file = File(application.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
            val json = gson.toJson(this.statisticsConfig.filter)
            file.writeText(json)
        }.subscribeOn(Schedulers.io())
    }

    fun incrementXLyOffset() {
        val offset = statisticsConfig.xLyOffset
        if(offset + 1 > 0) {
            LOGGER.info("xLyOffset capped at 0")
        } else {
            this.statisticsConfig = this.statisticsConfig.copy(xLyOffset = offset + 1)
            setDateMode(this.statisticsConfig.dateMode)
        }

    }

    fun decrementXLyOffset() {
        val offset = statisticsConfig.xLyOffset
        val newOffset = offset - 1
        this.statisticsConfig = this.statisticsConfig.copy(xLyOffset = newOffset)
        setDateMode(this.statisticsConfig.dateMode)
    }

    fun setFirstWeekDay(weekday: DayOfWeek) {
        this.statisticsConfig = statisticsConfig.copy(weeklyFirstDay = weekday)
        _statisticsConfigLiveData.postValue(this.statisticsConfig)
        setDateMode(this.statisticsConfig.dateMode)
        LOGGER.info("setFirstWeekDay: changed")
    }

    fun setHomeAccountId(accountId: Long) {
        this.homeConfig = this.homeConfig.copy(accountId = accountId)
        _homeConfigLiveData.postValue(homeConfig)
    }

    fun setHomeDateRange(startDate: LocalDate, endDate: LocalDate) {
        this.homeConfig = this.homeConfig.copy(startDate = startDate, endDate = endDate)
        _homeConfigLiveData.postValue(homeConfig)
    }

    fun incrementHomeDay() {
        if(homeConfig.startDate.compareTo(homeConfig.endDate) != 0) {
            LOGGER.info("incrementHomeDay: Start and end date not the same. Will not increment")
            return
        }
        val date = homeConfig.startDate
        val today = LocalDate.now(timeAndLocaleHandler.getClock())
        if(date.plusDays(1).isAfter(today)) {
            LOGGER.info("Home day capped at today")
        } else {
            this.homeConfig = this.homeConfig.copy(startDate = date.plusDays(1), endDate = date.plusDays(1))
            _homeConfigLiveData.postValue(this.homeConfig)
        }

    }

    fun decrementHomeDay() {
        if(homeConfig.startDate.compareTo(homeConfig.endDate) != 0) {
            LOGGER.info("decrementHomeDay: Start and end date not the same. Will not decrement")
            return
        }
        val date = homeConfig.endDate
        this.homeConfig = this.homeConfig.copy(startDate = date.minusDays(1), endDate = date.minusDays(1))
        _homeConfigLiveData.postValue(this.homeConfig)
    }

    fun doesDraftExist(): Boolean {
        return fileHandler.draftExists()
    }


    data class HomeConfig(
        val accountId: Long,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainViewModel::class.java)
    }
}