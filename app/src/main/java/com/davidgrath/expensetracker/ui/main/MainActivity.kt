package com.davidgrath.expensetracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityMainBinding
import com.davidgrath.expensetracker.entities.db.PurchaseItemDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal

class MainActivity : FragmentActivity(), OnClickListener, OnLongClickListener, AddTransactionDialogFragment.AddTransactionListener {

    lateinit var activityMainBinding: ActivityMainBinding
    lateinit var transactionsFragment: TransactionsFragment
    var addTransactionDialog: AddTransactionDialogFragment? = null
    lateinit var viewModel: MainViewModel
    val FRAGMENT_TAG_TRANSACTIONS = "transactions"
    val FRAGMENT_TAG_ADD_TRANSACTION = "addTransaction"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(
            MainViewModel::class.java)
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
        activityMainBinding.fabMain.setOnLongClickListener(this)
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

    override fun onLongClick(v: View?): Boolean {
        v?.let {
            when(v) {
                activityMainBinding.fabMain -> {
                    val intent = Intent(this, AddDetailedTransactionActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        return true
    }

    override fun onAddTransaction(amount: BigDecimal, description: String, categoryId: Int) {
        val app = application as ExpenseTracker
        val category = app.tempDb.categories.find { it.id.toInt() == categoryId }!!

        var transaction = TransactionDb(1, amount, "USD", true, ZonedDateTime.now(), ZonedDateTime.now())
        app.addTransaction(transaction)
        val purchaseItem = PurchaseItemDb(transaction.id, amount, description, category.id)
        app.addPurchaseItem(purchaseItem)
    }

    override fun onGoToDetails(amount: BigDecimal?, description: String, categoryId: Int) {
        val bundle = AddDetailedTransactionActivity.createBundle(amount?.toString(), description, categoryId)
        val intent = Intent(this, AddDetailedTransactionActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
    }
}