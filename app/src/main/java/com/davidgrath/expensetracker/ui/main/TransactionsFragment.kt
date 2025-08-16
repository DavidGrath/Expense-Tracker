package com.davidgrath.expensetracker.ui.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.MaterialColors
import com.davidgrath.expensetracker.databinding.FragmentTransactionsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF

class TransactionsFragment: Fragment() {

    lateinit var binding: FragmentTransactionsBinding
    lateinit var viewModel: MainViewModel
    lateinit var adapter: TransactionItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        adapter = TransactionItemsAdapter(emptyList())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerviewTransactions.adapter = adapter
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())
        viewModel.listLiveData.observe(viewLifecycleOwner) { list ->
            Log.i("Transactions", "Item Count: ${list.size}")
            adapter.setItems(list)
        }
//        binding.pieChartMain.legend.isEnabled = false
//        binding.pieChartMain.description.isEnabled = false
        binding.barChartMain.legend.isEnabled = false
        binding.barChartMain.description.isEnabled = false
        binding.barChartMain.xAxis.setDrawLabels(false)
        binding.barChartMain.axisLeft.setDrawLabels(false)
        binding.barChartMain.axisLeft.axisMinimum = 0f
        binding.barChartMain.axisRight.axisMinimum = 0f
        viewModel.past7ByCategory.observe(viewLifecycleOwner) { list ->
            Log.i("Summary", "Item Count: ${list.size}")
            val dataSet = BarDataSet(list, "Summary for past 7 days")
            dataSet.colors = MaterialColors.Palette.map { it.value }
//            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.setDrawIcons(true)
            dataSet.setDrawValues(true)
            val data = BarData(dataSet)
//            binding.pieChartMain.isLogEnabled = true
//            binding.pieChartMain.data = data
//            binding.pieChartMain.invalidate()
            binding.barChartMain.isLogEnabled = true
            binding.barChartMain.data = data
            binding.barChartMain.invalidate()
        }
    }

    companion object {
        fun newInstance(): TransactionsFragment {
            val transactionsFragment = TransactionsFragment()
            return transactionsFragment
        }
    }
}