package com.davidgrath.expensetracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.MaterialColors
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.FragmentTransactionsBinding
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.dialogs.AddTransactionDialogFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.Locale

class TransactionsFragment: Fragment(), OnClickListener, OnLongClickListener, AddTransactionDialogFragment.AddTransactionListener, TransactionItemsAdapter.TransactionClickListener {

    lateinit var binding: FragmentTransactionsBinding
    lateinit var viewModel: MainViewModel
    lateinit var adapter: TransactionItemsAdapter

    var addTransactionDialog: AddTransactionDialogFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        addTransactionDialog = childFragmentManager.findFragmentByTag(FRAGMENT_TAG_ADD_TRANSACTION) as AddTransactionDialogFragment?
        adapter = TransactionItemsAdapter(emptyList(), this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerviewTransactions.adapter = adapter
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())
        viewModel.listLiveData.observe(viewLifecycleOwner) { list ->
            LOGGER.info("Transactions Item Count: {}", list.size)
            adapter.setItems(list)
        }
        /*binding.barChartMain.legend.isEnabled = false
        binding.barChartMain.description.isEnabled = false
        binding.barChartMain.xAxis.setDrawLabels(false)
        binding.barChartMain.axisLeft.setDrawLabels(false)
        binding.barChartMain.axisLeft.axisMinimum = 0f
        binding.barChartMain.axisRight.axisMinimum = 0f
        viewModel.statsPastXByCategory.observe(viewLifecycleOwner) { list ->
            LOGGER.info("Summary Item Count: {}", list.size)
            val dataSet = BarDataSet(list, "Summary for past 7 days")
            dataSet.colors = MaterialColors.Palette.map { it.value }
            dataSet.setDrawIcons(true)
            dataSet.setDrawValues(true)
            val data = BarData(dataSet)
            binding.barChartMain.isLogEnabled = true
            binding.barChartMain.data = data
            binding.barChartMain.invalidate()
        }*/
        viewModel.statsTotalIncome.observe(viewLifecycleOwner) {
            binding.textViewTransactionsTotalIncome.text = String.format(Locale.getDefault(), "%.2f", it)
        }
        viewModel.statsTotalExpense.observe(viewLifecycleOwner) {
            binding.textViewTransactionsTotalExpense.text = String.format(Locale.getDefault(), "%.2f", it)
        }

        binding.fabTransactions.setOnClickListener(this)
        binding.fabTransactions.setOnLongClickListener(this)
    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.fabTransactions -> {
                    if(addTransactionDialog == null) {
                        val categories = viewModel.getCategories()
                            .blockingGet()
                        val accounts = viewModel.getAccounts().blockingGet()
                        addTransactionDialog = AddTransactionDialogFragment()
                        addTransactionDialog!!.categories = categories.map { categoryDbToCategoryUi(it) }
                        addTransactionDialog!!.accounts = accounts.toMutableList()
                    }
                    if(!(addTransactionDialog?.dialog?.isShowing?:false)) {
                        addTransactionDialog?.listener = this
                        addTransactionDialog?.show(childFragmentManager, FRAGMENT_TAG_ADD_TRANSACTION)
                    }
                }
                else -> {}
            }
        }
    }


    override fun onLongClick(v: View?): Boolean {
        v?.let {
            when(v) {
                binding.fabTransactions -> {
                    val intent = Intent(requireActivity(), AddDetailedTransactionActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        return true
    }

    override fun onGoToDetails(accountId: Long, amount: BigDecimal?, description: String?, categoryId: Long?) {
        val bundle = AddDetailedTransactionActivity.createBundle(accountId, amount?.toString(), description, categoryId)
        val intent = Intent(requireActivity(), AddDetailedTransactionActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    override fun onAddTransaction(accountId: Long, amount: BigDecimal, description: String, categoryId: Long) {
        viewModel.saveTransaction(accountId, amount, description, categoryId)
    }

    override fun onTransactionClicked(transactionId: Long) {
        val intent = Intent(requireActivity(), TransactionDetailsActivity::class.java)
        intent.putExtra(TransactionDetailsActivity.ARG_TRANSACTION_ID, transactionId)
        startActivity(intent)
    }

    companion object {
        fun newInstance(): TransactionsFragment {
            val transactionsFragment = TransactionsFragment()
            return transactionsFragment
        }
        private const val FRAGMENT_TAG_ADD_TRANSACTION = "addTransaction"

        private val LOGGER = LoggerFactory.getLogger(TransactionsFragment::class.java)
    }
}