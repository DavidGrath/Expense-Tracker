package com.davidgrath.expensetracker.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityMainBinding
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragment
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : FragmentActivity(), AccountsFragment.AccountsFragmentListener {

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
}