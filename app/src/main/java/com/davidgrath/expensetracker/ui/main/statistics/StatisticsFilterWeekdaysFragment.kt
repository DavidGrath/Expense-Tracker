package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentStatisticsBinding
import com.davidgrath.expensetracker.databinding.FragmentStatisticsFilterTemplateBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.WeekFields
import javax.inject.Inject

class StatisticsFilterWeekdaysFragment: Fragment() {

    lateinit var binding: FragmentStatisticsFilterTemplateBinding
    lateinit var viewModel: StatisticsFilterViewModel
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    private var weekDaysMap = mutableMapOf<DayOfWeek, CheckBox>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsFilterTemplateBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider(requireActivity()).get(StatisticsFilterViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireContext().applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)
        val firstDay = WeekFields.of(timeAndLocaleHandler.getLocale()).firstDayOfWeek
        for(weekdayInt in 0L..6L) {
            val weekDay = firstDay.plus(weekdayInt)
            val linearLayout = binding.linearLayoutStatisticsFilterTemplate
            val checkbox = AppCompatCheckBox(requireContext())
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = weekDay.toString() //TODO Context and String IDs
            linearLayout.addView(checkbox)
            weekDaysMap[weekDay] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleWeekDay(weekDay)
            }
        }
        viewModel.statisticsFilterLiveData.observe(viewLifecycleOwner) { filter ->
            val weekDays = filter.weekdays
            for((weekDay, cb) in weekDaysMap) {
                cb.isChecked = weekDay in weekDays
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): StatisticsFilterWeekdaysFragment {
            val statisticsFilterWeekdaysFragment = StatisticsFilterWeekdaysFragment()
            return statisticsFilterWeekdaysFragment
        }
    }
}