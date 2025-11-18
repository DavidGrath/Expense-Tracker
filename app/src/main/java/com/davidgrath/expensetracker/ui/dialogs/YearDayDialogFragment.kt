package com.davidgrath.expensetracker.ui.dialogs

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.DialogFragmentDayOfYearBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.ui.MonthAdapter
import com.davidgrath.expensetracker.ui.MonthDayAdapter
import org.slf4j.LoggerFactory
import org.threeten.bp.Month
import org.threeten.bp.MonthDay
import javax.inject.Inject

class YearDayDialogFragment: DialogFragment() {


    interface YearDayDialogListener {
        fun onYearDayPicked(monthDay: MonthDay)
    }

    private lateinit var binding: DialogFragmentDayOfYearBinding
    private val monthArray = Month.values()
    private var daysArray = arrayOf<Int>()
    private var month: Month? = null
    private var day: Int? = null
    var listener: YearDayDialogListener? = null
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is YearDayDialogListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is YearDayDialogListener) {
            listener = parentFragment as YearDayDialogListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_ARG_MONTH, month!!.value!!)
        outState.putInt(BUNDLE_ARG_DAY, day!!)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentDayOfYearBinding.inflate(requireActivity().layoutInflater, null, false)
        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)

        val args = requireArguments()
        val m = args.getInt(ARG_INITIAL_MONTH, -1)
        val d = args.getInt(ARG_INITIAL_DAY, -1)
        val month = Month.of(m)
        val day = d
        var monthPos = monthArray.indexOf(month)
        this.daysArray = Array(month.maxLength()) {
            it+1
        }
        val adapter = MonthAdapter(requireContext(), monthArray, timeAndLocaleHandler)
        binding.spinnerDayOfYearMonth.adapter = adapter
        val monthDayAdapter = MonthDayAdapter(requireContext(), daysArray.toMutableList())
        binding.spinnerDayOfYearMonthDay.adapter = monthDayAdapter
        if(savedInstanceState != null) {
            val savedMonth = Month.of(savedInstanceState.getInt(BUNDLE_ARG_MONTH))
            val savedDay = savedInstanceState.getInt(BUNDLE_ARG_DAY)
            val savedMonthPos = monthArray.indexOf(savedMonth)
            val savedDayPos = daysArray.indexOf(savedDay)
            binding.spinnerDayOfYearMonth.setSelection(savedMonthPos)
            binding.spinnerDayOfYearMonthDay.setSelection(savedDayPos)
            this.month = month
            this.day = day
            LOGGER.info("Restored month day value from savedInstanceState")
        } else {
            this.month = month
            this.day = day
        }


        binding.spinnerDayOfYearMonth.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position >= 0) {
                    val xMonth = monthArray[position]
                    this@YearDayDialogFragment.month = xMonth
                    this@YearDayDialogFragment.daysArray = Array(xMonth.maxLength()) {
                        it+1
                    }
                    monthDayAdapter.setItems(daysArray.toMutableList())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerDayOfYearMonthDay.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position >= 0) {
                    this@YearDayDialogFragment.day = daysArray[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        return AlertDialog.Builder(requireContext())
            .setTitle("Choose Day of Year")
            .setView(binding.root)
            .setPositiveButton("Ok") { dialog, which -> }
            .setNegativeButton("Cancel") { dialog, which -> }
            .create()
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val selectedMonthPosition = binding.spinnerDayOfYearMonth.selectedItemPosition
            val month = if (selectedMonthPosition == Spinner.INVALID_POSITION) {
                null
            } else {
                monthArray[selectedMonthPosition]
            }
            val selectedDayPosition = binding.spinnerDayOfYearMonthDay.selectedItemPosition
            val day = if (selectedDayPosition == Spinner.INVALID_POSITION) {
                null
            } else {
                daysArray[selectedDayPosition]
            }
            if(month != null && day != null) {
                val monthDay = MonthDay.of(month, day)
                listener?.onYearDayPicked(monthDay)
            }
            dismiss()
        }

    }

    companion object {
        private const val BUNDLE_ARG_MONTH = "month"
        private const val BUNDLE_ARG_DAY = "day"
        private const val ARG_INITIAL_MONTH = "initialMonth"
        private const val ARG_INITIAL_DAY = "initialDay"

        fun newInstance(initialMonth: Int, initialDay: Int): YearDayDialogFragment {
            val bundle = bundleOf(
                ARG_INITIAL_MONTH to initialMonth,
                ARG_INITIAL_DAY to initialDay
            )
            val fragment = YearDayDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
        private val LOGGER = LoggerFactory.getLogger(YearDayDialogFragment::class.java)
    }
}