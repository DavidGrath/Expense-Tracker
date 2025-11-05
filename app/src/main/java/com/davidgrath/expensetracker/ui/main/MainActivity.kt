package com.davidgrath.expensetracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DayOfWeekGsonAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityMainBinding
import com.davidgrath.expensetracker.entities.ui.StatisticsFilter
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragment
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import java.io.File
import javax.inject.Inject

class MainActivity : FragmentActivity(), AccountsFragment.AccountsFragmentListener {

    lateinit var activityMainBinding: ActivityMainBinding
    lateinit var viewModel: MainViewModel
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        app.appComponent.inject(this)
        if(savedInstanceState == null) {
            if(addDetailedTransactionRepository.draftExists()) {
                addDetailedTransactionRepository.restoreDraft().blockingSubscribe() //TODO I think I'll rework the Repository to no longer be a Singleton, this way of restoring the draft feels unnecessary
                if (addDetailedTransactionRepository.isDraftEmpty()) {
                    LOGGER.info("Draft is empty. Removing")
                    addDetailedTransactionRepository.deleteDraft()
                }
            }
        }
        viewModel = ViewModelProvider(this, MainViewModelFactory(app.appComponent)).get(
            MainViewModel::class.java)
        activityMainBinding.viewpagerMain.adapter = MainFragmentStateAdapter(this)
        TabLayoutMediator(activityMainBinding.tabLayoutMain, activityMainBinding.viewpagerMain) { tab, position ->
            when(position) {
                0 -> {
                    tab.text = "Transactions"
                }
                1 -> {
                    tab.text = "Statistics"
                }
                2 -> {
                    tab.text = "Accounts"
                }
            }
        }.attach()
        setContentView(activityMainBinding.root)
    }

    override fun onNavigateToStats() {
        activityMainBinding.viewpagerMain.setCurrentItem(1, true)
    }

    class MainFragmentStateAdapter(mainActivity: MainActivity): FragmentStateAdapter(mainActivity) {
        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            when(position) {
                0 -> {
                    return TransactionsFragment.newInstance()
                }
                1 -> {
                    return StatisticsFragment.newInstance()
                }
                2 -> {
                    return AccountsFragment.newInstance()
                }
                else -> {
                    return TransactionsFragment.newInstance()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CODE_OPEN_FILTER -> {
                if(resultCode == RESULT_OK) {
                    val file = File(application.filesDir, Constants.FILE_NAME_STATS_FILTER_DATA)
                    if(file.exists()) {
                        val size = file.length()
                        val reader = file.bufferedReader()
                        val gson = GsonBuilder().registerTypeAdapter(DayOfWeek::class.java, DayOfWeekGsonAdapter()).create()
                        val statisticsFilter = gson.fromJson(reader, StatisticsFilter::class.java)
                        LOGGER.info("Read {} bytes from existing statistics JSON file", size)
                        viewModel.setFilter(statisticsFilter)
                        val b = file.delete()
                        LOGGER.info("Delete statistics JSON file: {}", b)
                    } else {
                        LOGGER.info("No statistics JSON file found")
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE_OPEN_FILTER = 100
        private val LOGGER = LoggerFactory.getLogger(MainActivity::class.java)
    }
}