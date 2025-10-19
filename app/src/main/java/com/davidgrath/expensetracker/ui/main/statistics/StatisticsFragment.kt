package com.davidgrath.expensetracker.ui.main.statistics

import android.content.Intent
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
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.davidgrath.expensetracker.ui.main.MainViewModel
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
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
            LOGGER.debug("config: {}", stats)
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
        val barWidth = 0.44f
        val groupSpace = 0f
        val barSpace = 0.03f
        viewModel.statsTotalByCategory.observe(viewLifecycleOwner) { pair ->
            val expenseDataSet = BarDataSet(pair.first, null)
//            expenseDataSet.colors = MaterialColors.Palette.map { it.value }
            expenseDataSet.colors = listOf(MaterialColors.Red600.value)
            expenseDataSet.setDrawIcons(true)
            expenseDataSet.setDrawValues(true)

            val incomeDataSet = BarDataSet(pair.second, null)
//            incomeDataSet.colors = MaterialColors.Palette.map { it.value }
            incomeDataSet.colors = listOf(MaterialColors.Green600.value)
            incomeDataSet.setDrawIcons(true)
            incomeDataSet.setDrawValues(true)

            val data = BarData(expenseDataSet, incomeDataSet)

            data.barWidth = barWidth

            binding.barChartStatisticsCategories.isLogEnabled = true
            binding.barChartStatisticsCategories.data = data
            binding.barChartStatisticsCategories.groupBars(0f, groupSpace, barSpace)
            binding.barChartStatisticsCategories.xAxis.axisMinimum = 0f
            binding.barChartStatisticsCategories.xAxis.axisMaximum = pair.first.size.toFloat()
//            binding.barChartStatisticsCategories.xAxis.setCenterAxisLabels(true)
            binding.barChartStatisticsCategories.invalidate()
        }

        binding.lineChartStatisticsTotal.legend.isEnabled = false
        binding.lineChartStatisticsTotal.description.isEnabled = false
        binding.lineChartStatisticsTotal.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.lineChartStatisticsTotal.axisLeft.setDrawLabels(false)
        binding.lineChartStatisticsTotal.axisLeft.axisMinimum = 0f
        binding.lineChartStatisticsTotal.axisRight.axisMinimum = 0f
        viewModel.statsTotalByDay.observe(viewLifecycleOwner) { pair ->
            val (expenses, income) = pair
            val expensesEntries = expenses.mapIndexed { i, summary ->
                Entry(i.toFloat(), summary.sum.toFloat())
            }
            val expensesDataSet = LineDataSet(expensesEntries, null)
            expensesDataSet.colors = listOf(MaterialColors.Red600.value)
            expensesDataSet.setDrawCircles(false)
            expensesDataSet.setDrawValues(true)

            val incomeEntries = income.mapIndexed { i, summary ->
                Entry(i.toFloat(), summary.sum.toFloat())
            }
            val incomeDataSet = LineDataSet(incomeEntries, null)
            incomeDataSet.colors = listOf(MaterialColors.Green600.value)
            incomeDataSet.setDrawCircles(false)
            incomeDataSet.setDrawValues(true)


            val data = LineData(expensesDataSet, incomeDataSet)
            data.setValueFormatter(NoZeroValueFormatter())
            val dates = expenses.map { summary -> summary.aggregateDate }
            binding.lineChartStatisticsTotal.xAxis.setValueFormatter(DateValuesFormatter(dates))
            binding.lineChartStatisticsTotal.isLogEnabled = true
            binding.lineChartStatisticsTotal.data = data
            binding.lineChartStatisticsTotal.invalidate()
        }
        viewModel.statsTransactionAndItemCount.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTransactionCount.text = "${it.transactionCount} transactions"
            binding.textViewStatisticsItemCount.text = "${it.itemCount} items"
        }

        binding.imageViewStatisticsConfigureCurrentMode.setOnClickListener(this)
        binding.imageViewStatisticsOpenFilter.setOnClickListener(this)
        binding.imageViewStatisticsViewItems.setOnClickListener(this)
        binding.spinnerStatisticsCurrentMode.adapter = SpinnerStatisticModeAdapter(viewModel.statisticsConfig.xDays, requireContext(), dateModes)
        binding.spinnerStatisticsCurrentMode.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedMode = dateModes[position]
        LOGGER.info("onItemSelected: {}", selectedMode)
        viewModel.setDateMode(selectedMode)
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
                binding.imageViewStatisticsOpenFilter -> {
                    viewModel.saveStatisticsFilterToFile().blockingSubscribe()
                    val intent = Intent(requireContext(), StatisticsFilterActivity::class.java)
                    requireActivity().startActivityForResult(intent, MainActivity.REQUEST_CODE_OPEN_FILTER)
                }
                binding.imageViewStatisticsViewItems -> {

                }
            }
        }
    }

    override fun onNumberPicked(number: Int) {
        viewModel.setXDaysPast(number)
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

    class NoZeroValueFormatter: DefaultValueFormatter(0) {
        override fun getFormattedValue(value: Float): String {
            if(value == 0f) {
                return ""
            } else {
                return super.getFormattedValue(value)
            }
        }
    }
}