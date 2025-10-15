package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.MaterialColors
import com.davidgrath.expensetracker.databinding.FragmentStatisticsBinding
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.ui.SpinnerStatisticModeAdapter
import com.davidgrath.expensetracker.ui.dialogs.NumberDialogFragment
import com.davidgrath.expensetracker.ui.main.MainViewModel
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate

class StatisticsFragment: Fragment(), OnClickListener, OnItemSelectedListener, NumberDialogFragment.NumberDialogListener {

    lateinit var binding: FragmentStatisticsBinding
    lateinit var viewModel: MainViewModel
    val dateModes = StatisticsConfig.DateMode.values()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.statisticsConfigLiveData.observe(viewLifecycleOwner) { stats ->
            when(stats.dateMode) {
                StatisticsConfig.DateMode.Daily -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                }
                StatisticsConfig.DateMode.PastXDays -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                }
                StatisticsConfig.DateMode.PastWeek -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                }
                StatisticsConfig.DateMode.Weekly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                }
                StatisticsConfig.DateMode.PastMonth -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                }
                StatisticsConfig.DateMode.Monthly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                }
                StatisticsConfig.DateMode.PastYear -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                }
                StatisticsConfig.DateMode.Yearly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                }
                StatisticsConfig.DateMode.Range -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                }
                StatisticsConfig.DateMode.All -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                }
            }
        }
        viewModel.statsTotalIncome.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTotalIncome.text = it.toString()
        }
        viewModel.statsTotalExpense.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTotalExpenses.text = it.toString()
        }
        binding.barChartStatisticsCategories.legend.isEnabled = false
        binding.barChartStatisticsCategories.description.isEnabled = false
        binding.barChartStatisticsCategories.xAxis.setDrawLabels(false)
        binding.barChartStatisticsCategories.axisLeft.setDrawLabels(false)
        binding.barChartStatisticsCategories.axisLeft.axisMinimum = 0f
        binding.barChartStatisticsCategories.axisRight.axisMinimum = 0f
//        viewModel.statsTotalByCategory.observe(viewLifecycleOwner) { list ->
        viewModel.statsPastXByCategory.observe(viewLifecycleOwner) { list ->
            LOGGER.info("statsPastXByCategory: Item Count: ${list.size}")
            val dataSet = BarDataSet(list, null)
            dataSet.colors = MaterialColors.Palette.map { it.value }
            dataSet.setDrawIcons(true)
            dataSet.setDrawValues(true)
            val data = BarData(dataSet)
            binding.barChartStatisticsCategories.isLogEnabled = true
            binding.barChartStatisticsCategories.data = data
            binding.barChartStatisticsCategories.invalidate()
        }

        binding.lineChartStatisticsTotal.legend.isEnabled = false
        binding.lineChartStatisticsTotal.description.isEnabled = false
        binding.lineChartStatisticsTotal.xAxis.setDrawLabels(false)
        binding.lineChartStatisticsTotal.axisLeft.setDrawLabels(false)
        binding.lineChartStatisticsTotal.axisLeft.axisMinimum = 0f
        binding.lineChartStatisticsTotal.axisRight.axisMinimum = 0f
        viewModel.statsTotalExpensesByDay.observe(viewLifecycleOwner) { list -> //TODO Combine with income
            LOGGER.info("Item Count: ${list.size}")
            val entries = list.mapIndexed { i, summary ->
                Entry(i.toFloat(), summary.sum.toFloat())
            }
            val dataSet = LineDataSet(entries, null)
            dataSet.colors = MaterialColors.Palette.map { it.value }
            dataSet.setDrawIcons(true)
            dataSet.setDrawValues(true)
            val data = LineData(dataSet)
            val dates = list.map { summary -> summary.aggregateDate }
            data.setValueFormatter(DateValuesFormatter(dates))
            binding.lineChartStatisticsTotal.isLogEnabled = true
            binding.lineChartStatisticsTotal.data = data
            binding.lineChartStatisticsTotal.invalidate()
        }
        viewModel.statsTransactionAndItemCount.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTransactionCount.text = "${it.transactionCount} transactions"
            binding.textViewStatisticsItemCount.text = "${it.itemCount} items"
        }

        binding.imageViewStatisticsConfigureCurrentMode.setOnClickListener(this)
        binding.imageViewStatisticsFilter.setOnClickListener(this)
        binding.imageViewStatisticsViewItems.setOnClickListener(this)
        binding.spinnerStatisticsCurrentMode.adapter = SpinnerStatisticModeAdapter(viewModel.statisticsConfig.xDays, requireContext(), dateModes)
        binding.spinnerStatisticsCurrentMode.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedMode = dateModes[position]
        LOGGER.info("onItemSelected: {}", selectedMode)
        viewModel.setConfig(viewModel.statisticsConfig.copy(dateMode = selectedMode))
        if(selectedMode == StatisticsConfig.DateMode.Range) {
            //Open Dialog
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.imageViewStatisticsConfigureCurrentMode -> {
                    when(viewModel.statisticsConfig.dateMode) {
                        StatisticsConfig.DateMode.Daily -> {

                        }
                        StatisticsConfig.DateMode.PastXDays -> {
                            val x = viewModel.statisticsConfig.xDays
                            val numberDialogFragment = NumberDialogFragment.newInstance(x)
                            numberDialogFragment.listener = this
                            numberDialogFragment.show(childFragmentManager, DIALOG_TAG_NUMBER)
                        }
                        StatisticsConfig.DateMode.Weekly -> {

                        }
                        StatisticsConfig.DateMode.Monthly -> {

                        }
                        StatisticsConfig.DateMode.Yearly -> {

                        }
                        StatisticsConfig.DateMode.Range -> {

                        }
                        StatisticsConfig.DateMode.PastWeek,
                        StatisticsConfig.DateMode.PastMonth,
                        StatisticsConfig.DateMode.PastYear,
                        StatisticsConfig.DateMode.All, -> {

                        }
                    }
                }
                binding.imageViewStatisticsFilter -> {

                }
                binding.imageViewStatisticsViewItems -> {

                }
            }
        }
    }

    override fun onNumberPicked(number: Int) {
        viewModel.setConfig(viewModel.statisticsConfig.copy(xDays = number))
    }

    companion object {
        fun newInstance(): StatisticsFragment {
            val statisticsFragment = StatisticsFragment()
            return statisticsFragment
        }
        private const val DIALOG_TAG_NUMBER = "number"
        private val LOGGER = LoggerFactory.getLogger(StatisticsFragment::class.java)
    }

    class DateValuesFormatter(private val dates: List<LocalDate>): ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            val localDate = dates[index]
            return localDate.toString()
        }
    }
}