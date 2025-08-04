package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionMainBinding
import com.davidgrath.expensetracker.entities.db.PurchaseItemDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal
import java.util.Locale

class AddDetailedTransactionMainFragment: Fragment(), AddTransactionPurchaseItemRecyclerAdapter.AddTransactionPurchaseItemRecyclerListener, OnClickListener {


    interface AddDetailedTransactionMainListener {
        fun onFinished()
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
        viewModel = ViewModelProvider.create(requireActivity()).get(AddDetailedTransactionViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AddTransactionPurchaseItemRecyclerAdapter(this)
        binding.recyclerviewAddDetailedTransactionMain.adapter = adapter
        binding.recyclerviewAddDetailedTransactionMain.layoutManager = LinearLayoutManager(requireContext())
        viewModel.purchaseItemsLiveData.observe(viewLifecycleOwner) { triple ->
            val list = triple.first.items
            val event = triple.second
            val position = triple.third
            when(event) {
                AddDetailedTransactionRepository.TransactionDetailEvent.Delete -> {
                    adapter.submitList(list)
                    adapter.notifyItemRemoved(position)
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.Insert -> {
                    adapter.submitList(list)
                    adapter.notifyItemInserted(position)
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.All -> {
                    adapter.submitList(list)
                    adapter.notifyDataSetChanged()
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.Change -> {
                    adapter.submitList(list)
                    //No change to prevent EditText focus loss
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.ChangeInvalidate -> {
                    adapter.submitList(list)
                    adapter.notifyItemChanged(position)
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.None -> {

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
                    viewModel.finishDraft()
                    listener?.onFinished()
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

    override fun onRequestAddImage(position: Int, itemId: Int) {
        viewModel.getImageItemId = itemId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_ITEM_OPEN_IMAGE)
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