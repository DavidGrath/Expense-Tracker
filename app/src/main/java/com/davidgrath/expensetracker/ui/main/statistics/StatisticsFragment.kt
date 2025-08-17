package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.util.Log
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
import com.davidgrath.expensetracker.entities.ui.TempStatisticsConfig
import com.davidgrath.expensetracker.ui.SpinnerStatisticModeAdapter
import com.davidgrath.expensetracker.ui.main.MainViewModel
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import org.threeten.bp.LocalDate

class StatisticsFragment: Fragment(), OnClickListener, OnItemSelectedListener {

    lateinit var binding: FragmentStatisticsBinding
    lateinit var viewModel: MainViewModel
    val modes = TempStatisticsConfig.Mode.values()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.statisticsConfigLiveData.observe(viewLifecycleOwner) { stats ->
            when(stats.mode) {
                TempStatisticsConfig.Mode.Daily -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = true
                }
                TempStatisticsConfig.Mode.PastXDays -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = true
                }
                TempStatisticsConfig.Mode.PastWeek -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = false
                }
                TempStatisticsConfig.Mode.Weekly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = true
                }
                TempStatisticsConfig.Mode.PastMonth -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = false
                }
                TempStatisticsConfig.Mode.Monthly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = true
                }
                TempStatisticsConfig.Mode.PastYear -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = false
                }
                TempStatisticsConfig.Mode.Yearly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = true
                }
                TempStatisticsConfig.Mode.Range -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.buttonStatisticsConfigureCurrentMode.isEnabled = true
                }
            }
        }
        viewModel.statsTotalSpent.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTotal.text = it.toString()
        }
        binding.barChartStatisticsCategories.legend.isEnabled = false
        binding.barChartStatisticsCategories.description.isEnabled = false
        binding.barChartStatisticsCategories.xAxis.setDrawLabels(false)
        binding.barChartStatisticsCategories.axisLeft.setDrawLabels(false)
        binding.barChartStatisticsCategories.axisLeft.axisMinimum = 0f
        binding.barChartStatisticsCategories.axisRight.axisMinimum = 0f
        viewModel.statsTotalByCategory.observe(viewLifecycleOwner) { list ->
            Log.i("StatisticsFragment", "Item Count: ${list.size}")
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
        viewModel.statsTotalByDay.observe(viewLifecycleOwner) { list ->
            Log.i("StatisticsFragment", "Item Count: ${list.size}")
            val entries = list.mapIndexed { i, pair ->
                Entry(i.toFloat(), pair.second.toFloat())
            }
            val dataSet = LineDataSet(entries, null)
            dataSet.colors = MaterialColors.Palette.map { it.value }
            dataSet.setDrawIcons(true)
            dataSet.setDrawValues(true)
            val data = LineData(dataSet)
            val dates = list.map { it.first }
            data.setValueFormatter(DateValuesFormatter(dates))
            binding.lineChartStatisticsTotal.isLogEnabled = true
            binding.lineChartStatisticsTotal.data = data
            binding.lineChartStatisticsTotal.invalidate()
        }

        binding.buttonStatisticsConfigureCurrentMode.setOnClickListener(this)
        binding.spinnerStatisticsCurrentMode.adapter = SpinnerStatisticModeAdapter(viewModel.statisticsConfig.xDays, requireContext(), modes)
        binding.spinnerStatisticsCurrentMode.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedMode = modes[position]
        viewModel.setConfig(viewModel.statisticsConfig.copy(mode = selectedMode))
        if(selectedMode == TempStatisticsConfig.Mode.Range) {
            //Open Dialog
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.buttonStatisticsConfigureCurrentMode -> {
                    when(viewModel.statisticsConfig.mode) {
                        TempStatisticsConfig.Mode.Daily -> {

                        }
                        TempStatisticsConfig.Mode.PastXDays -> {

                        }
                        TempStatisticsConfig.Mode.Weekly -> {

                        }
                        TempStatisticsConfig.Mode.Monthly -> {

                        }
                        TempStatisticsConfig.Mode.Yearly -> {

                        }
                        TempStatisticsConfig.Mode.Range -> {

                        }
                        TempStatisticsConfig.Mode.PastWeek,
                        TempStatisticsConfig.Mode.PastMonth,
                        TempStatisticsConfig.Mode.PastYear -> {

                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): StatisticsFragment {
            val statisticsFragment = StatisticsFragment()
            return statisticsFragment
        }
    }

    class DateValuesFormatter(private val dates: List<LocalDate>): ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            val localDate = dates[index]
            return localDate.toString()
        }
    }
}