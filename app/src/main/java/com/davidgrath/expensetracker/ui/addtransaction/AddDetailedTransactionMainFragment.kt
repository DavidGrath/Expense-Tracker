package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionMainBinding
import com.davidgrath.expensetracker.entities.db.PurchaseItemDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal
import java.util.Locale

class AddDetailedTransactionMainFragment: Fragment(), AddTransactionPurchaseItemRecyclerAdapter.AddTransactionPurchaseItemRecyclerListener, OnClickListener {


    interface AddDetailedTransactionMainListener {
        fun tempOnFinished()
    }

    private var listener: AddDetailedTransactionMainListener? = null
    lateinit var binding: FragmentAddDetailedTransactionMainBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    lateinit var adapter: AddTransactionPurchaseItemRecyclerAdapter
    var currencyCode = "USD"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddDetailedTransactionMainListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAddDetailedTransactionMainBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider.create(viewModelStore, ViewModelProvider.NewInstanceFactory()).get(AddDetailedTransactionViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AddTransactionPurchaseItemRecyclerAdapter(this)
        binding.recyclerviewAddDetailedTransactionMain.adapter = adapter
        binding.recyclerviewAddDetailedTransactionMain.layoutManager = LinearLayoutManager(requireContext())
        viewModel.purchaseItemsLiveData.observe(viewLifecycleOwner) { listPair ->
            val list = listPair.first
            val position = listPair.second and AddDetailedTransactionViewModel.POSITION_MASK
            val event = listPair.second and AddDetailedTransactionViewModel.EVENT_MASK
            when(event) {
                AddDetailedTransactionViewModel.EVENT_ALL -> {
                    adapter.submitList(list)
                    adapter.notifyDataSetChanged()
                }
                AddDetailedTransactionViewModel.EVENT_INSERT -> {
                    adapter.submitList(list)
                    adapter.notifyItemInserted(position)
                }
                AddDetailedTransactionViewModel.EVENT_DELETE -> {
                    adapter.submitList(list)
                    adapter.notifyItemRemoved(position)
                }
                AddDetailedTransactionViewModel.EVENT_CHANGE -> {
                    adapter.submitList(list)
                    //No change to prevent EditText focus loss
                }
            }
        }
        binding.linearLayoutAddDetailedTransactionMainAddItem.setOnClickListener(this)
        viewModel.transactionTotalLiveData.observe(viewLifecycleOwner) { total ->
            binding.textViewAddDetailedTransactionMainTotal.text = currencyCode + " " + String.format(Locale.getDefault(), "%.2f", total)
        }
        binding.imageButtonAddDetailedTransactionDone.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        v?.let {
            when(v) {
                binding.linearLayoutAddDetailedTransactionMainAddItem -> {
                    viewModel.addItem()
                }
                binding.imageButtonAddDetailedTransactionDone -> {
                    //TODO Validate items
                    val app = requireContext().applicationContext as ExpenseTracker
                    val total = viewModel.purchaseItems.first.map { it.amount?: BigDecimal.ZERO }.reduceOrNull { acc, bigDecimal -> acc.plus(bigDecimal) }?: BigDecimal.ZERO
                    val transaction = TransactionDb(0, total, currencyCode, true, ZonedDateTime.now(), ZonedDateTime.now())
                    app.addTransaction(transaction)
                    for(item in viewModel.purchaseItems.first) {
                        val purchaseItem = PurchaseItemDb(0, transaction.id, item.amount!!, item.description!!, item.category.id, item.brand)
                        app.addPurchaseItem(purchaseItem)
                    }
                    listener?.tempOnFinished()
                }
                else -> {

                }
            }
        }
    }

    override fun onItemChanged(position: Int, item: AddTransactionPurchaseItem) {
        viewModel.onItemChanged(position, item)
    }

    override fun onItemDeleted(position: Int) {
        viewModel.onItemDeleted(position)
    }

    companion object {
        @JvmStatic
        fun newInstance(initialAmount: BigDecimal? = null, initialDescription: String? = null, initialCategoryId: Int? = null): AddDetailedTransactionMainFragment {
            val fragment = AddDetailedTransactionMainFragment()
            //TODO Bundle
            return fragment
        }
    }
}