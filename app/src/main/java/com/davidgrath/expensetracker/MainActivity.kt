package com.davidgrath.expensetracker

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.databinding.ActivityMainBinding
import com.davidgrath.expensetracker.entities.ui.Transaction
import com.davidgrath.expensetracker.ui.AddTransactionDialogFragment
import com.davidgrath.expensetracker.ui.MainViewModel
import com.davidgrath.expensetracker.ui.TransactionsFragment
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal

class MainActivity : FragmentActivity(), OnClickListener, AddTransactionDialogFragment.AddTransactionListener {

    lateinit var activityMainBinding: ActivityMainBinding
    lateinit var transactionsFragment: TransactionsFragment
    var addTransactionDialog: AddTransactionDialogFragment? = null
    lateinit var viewModel: MainViewModel
    val FRAGMENT_TAG_TRANSACTIONS = "transactions"
    val FRAGMENT_TAG_ADD_TRANSACTION = "addTransaction"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)
        addTransactionDialog = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_ADD_TRANSACTION) as AddTransactionDialogFragment?
        if(savedInstanceState == null) {
            transactionsFragment = TransactionsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_main, transactionsFragment, FRAGMENT_TAG_TRANSACTIONS)
                .show(transactionsFragment)
                .commit()
        } else {
            transactionsFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_TRANSACTIONS) as TransactionsFragment
        }
        activityMainBinding.fabMain.setOnClickListener(this)
        setContentView(activityMainBinding.root)
    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                activityMainBinding.fabMain -> {
                    if(addTransactionDialog == null) {
                        addTransactionDialog = AddTransactionDialogFragment()
                    }
                    addTransactionDialog?.listener = this
                    addTransactionDialog?.show(supportFragmentManager, FRAGMENT_TAG_ADD_TRANSACTION)
                }
                else -> {}
            }
        }
    }

    override fun onAddTransaction(amount: BigDecimal, description: String, category: String) {
        viewModel.addToList((Transaction(1, amount, "USD", description, true, category, ZonedDateTime.now(), ZonedDateTime.now())))
    }
}