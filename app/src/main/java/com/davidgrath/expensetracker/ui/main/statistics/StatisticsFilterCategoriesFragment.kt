package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.FragmentStatisticsFilterTemplateBinding

class StatisticsFilterCategoriesFragment: Fragment() {

    lateinit var binding: FragmentStatisticsFilterTemplateBinding
    lateinit var viewModel: StatisticsFilterViewModel
    private var categoriesMap = mutableMapOf<Long, CheckBox>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsFilterTemplateBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider(requireActivity()).get(StatisticsFilterViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categories = viewModel.getCategories().blockingGet().map { categoryDbToCategoryUi(it) }
        categories.forEach { cat ->
            val linearLayout = binding.root
            val checkbox = CheckBox(requireContext())
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = cat.name
            linearLayout.addView(checkbox)
            categoriesMap[cat.id] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleCategory(cat.id)
            }
        }
        viewModel.statisticsFilterLiveData.observe(viewLifecycleOwner) { filter ->
            val categories = filter.categories
            for((catId, cb) in categoriesMap) {
                cb.isChecked = catId in categories
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): StatisticsFilterCategoriesFragment {
            val statisticsFilterCategoriesFragment = StatisticsFilterCategoriesFragment()
            return statisticsFilterCategoriesFragment
        }
    }
}