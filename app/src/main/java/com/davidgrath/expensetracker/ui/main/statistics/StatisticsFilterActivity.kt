package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.ActivityStatisticsFilterBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.WeekFields
import javax.inject.Inject

class StatisticsFilterActivity: AppCompatActivity(), OnClickListener {

    lateinit var binding: ActivityStatisticsFilterBinding
    lateinit var viewModel: StatisticsViewModel
    private var accountsMap = mutableMapOf<Long, CheckBox>()
    private var weekDaysMap = mutableMapOf<DayOfWeek, CheckBox>()
    private var categoriesMap = mutableMapOf<Long, CheckBox>()
    private var statsUsed = false
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsFilterBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)
        viewModel = ViewModelProvider(this, StatisticsViewModelFactory(appComponent)).get(
            StatisticsViewModel::class.java)

        val accounts = viewModel.accounts
        accounts.forEach { account ->
            val linearLayout = binding.linearLayoutStatisticsFilterAccounts
            val checkbox = CheckBox(this)
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = "${account.name} (${account.currencyCode})"
            linearLayout.addView(checkbox)
            accountsMap[account.id] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleAccountChecked(account.id)
            }
        }
        val categories = viewModel.getCategories().blockingGet().map { categoryDbToCategoryUi(it) }
        categories.forEach { cat ->
            val linearLayout = binding.linearLayoutStatisticsFilterCategories
            val checkbox = CheckBox(this)
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = cat.name
            linearLayout.addView(checkbox)
            categoriesMap[cat.id] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleCategory(cat.id)
            }
        }
        val firstDay = WeekFields.of(timeAndLocaleHandler.getLocale()).firstDayOfWeek
        for(weekdayInt in 0L..6L) {
            val weekDay = firstDay.plus(weekdayInt)
            val linearLayout = binding.linearLayoutStatisticsFilterWeekdays
            val checkbox = CheckBox(this)
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = weekDay.toString() //TODO Context and String IDs
            linearLayout.addView(checkbox)
            weekDaysMap[weekDay] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleWeekDay(weekDay)
            }
        }
        viewModel.statisticsFilterLiveData.observe(this) { filter ->
            val accountIds = filter.accountIds
            for((id, cb) in accountsMap) {
                cb.isChecked = id in accountIds
            }
            val weekDays = filter.weekdays
            for((weekDay, cb) in weekDaysMap) {
                cb.isChecked = weekDay in weekDays
            }
            val categories = filter.categories
            for((catId, cb) in categoriesMap) {
                cb.isChecked = catId in categories
            }
        }
        binding.imageViewStatisticsFilterDone.setOnClickListener(this)
        setContentView(binding.root)
    }

    override fun onClick(v: View?) {
        when(v) {
            binding.imageViewStatisticsFilterDone -> {
                statsUsed = true
                LOGGER.info("Done with statistics")
                viewModel.saveStatisticsFilterToFile().blockingSubscribe()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isFinishing) {
            if(statsUsed) {
                LOGGER.info("Statistics sent back to calling activity")
            } else {
                LOGGER.info("Statistics discarded")
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsFilterActivity::class.java)
    }
}