package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.ActivityStatisticsFilterBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsEvidenceFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsItemsFragment
import com.google.android.material.tabs.TabLayoutMediator
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.WeekFields
import javax.inject.Inject

class StatisticsFilterActivity: AppCompatActivity(), OnClickListener {

    lateinit var binding: ActivityStatisticsFilterBinding
    lateinit var viewModel: StatisticsFilterViewModel

    private var statsUsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsFilterBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        val appComponent = app.appComponent
        viewModel = ViewModelProvider(this, StatisticsFilterViewModelFactory(appComponent)).get(
            StatisticsFilterViewModel::class.java)

        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return FilterScreens.values().size
            }

            override fun createFragment(position: Int): Fragment {
                when(position) {
                    FilterScreens.Accounts.position -> {
                        return StatisticsFilterAccountsFragment.newInstance()
                    }
                    FilterScreens.Weekdays.position -> {
                        return StatisticsFilterWeekdaysFragment.newInstance()
                    }
                    FilterScreens.Categories.position -> {
                        return StatisticsFilterCategoriesFragment.newInstance()
                    }
                    FilterScreens.Modes.position -> {
                        return StatisticsFilterModesFragment.newInstance()
                    }
                    FilterScreens.Sellers.position -> {
                        return StatisticsFilterSellersFragment.newInstance()
                    }
                    else -> {
                        return StatisticsFilterAccountsFragment.newInstance()
                    }
                }
            }
        }
        binding.viewPagerStatisticsFilter.adapter = pagerAdapter
        val tabLayoutMediator = TabLayoutMediator(binding.tabLayoutStatisticsFilter, binding.viewPagerStatisticsFilter) { tab, position ->
            when(position) {
                FilterScreens.Accounts.position -> {
                    tab.text = "Accounts"
                }
                FilterScreens.Weekdays.position -> {
                    tab.text = "Weekdays"
                }
                FilterScreens.Categories.position -> {
                    tab.text = "Categories"
                }
                FilterScreens.Modes.position -> {
                    tab.text = "Modes"
                }
                FilterScreens.Sellers.position -> {
                    tab.text = "Sellers"
                }
            }
        }
        tabLayoutMediator.attach()
        binding.linearLayoutStatisticsFilterSelectionSectionAll.setOnClickListener(this)
        binding.linearLayoutStatisticsFilterSelectionSectionNone.setOnClickListener(this)
        binding.linearLayoutStatisticsFilterSelectionSectionInvert.setOnClickListener(this)

        binding.fabStatisticsFilterDone.setOnClickListener(this)
        setContentView(binding.root)
    }

    override fun onClick(v: View?) {
        when(v) {
            binding.fabStatisticsFilterDone -> {
                statsUsed = true
                LOGGER.info("Done with statistics")
                viewModel.saveStatisticsFilterToFile().blockingSubscribe()
                setResult(RESULT_OK)
                finish()
            }
            binding.linearLayoutStatisticsFilterSelectionSectionAll -> {
                val selectedTab = binding.tabLayoutStatisticsFilter.selectedTabPosition
                viewModel.selectAll(positionToFilterScreen(selectedTab))
            }
            binding.linearLayoutStatisticsFilterSelectionSectionNone -> {
                val selectedTab = binding.tabLayoutStatisticsFilter.selectedTabPosition
                viewModel.selectNone(positionToFilterScreen(selectedTab))
            }
            binding.linearLayoutStatisticsFilterSelectionSectionInvert -> {
                val selectedTab = binding.tabLayoutStatisticsFilter.selectedTabPosition
                viewModel.invertSelection(positionToFilterScreen(selectedTab))
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

    fun positionToFilterScreen(position: Int): FilterScreens {
        val screen = FilterScreens.values().firstOrNull { it.position == position }
        return screen?: FilterScreens.Accounts
    }

    enum class FilterScreens(val position: Int) {
        Accounts(0),
        Categories(1),
        Sellers(2),
        Modes(3),
        Weekdays(4)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsFilterActivity::class.java)
    }
}