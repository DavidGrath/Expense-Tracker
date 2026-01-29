package com.davidgrath.expensetracker.ui.main.statistics

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.Constants.Companion.ALPHA_DISABLED
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.MaterialColors
import com.davidgrath.expensetracker.databinding.FragmentStatisticsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.formatDecimal
import com.davidgrath.expensetracker.ui.SpinnerStatisticModeAdapter
import com.davidgrath.expensetracker.ui.dialogs.NumberDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.WeekDayDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.YearDayDialogFragment
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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.MonthDay
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.temporal.WeekFields
import javax.inject.Inject

class StatisticsFragment: Fragment(), OnClickListener, OnItemSelectedListener, NumberDialogFragment.NumberDialogListener,
    MaterialPickerOnPositiveButtonClickListener<Long>, WeekDayDialogFragment.WeekdayDialogListener, YearDayDialogFragment.YearDayDialogListener {

    lateinit var binding: FragmentStatisticsBinding
    lateinit var viewModel: MainViewModel
    val dateModes = StatisticsConfig.DateMode.values()
    private var datePicker: MaterialDatePicker<Long>? = null
    private var rangeStartDate: LocalDate? = null
    private var rangeEndDate: LocalDate? = null
    private var dateRangePicker: MaterialDatePicker<Pair<Long, Long>>? = null
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        val app = (requireContext().applicationContext as ExpenseTracker)
        app.appComponent.inject(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = SpinnerStatisticModeAdapter(viewModel.statisticsConfig.xDays, requireContext(), dateModes)
        viewModel.statisticsConfigLiveData.observe(viewLifecycleOwner) { stats ->
            rangeStartDate = stats.filter.startDay
            rangeEndDate = stats.filter.endDay
            when(stats.dateMode) {
                StatisticsConfig.DateMode.Daily -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = 1f
                }
                StatisticsConfig.DateMode.PastXDays -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = 1f
                    adapter.currentXDays = stats.xDays
                    adapter.notifyDataSetChanged()
                }
                StatisticsConfig.DateMode.PastWeek -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = ALPHA_DISABLED
                }
                StatisticsConfig.DateMode.Weekly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = 1f
                }
                StatisticsConfig.DateMode.PastMonth -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = ALPHA_DISABLED
                }
                StatisticsConfig.DateMode.Monthly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = 1f
                }
                StatisticsConfig.DateMode.PastYear -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = ALPHA_DISABLED
                }
                StatisticsConfig.DateMode.Yearly -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = true
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = 1f
                }
                StatisticsConfig.DateMode.Range -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = true
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = 1f
                    if(stats.filter.startDay == null && stats.filter.endDay == null) {
                        LOGGER.info("No dates selected for DateMode Range. Opening dialog")
                        openDateRangeDialog()
                    }
                }
                StatisticsConfig.DateMode.All -> {
                    binding.imageButtonStatisticsCycleModeLeft.isEnabled = false
                    binding.imageButtonStatisticsCycleModeRight.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.isEnabled = false
                    binding.imageViewStatisticsConfigureCurrentMode.alpha = ALPHA_DISABLED
                }
            }

            if(stats.dateMode == StatisticsConfig.DateMode.Daily) {
                val startDateFormat = dateFormat.format(rangeStartDate!!)
                binding.textViewStatisticsDateInfo.text = startDateFormat
            } else if(stats.dateMode != StatisticsConfig.DateMode.All) {

                val startDateFormat = if(rangeStartDate != null) {
                    dateFormat.format(rangeStartDate)
                } else {
                    "?"
                }
                val endDateFormat = if(rangeEndDate != null) {
                    dateFormat.format(rangeEndDate)
                } else {
                    "?"
                }
                binding.textViewStatisticsDateInfo.text = "$startDateFormat - $endDateFormat"
            } else {
                binding.textViewStatisticsDateInfo.text = null
            }
        }
        viewModel.statsTotalIncome.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTotalIncome.text = formatDecimal(it, timeAndLocaleHandler.getLocale())
        }
        viewModel.statsTotalExpense.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTotalExpenses.text = formatDecimal(it, timeAndLocaleHandler.getLocale())
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
            binding.lineChartStatisticsTotal.data = data
            binding.lineChartStatisticsTotal.xAxis.axisMaximum = dates.size.toFloat()
            binding.lineChartStatisticsTotal.xAxis.setValueFormatter(DateValuesFormatter(dates))
            binding.lineChartStatisticsTotal.isLogEnabled = true
            binding.lineChartStatisticsTotal.invalidate()
        }
        viewModel.statsTransactionAndItemCount.observe(viewLifecycleOwner) {
            binding.textViewStatisticsTransactionCount.text = "${it.transactionCount} transactions"
            binding.textViewStatisticsItemCount.text = "${it.itemCount} items"
        }

        binding.imageViewStatisticsConfigureCurrentMode.setOnClickListener(this)
        binding.imageViewStatisticsOpenFilter.setOnClickListener(this)
        binding.imageViewStatisticsViewItems.setOnClickListener(this)
        binding.imageButtonStatisticsCycleModeLeft.setOnClickListener(this)
        binding.imageButtonStatisticsCycleModeRight.setOnClickListener(this)

        binding.spinnerStatisticsCurrentMode.adapter = adapter
        binding.spinnerStatisticsCurrentMode.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedMode = dateModes[position]
        LOGGER.info("onItemSelected: {}", selectedMode)
        viewModel.setDateMode(selectedMode)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.imageViewStatisticsConfigureCurrentMode -> {
                    when(viewModel.statisticsConfig.dateMode) {
                        StatisticsConfig.DateMode.Daily -> {
                            datePicker = childFragmentManager.findFragmentByTag(DIALOG_TAG_DATE) as MaterialDatePicker<Long>?
                            if(datePicker == null) {
                                LOGGER.info("datePicker is null. Creating")
                                val calendarConstraints = CalendarConstraints.Builder()
                                    .setValidator(DateValidatorPointBackward.now())
                                    .build()
                                val builder = MaterialDatePicker.Builder.datePicker()
                                    .setCalendarConstraints(calendarConstraints)
                                if(rangeStartDate != null) {
                                    val millis = rangeStartDate!!.atStartOfDay().toInstant(
                                        ZoneOffset.UTC).toEpochMilli()
                                    builder.setSelection(millis)
                                    LOGGER.info("Using existing date for datePicker")
                                } else {
                                    val millis = Instant.now(timeAndLocaleHandler.getClock()).toEpochMilli()
                                    builder.setSelection(millis)
                                    LOGGER.info("Using current date for datePicker")
                                }
                                datePicker = builder
                                    .build()
                                datePicker!!.addOnPositiveButtonClickListener(this)
                            }
                            if(!(datePicker?.dialog?.isShowing?: false)) {
                                datePicker!!.show(childFragmentManager,
                                    DIALOG_TAG_DATE
                                )
                                LOGGER.info("Showed datePicker")
                            }
                        }
                        StatisticsConfig.DateMode.PastXDays -> {
                            val x = viewModel.statisticsConfig.xDays
                            val numberDialogFragment = NumberDialogFragment.newInstance("Choose number of days", x, null, DISAMBIGUATION_TAG_X_DAYS)
                            numberDialogFragment.show(childFragmentManager, DIALOG_TAG_NUMBER)
                        }
                        StatisticsConfig.DateMode.Weekly -> {
                            val firstDay = WeekFields.of(timeAndLocaleHandler.getLocale()).firstDayOfWeek
                            val weekDay = viewModel.statisticsConfig.weeklyFirstDay
                            val weekDayDialogFragment = WeekDayDialogFragment.newInstance(firstDay.value, weekDay.value)
                            weekDayDialogFragment.show(childFragmentManager, DIALOG_TAG_WEEKDAY)
                        }
                        StatisticsConfig.DateMode.Monthly -> {
                            val x = viewModel.statisticsConfig.monthlyDayOfMonth
                            val numberDialogFragment = NumberDialogFragment.newInstance("Choose day of month", x, 31, DISAMBIGUATION_TAG_MONTHLY)
                            numberDialogFragment.show(childFragmentManager, DIALOG_TAG_NUMBER)
                        }
                        StatisticsConfig.DateMode.Yearly -> {
                            val monthDay = viewModel.statisticsConfig.monthDayOfYear
                            val yearDayDialogFragment = YearDayDialogFragment.newInstance(monthDay.monthValue, monthDay.dayOfMonth)
                            yearDayDialogFragment.listener = this
                            yearDayDialogFragment.show(childFragmentManager, DIALOG_TAG_YEAR_DAY)
                        }
                        StatisticsConfig.DateMode.Range -> {
                            openDateRangeDialog()
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
                    //TODO Don't bother if empty
                    viewModel.saveStatisticsFilterToFile().blockingSubscribe()
                    val intent = Intent(requireContext(), FilteredTransactionsActivity::class.java)
                    requireActivity().startActivityForResult(intent, MainActivity.REQUEST_CODE_OPEN_FILTER)
                }
                binding.imageButtonStatisticsCycleModeLeft -> {
                    viewModel.decrementXLyOffset()
                }
                binding.imageButtonStatisticsCycleModeRight -> {
                    viewModel.incrementXLyOffset()
                }
            }
        }
    }

    override fun onNumberPicked(number: Int, disambiguationTag: String) {
        when(disambiguationTag) {
            DISAMBIGUATION_TAG_X_DAYS -> {
                viewModel.setXDaysPast(number)
            }
            DISAMBIGUATION_TAG_MONTHLY -> {
                viewModel.setMonthlyDayOfMonth(number)
            }
        }
    }

    override fun onPositiveButtonClick(selection: Long?) {
        selection?.let {
            LOGGER.info("datePicker date picked")
            val instant = Instant.ofEpochMilli(it)
            val localDate = instant.atZone(timeAndLocaleHandler.getZone()).toLocalDate()
            val today = LocalDate.now(timeAndLocaleHandler.getClock())
            val dateDiff = ChronoUnit.DAYS.between(today, localDate)
            viewModel.setXLyOffset(dateDiff.toInt())
        }
    }

    override fun onWeekdayPicked(dayOfWeek: DayOfWeek) {
        LOGGER.info("onWeekDayPicked")
        viewModel.setFirstWeekDay(dayOfWeek)
    }

    override fun onYearDayPicked(monthDay: MonthDay) {
        LOGGER.info("onYearDayPicked: {}", monthDay)
        viewModel.setMonthDayOfYear(monthDay)
    }



    var dateRangePositiveListener = MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> {
        LOGGER.info("dateRangePicker date picked")
        val startInstant = Instant.ofEpochMilli(it.first)
        val startLocalDate = startInstant.atZone(timeAndLocaleHandler.getZone()).toLocalDate()

        val endInstant = Instant.ofEpochMilli(it.second)
        val endLocalDate = endInstant.atZone(timeAndLocaleHandler.getZone()).toLocalDate()
        viewModel.setDateRange(startLocalDate, endLocalDate)
    }

    fun openDateRangeDialog() {
        dateRangePicker = childFragmentManager.findFragmentByTag(DIALOG_TAG_DATE_RANGE) as MaterialDatePicker<Pair<Long, Long>>?
        if(dateRangePicker == null) {
            LOGGER.info("dateRangePicker is null. Creating")
            val calendarConstraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()
            val builder = MaterialDatePicker.Builder.dateRangePicker()
                .setCalendarConstraints(calendarConstraints)
            val startMillis: Long
            if(rangeStartDate != null) {
                startMillis = rangeStartDate!!.atStartOfDay().toInstant(
                    ZoneOffset.UTC).toEpochMilli()
                LOGGER.info("Using existing date for datePicker")
            } else {
                startMillis = Instant.now(timeAndLocaleHandler.getClock()).toEpochMilli()
                LOGGER.info("Using current date for datePicker")
            }
            val endMillis: Long
            if(rangeEndDate != null) {
                endMillis = rangeEndDate!!.atTime(LocalTime.MAX).toInstant(
                    ZoneOffset.UTC).toEpochMilli()
                LOGGER.info("Using existing date for datePicker")
            } else {
                endMillis = Instant.now(timeAndLocaleHandler.getClock()).toEpochMilli()
                LOGGER.info("Using current date for datePicker")
            }
            builder.setSelection(Pair(startMillis, endMillis))
            dateRangePicker = builder.build()
            dateRangePicker!!.addOnPositiveButtonClickListener(dateRangePositiveListener)
        }
        if(dateRangePicker?.dialog?.isShowing != true) {
            dateRangePicker!!.show(childFragmentManager,
                DIALOG_TAG_DATE_RANGE
            )
            LOGGER.info("Showed dateRangePicker")
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(): StatisticsFragment {
            val statisticsFragment = StatisticsFragment()
            return statisticsFragment
        }
        private const val DIALOG_TAG_NUMBER = "number"
        private const val DIALOG_TAG_DATE = "dateDialog"
        private const val DIALOG_TAG_DATE_RANGE = "dateRangeDialog"
        private const val DIALOG_TAG_WEEKDAY = "weekday"
        private const val DIALOG_TAG_YEAR_DAY = "yearDay"
        private const val DISAMBIGUATION_TAG_X_DAYS = "xDays"
        private const val DISAMBIGUATION_TAG_MONTHLY = "monthly"
        private val LOGGER = LoggerFactory.getLogger(StatisticsFragment::class.java)
    }

    class DateValuesFormatter(private val dates: List<LocalDate>): ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            if(index >= dates.size) { //Keeps giving IndexOutOfBoundsException for some reason. Workaround for now
                return ""
            }
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