package com.davidgrath.expensetracker.ui.main.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.databinding.FragmentStatisticsBinding
import com.davidgrath.expensetracker.databinding.FragmentStatisticsFilterTemplateBinding
import com.davidgrath.expensetracker.entities.TransactionMode

class StatisticsFilterSellersFragment: Fragment() {

    lateinit var binding: FragmentStatisticsFilterTemplateBinding
    lateinit var viewModel: StatisticsFilterViewModel
    private var sellersMap = mutableMapOf<Long, CheckBox>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsFilterTemplateBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider(requireActivity()).get(StatisticsFilterViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sellers = viewModel.sellers
        for(seller in sellers) {
            val linearLayout = binding.root
            val checkbox = CheckBox(requireContext())
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = seller.name
            linearLayout.addView(checkbox)
            sellersMap[seller.id!!] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleSeller(seller.id)
            }
        }
        viewModel.statisticsFilterLiveData.observe(viewLifecycleOwner) { filter ->
            val sellerIds = filter.sellerIds
            for((seller, cb) in sellersMap) {
                cb.isChecked = seller in sellerIds
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): StatisticsFilterSellersFragment {
            val statisticsFilterModesFragment = StatisticsFilterSellersFragment()
            return statisticsFilterModesFragment
        }
    }
}