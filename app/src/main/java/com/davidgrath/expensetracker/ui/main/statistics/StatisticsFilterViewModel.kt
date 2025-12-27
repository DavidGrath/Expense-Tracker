package com.davidgrath.expensetracker.ui.main.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DayOfWeekGsonAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.LocalDateGsonAdapter
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.SellerDb
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.StatisticsFilter
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.repositories.SellerRepository
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import java.io.File
import javax.inject.Inject

class StatisticsFilterViewModel
@Inject
    constructor(
    private val application: Application,
    private val categoryRepository: CategoryRepository,
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
        private val sellerRepository: SellerRepository
) : AndroidViewModel(application) {

    private var statisticsFilter: StatisticsFilter
    private var _statisticsFilterLiveData : MutableLiveData<StatisticsFilter>
    val statisticsFilterLiveData: LiveData<StatisticsFilter>
    val categories: List<CategoryDb>

    val accounts: List<AccountUi>
    val sellers: List<SellerDb>
    lateinit var _weekdays: List<DayOfWeek>
    private val gson = GsonBuilder()
        .registerTypeAdapter(DayOfWeek::class.java, DayOfWeekGsonAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateGsonAdapter())
        .create()
    val profile: ProfileDb

    init {
        val app = application as ExpenseTracker
        profile = app.profileObservable.blockingFirst()
        accounts = accountRepository.getAccountsForProfileSingle(profile.id!!).map { accounts ->
            accounts.map { accountDbToAccountUi(it, timeAndLocaleHandler.getLocale()) }
        }.blockingGet()
        categories = getCategories().blockingGet()
        sellers = sellerRepository.getSellersSingle(profile.id!!).blockingGet()
//        val file = file(application.filesDir, "profiles", profile.stringId, Constants.FILE_NAME_STATS_FILTER_DATA) //TODO Move all files to profiles/<stringId>
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
        _statisticsFilterLiveData = MutableLiveData(statisticsFilter)
        statisticsFilterLiveData = _statisticsFilterLiveData
    }

    fun toggleAccountChecked(accountId: Long) {
        val checkedAccounts = statisticsFilter.accountIds
        val newAccountIds: List<Long>
        if(accountId in checkedAccounts) {
            newAccountIds = checkedAccounts - accountId
        } else {
            if(checkedAccounts.isEmpty()) {
                newAccountIds = listOf(accountId)
            } else {
                val selectedCurrency = checkedAccounts.map { id -> accounts.find { it.id == id }!! }[0].currencyCode
                val newCurrency = accounts.find { it.id == accountId }!!.currencyCode
                if(selectedCurrency != newCurrency) {
                    newAccountIds = listOf(accountId)
                } else {
                    newAccountIds = checkedAccounts + accountId
                }
            }
        }
        statisticsFilter = statisticsFilter.copy(accountIds = newAccountIds)
        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun getCategories(): Single<List<CategoryDb>> {
        return categoryRepository.getCategoriesSingle(profile.id!!)
    }

    fun toggleWeekDay(dayOfWeek: DayOfWeek) {
        val weekDays = statisticsFilter.weekdays
        val newWeekDays = if(dayOfWeek in weekDays) {
            weekDays - dayOfWeek
        } else {
            weekDays + dayOfWeek
        }
        statisticsFilter = statisticsFilter.copy(weekdays = newWeekDays)
        _statisticsFilterLiveData.postValue(statisticsFilter)
    }



    fun toggleCategory(categoryId: Long) {
        val categories = statisticsFilter.categories
        val newCategories = if(categoryId in categories) {
            categories - categoryId
        } else {
            categories + categoryId
        }
        statisticsFilter = statisticsFilter.copy(categories = newCategories)
        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun toggleMode(mode: TransactionMode) {
        val modes = statisticsFilter.modes
        val newModes = if(mode in modes) {
            modes - mode
        } else {
            modes + mode
        }
        statisticsFilter = statisticsFilter.copy(modes = newModes)
        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun toggleSeller(sellerId: Long) {
        val sellers = statisticsFilter.sellerIds
        val newSellers = if(sellerId in sellers) {
            sellers - sellerId
        } else {
            sellers + sellerId
        }
        statisticsFilter = statisticsFilter.copy(sellerIds = newSellers)
        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun selectAll(filterScreen: StatisticsFilterActivity.FilterScreens) {
        when(filterScreen) {
            StatisticsFilterActivity.FilterScreens.Accounts -> {
                val checkedAccounts = statisticsFilter.accountIds
                val newAccountIds: List<Long>

                    if(checkedAccounts.isEmpty()) {
                        val firstCurrencyCode = accounts[0].currencyCode
                        newAccountIds = accounts.filter { it.currencyCode == firstCurrencyCode }.map { it.id }
                    } else {
                        val selectedCurrency = checkedAccounts.map { id -> accounts.find { it.id == id }!! }[0].currencyCode
                        newAccountIds = accounts.filter { it.currencyCode == selectedCurrency }.map { it.id }
                    }

                statisticsFilter = statisticsFilter.copy(accountIds = newAccountIds)
            }
            StatisticsFilterActivity.FilterScreens.Categories -> {
                statisticsFilter = statisticsFilter.copy(categories = categories.map { it.id!! })
            }
            StatisticsFilterActivity.FilterScreens.Sellers -> {
                statisticsFilter = statisticsFilter.copy(sellerIds = sellers.map { it.id!! })
            }
            StatisticsFilterActivity.FilterScreens.Modes -> {
                statisticsFilter = statisticsFilter.copy(modes = TransactionMode.values().toList())
            }
            StatisticsFilterActivity.FilterScreens.Weekdays -> {
                statisticsFilter = statisticsFilter.copy(weekdays = _weekdays)
            }
        }

        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun selectNone(filterScreen: StatisticsFilterActivity.FilterScreens) {
        when(filterScreen) {
            StatisticsFilterActivity.FilterScreens.Accounts -> {
                statisticsFilter = statisticsFilter.copy(accountIds = emptyList())
            }
            StatisticsFilterActivity.FilterScreens.Categories -> {
                statisticsFilter = statisticsFilter.copy(categories = emptyList())
            }
            StatisticsFilterActivity.FilterScreens.Sellers -> {
                statisticsFilter = statisticsFilter.copy(sellerIds = emptyList())
            }
            StatisticsFilterActivity.FilterScreens.Modes -> {
                statisticsFilter = statisticsFilter.copy(modes = emptyList())
            }
            StatisticsFilterActivity.FilterScreens.Weekdays -> {
                statisticsFilter = statisticsFilter.copy(weekdays = emptyList())
            }
        }

        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun invertSelection(filterScreen: StatisticsFilterActivity.FilterScreens) {
        when(filterScreen) {
            StatisticsFilterActivity.FilterScreens.Accounts -> {
                val checkedAccounts = statisticsFilter.accountIds
                val newAccountIds: List<Long>
                if(checkedAccounts.isEmpty()) {
                    val firstCurrencyCode = accounts[0].currencyCode
                    newAccountIds = accounts.filter { it.currencyCode == firstCurrencyCode }.map { it.id }
                } else {
                    val selectedCurrency = checkedAccounts.map { id -> accounts.find { it.id == id }!! }[0].currencyCode
                    val currencyMatchedAccounts = accounts.filter { it.currencyCode == selectedCurrency }
                    val mutableList = mutableListOf<Long>()
                    for(account in currencyMatchedAccounts) {
                        if(account.id !in checkedAccounts) {
                            mutableList += account.id
                        }
                    }
                    newAccountIds = mutableList
                }

                statisticsFilter = statisticsFilter.copy(accountIds = newAccountIds)
            }
            StatisticsFilterActivity.FilterScreens.Categories -> {
                val selectedCategories = statisticsFilter.categories
                val newCategories = mutableListOf<Long>()
                for(category in categories) {
                    if(category.id!! !in selectedCategories) {
                        newCategories += category.id
                    }
                }
                statisticsFilter = statisticsFilter.copy(categories = newCategories)
            }
            StatisticsFilterActivity.FilterScreens.Sellers -> {
                val selectedSellers = statisticsFilter.sellerIds
                val newSellers = mutableListOf<Long>()
                for(seller in sellers) {
                    if(seller.id!! !in selectedSellers) {
                        newSellers += seller.id
                    }
                }
                statisticsFilter = statisticsFilter.copy(sellerIds = newSellers)
            }
            StatisticsFilterActivity.FilterScreens.Modes -> {
                val selectedModes = statisticsFilter.modes
                val newModes = mutableListOf<TransactionMode>()
                for(mode in TransactionMode.values().toList()) {
                    if(mode !in selectedModes) {
                        newModes += mode
                    }
                }
                statisticsFilter = statisticsFilter.copy(modes = newModes)
            }
            StatisticsFilterActivity.FilterScreens.Weekdays -> {
                val selectedWeekdays = statisticsFilter.weekdays
                val newWeekdays = mutableListOf<DayOfWeek>()
                for(weekday in _weekdays) {
                    if(weekday !in selectedWeekdays) {
                        newWeekdays += weekday
                    }
                }
                statisticsFilter = statisticsFilter.copy(weekdays = newWeekdays)
            }
        }

        _statisticsFilterLiveData.postValue(statisticsFilter)
    }

    fun saveStatisticsFilterToFile(): Single<Unit> {
        return Single.fromCallable {
            val file = File(application.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
            val json = gson.toJson(this.statisticsFilter)
            file.writeText(json)
        }.subscribeOn(Schedulers.io())
    }

    override fun onCleared() {
        val file = File(application.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
        if(file.exists()) {
            val b = file.delete()
            LOGGER.info("Delete statistics JSON file: {}", b)
        }
        super.onCleared()
    }

    fun setWeekdays(weekdays: List<DayOfWeek>) {
        this._weekdays = weekdays
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsFilterViewModel::class.java)
    }
}