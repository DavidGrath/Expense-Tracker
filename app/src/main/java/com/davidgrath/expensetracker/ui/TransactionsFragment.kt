package com.davidgrath.expensetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.PurchaseItemsAdapter
import com.davidgrath.expensetracker.databinding.FragmentTransactionsBinding
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.ui.Category
import com.davidgrath.expensetracker.entities.ui.PurchaseItem
import com.davidgrath.expensetracker.entities.ui.Transaction
import com.davidgrath.expensetracker.transactionsToTransactionItems

class TransactionsFragment: Fragment(), ExpenseTracker.TempDbListener {

    lateinit var binding: FragmentTransactionsBinding
    lateinit var viewModel: MainViewModel
    lateinit var adapter: PurchaseItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        adapter = PurchaseItemsAdapter(emptyList())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerviewTransactions.adapter = adapter
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())
//        viewModel.listLiveData.observe(viewLifecycleOwner) { list ->
//            adapter.setItems(list)
//        }
        val app = requireContext().applicationContext as ExpenseTracker
        app.tempListeners += this
    }

    override fun onDbChanged(tempDb: ExpenseTracker.TempDb) {
        val transformedItems = tempDb.transactions.map<TransactionDb, Transaction> { transaction ->
            val t = Transaction(transaction.id, transaction.amount, transaction.currencyCode, transaction.isCashless, transaction.timestamp, transaction.datedTimestamp, emptyList())
            val items = tempDb.purchaseItems
                .filter { it.transactionId == transaction.id }
                .map { pi -> PurchaseItem(t, pi.amount, pi.description, Category.TEMP_DEFAULT_CATEGORIES.find { it.id == pi.categoryId }!!, pi.brand) }
            t.copy(items = items)
        }
        adapter.setItems(transactionsToTransactionItems(transformedItems))
    }

    companion object {
        fun newInstance(): TransactionsFragment {
            val transactionsFragment = TransactionsFragment()
            return transactionsFragment
        }
    }
}