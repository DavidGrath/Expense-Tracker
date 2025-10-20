package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.ui.WeekDayAdapter
import org.slf4j.LoggerFactory
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

class WeekDayDialogFragment: DialogFragment() {


    interface WeekdayDialogListener {
        fun onWeekdayPicked(dayOfWeek: DayOfWeek)
    }

    private lateinit var spinner: Spinner
    private var weekDayArray = arrayOf<DayOfWeek>()
    private var weekDay: DayOfWeek? = null
    var listener: WeekdayDialogListener? = null
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_ARG_WEEKDAY, weekDay?.value!!)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)

        spinner = Spinner(requireContext())
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        spinner.layoutParams = layoutParams
        val args = requireArguments()
        val firstDay = args.getInt(ARG_LOCALE_FIRST_WEEKDAY, -1)
        val day = args.getInt(ARG_INITIAL_WEEKDAY, -1)
        val firstWeekDay = DayOfWeek.of(firstDay)
        val initialWeekDay = DayOfWeek.of(day)
        var pos = -1
        val array = Array(7) {
            val wd = firstWeekDay.plus(it.toLong())
            if(wd == initialWeekDay) {
                pos = it
            }
            wd
        }
        this.weekDayArray = array
        val adapter = WeekDayAdapter(requireContext(), array, timeAndLocaleHandler)
        spinner.adapter = adapter

        if(savedInstanceState != null) {
            val weekday = DayOfWeek.of(savedInstanceState.getInt(BUNDLE_ARG_WEEKDAY))
            val savedWeekDayPos = array.indexOf(weekday)
            spinner.setSelection(savedWeekDayPos)
            this.weekDay = array[savedWeekDayPos]
            LOGGER.info("Restored weekday value from savedInstanceState")
        } else {
            spinner.setSelection(pos)
            this.weekDay = array[pos]
        }


        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position >= 0) {
                    this@WeekDayDialogFragment.weekDay = array[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        return AlertDialog.Builder(requireContext())
            .setTitle("Choose First Day of Week")
            .setView(spinner)
            .setPositiveButton("Ok") { dialog, which -> }
            .setNegativeButton("Cancel") { dialog, which -> }
            .create()
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val selectedPosition = spinner.selectedItemPosition
            val weekDay = if (selectedPosition == Spinner.INVALID_POSITION) {
                null
            } else {
                weekDayArray[selectedPosition]
            }
            if(weekDay != null) {
                listener?.onWeekdayPicked(weekDay)
            }
            dismiss()
        }

    }

    companion object {
        private const val BUNDLE_ARG_WEEKDAY = "weekday"
        private const val ARG_LOCALE_FIRST_WEEKDAY = "localeFirstWeekday"
        private const val ARG_INITIAL_WEEKDAY = "initialWeekday"

        fun newInstance(localeFirstWeekday: Int, initialWeekday: Int): WeekDayDialogFragment {
            val bundle = bundleOf(
                ARG_LOCALE_FIRST_WEEKDAY to localeFirstWeekday,
                ARG_INITIAL_WEEKDAY to initialWeekday
            )
            val fragment = WeekDayDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
        private val LOGGER = LoggerFactory.getLogger(WeekDayDialogFragment::class.java)
    }
}