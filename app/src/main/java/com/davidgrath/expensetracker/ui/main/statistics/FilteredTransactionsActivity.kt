package com.davidgrath.expensetracker.ui.main.statistics

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityFilteredTransactionsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.ui.main.TransactionItemsAdapter
import com.davidgrath.expensetracker.ui.main.TransactionsFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import org.slf4j.LoggerFactory
import javax.inject.Inject

class FilteredTransactionsActivity: AppCompatActivity(), TransactionItemsAdapter.TransactionClickListener {

    lateinit var adapter: TransactionItemsAdapter
    lateinit var binding: ActivityFilteredTransactionsBinding
    lateinit var viewModel: FilteredTransactionsViewModel
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilteredTransactionsBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)
        viewModel = ViewModelProvider(this, FilteredTransactionsViewModelFactory(appComponent)).get(
            FilteredTransactionsViewModel::class.java)
        adapter = TransactionItemsAdapter(emptyList(), timeAndLocaleHandler, this)
        viewModel.transactionsAndItems.observe(this) { list ->
            LOGGER.info("Transactions Item Count: {}", list.size)
            adapter.setItems(list)
        }
        viewModel.statsTransactionAndItemCount.observe(this) {
            binding.textViewFilteredTransactionsTransactionCount.text = "${it.transactionCount} transactions" //TODO Pluralization
            binding.textViewFilteredTransactionsItemCount.text = "${it.itemCount} items"
        }
        binding.recyclerViewFilteredTransactions.adapter = adapter
        binding.recyclerViewFilteredTransactions.layoutManager = LinearLayoutManager(this)
        setContentView(binding.root)
    }

    override fun onTransactionClicked(transactionId: Long) {
        val intent = Intent(this, TransactionDetailsActivity::class.java)
        intent.putExtra(TransactionDetailsActivity.ARG_TRANSACTION_ID, transactionId)
        startActivity(intent)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FilteredTransactionsActivity::class.java)
    }
}