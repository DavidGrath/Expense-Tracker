package com.davidgrath.expensetracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.ActivityMainBinding
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal

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