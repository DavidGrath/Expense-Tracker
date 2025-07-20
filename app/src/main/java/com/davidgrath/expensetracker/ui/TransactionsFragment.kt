package com.davidgrath.expensetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.TransactionsAdapter
import com.davidgrath.expensetracker.databinding.FragmentTransactionsBinding

class TransactionsFragment: Fragment() {

    lateinit var binding: FragmentTransactionsBinding
    lateinit var viewModel: MainViewModel
    lateinit var adapter: TransactionsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        adapter = TransactionsAdapter(emptyList())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerviewTransactions.adapter = adapter
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())
        viewModel.listLiveData.observe(viewLifecycleOwner) { list ->
            adapter.setItems(list)
        }
    }

    companion object {
        fun newInstance(): TransactionsFragment {
            val transactionsFragment = TransactionsFragment()
            return transactionsFragment
        }
    }
}