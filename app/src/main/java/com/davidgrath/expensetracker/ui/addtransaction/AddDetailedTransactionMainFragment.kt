package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionMainBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import java.util.Locale

class AddDetailedTransactionMainFragment: Fragment(), AddTransactionItemRecyclerAdapter.AddTransactionItemRecyclerListener, OnClickListener {


    interface AddDetailedTransactionMainListener {
        fun onFinished()
    }

    private var listener: AddDetailedTransactionMainListener? = null
    lateinit var binding: FragmentAddDetailedTransactionMainBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    lateinit var adapter: AddTransactionItemRecyclerAdapter
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
        val categories = viewModel.getCategories()
//            .observeOn(Schedulers.io())
//            .subscribeOn(AndroidSchedulers.mainThread())
            .blockingGet()
        val c = categories.map { categoryDbToCategoryUi(it) }
        adapter = AddTransactionItemRecyclerAdapter(c, this)
        binding.recyclerviewAddDetailedTransactionMain.adapter = adapter
        binding.recyclerviewAddDetailedTransactionMain.layoutManager = LinearLayoutManager(requireContext())
        viewModel.transactionItemsLiveData.observe(viewLifecycleOwner) { triple ->
            Log.d("TRIPLE", triple.toString())
            val list = triple.first.items
            val event = triple.second
            val position = triple.third
            when(event) {
                AddDetailedTransactionRepository.TransactionDetailEvent.Delete -> {
                    adapter.setItems(list)
                    adapter.notifyItemRemoved(position)
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.Insert -> {
                    adapter.setItems(list)
                    adapter.notifyItemInserted(position)
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.All -> {
                    adapter.setItems(list)
                    adapter.notifyDataSetChanged()
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.Change -> {
                    adapter.setItems(list)
                    //No change to prevent EditText focus loss
                }
                AddDetailedTransactionRepository.TransactionDetailEvent.ChangeInvalidate -> {
                    adapter.setItems(list)
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
                    if(viewModel.validateDraft()) {
                        viewModel.finishDraft()
                        listener?.onFinished()
                    } else {
                        Snackbar.make(binding.root, "Invalid input", Snackbar.LENGTH_SHORT).show()
                    }
                }
                else -> {

                }
            }
        }
    }

    override fun onItemChanged(position: Int, item: AddTransactionItem) {
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
        fun newInstance(): AddDetailedTransactionMainFragment {
            val fragment = AddDetailedTransactionMainFragment()
            return fragment
        }
    }
}