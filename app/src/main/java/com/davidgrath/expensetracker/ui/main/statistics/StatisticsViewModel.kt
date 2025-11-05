package com.davidgrath.expensetracker.ui.main.statistics

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DayOfWeekGsonAdapter
import com.davidgrath.expensetracker.UriTypeAdapter
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.StatisticsFilter
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import java.io.File
import javax.inject.Inject

class StatisticsViewModel
@Inject
    constructor(
    private val application: Application,
    private val categoryRepository: CategoryRepository,
    private val timeAndLocaleHandler: TimeAndLocaleHandler,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository
) : AndroidViewModel(application) {

    private var currentProfile = -1L
    private var statisticsFilter: StatisticsFilter
    private var _statisticsFilterLiveData : MutableLiveData<StatisticsFilter>
    val statisticsFilterLiveData: LiveData<StatisticsFilter>
    val accounts: List<AccountUi>
    private val gson = GsonBuilder().registerTypeAdapter(DayOfWeek::class.java, DayOfWeekGsonAdapter()).create()

    init {
        val preferences = application.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE) //TODO Create profile Observable in Application
        val currentProfileId = preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null)
        val profile = profileRepository.getByStringId(currentProfileId!!).blockingGet()
        currentProfile = profile.id!!
        accounts = accountRepository.getAccountsForProfileSingle(currentProfile).map { accounts ->
            accounts.map { accountDbToAccountUi(it, timeAndLocaleHandler.getLocale()) }
        }.blockingGet()
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
        return categoryRepository.getCategoriesSingle()
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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsViewModel::class.java)
    }
}