package com.davidgrath.expensetracker.ui.transactiondetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.databinding.FragmentTransactionDetailsItemsBinding
import com.davidgrath.expensetracker.ui.main.MainViewModel

class TransactionDetailsItemsFragment: Fragment() {

    lateinit var binding: FragmentTransactionDetailsItemsBinding
    lateinit var viewModel: TransactionDetailsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionDetailsItemsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(TransactionDetailsViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TransactionDetailsItemRecyclerAdapter(emptyList())
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewTransactionDetailsItems.adapter = adapter
        binding.recyclerviewTransactionDetailsItems.layoutManager = layoutManager
        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.changeItems(items)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): TransactionDetailsItemsFragment {
            val fragment = TransactionDetailsItemsFragment()
            return fragment
        }
    }
}