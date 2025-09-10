package com.davidgrath.expensetracker.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityMainBinding
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : FragmentActivity() {

    lateinit var activityMainBinding: ActivityMainBinding
    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        viewModel = ViewModelProvider(this, MainViewModelFactory(app.appComponent)).get(
            MainViewModel::class.java)
        activityMainBinding.viewpagerMain.adapter = MainFragmentStateAdapter(this)
        TabLayoutMediator(activityMainBinding.tabLayoutMain, activityMainBinding.viewpagerMain) { tab, position ->
            if(position == 0) {
                tab.text = "Transactions"
            } else {
                tab.text = "Statistics"
            }
        }.attach()
        setContentView(activityMainBinding.root)
    }

    class MainFragmentStateAdapter(mainActivity: MainActivity): FragmentStateAdapter(mainActivity) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            if(position == 0) {
                return TransactionsFragment.newInstance()
            } else {
                return StatisticsFragment.newInstance()
            }
        }
    }
}