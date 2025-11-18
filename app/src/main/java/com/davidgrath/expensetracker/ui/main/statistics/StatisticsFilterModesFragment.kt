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
import com.davidgrath.expensetracker.databinding.FragmentStatisticsBinding
import com.davidgrath.expensetracker.databinding.FragmentStatisticsFilterTemplateBinding
import com.davidgrath.expensetracker.entities.TransactionMode

class StatisticsFilterModesFragment: Fragment() {

    lateinit var binding: FragmentStatisticsFilterTemplateBinding
    lateinit var viewModel: StatisticsFilterViewModel
    private var modesMap = mutableMapOf<TransactionMode, CheckBox>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsFilterTemplateBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider(requireActivity()).get(StatisticsFilterViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val modes = TransactionMode.values()
        for(mode in modes) {
            val linearLayout = binding.linearLayoutStatisticsFilterTemplate
            val checkbox = AppCompatCheckBox(requireContext())
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = mode.toString() //TODO Context and String IDs
            linearLayout.addView(checkbox)
            modesMap[mode] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleMode(mode)
            }
        }
        viewModel.statisticsFilterLiveData.observe(viewLifecycleOwner) { filter ->
            val xModes = filter.modes
            for((mode, cb) in modesMap) {
                cb.isChecked = mode in xModes
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): StatisticsFilterModesFragment {
            val statisticsFilterModesFragment = StatisticsFilterModesFragment()
            return statisticsFilterModesFragment
        }
    }
}