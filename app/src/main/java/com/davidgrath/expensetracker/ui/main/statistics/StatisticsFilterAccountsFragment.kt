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

class StatisticsFilterAccountsFragment: Fragment() {

    lateinit var binding: FragmentStatisticsFilterTemplateBinding
    lateinit var viewModel: StatisticsFilterViewModel
    private var accountsMap = mutableMapOf<Long, CheckBox>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsFilterTemplateBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider(requireActivity()).get(StatisticsFilterViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val accounts = viewModel.accounts
        accounts.forEach { account ->
            val linearLayout = binding.root
            val checkbox = CheckBox(requireContext())
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            checkbox.layoutParams = layoutParams
            checkbox.text = "${account.name} (${account.currencyCode})"
            linearLayout.addView(checkbox)
            accountsMap[account.id] = checkbox
            checkbox.setOnClickListener {
                viewModel.toggleAccountChecked(account.id)
            }
        }
        viewModel.statisticsFilterLiveData.observe(viewLifecycleOwner) { filter ->
            val accountIds = filter.accountIds
            for((id, cb) in accountsMap) {
                cb.isChecked = id in accountIds
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): StatisticsFilterAccountsFragment {
            val statisticsFilterAccountsFragment = StatisticsFilterAccountsFragment()
            return statisticsFilterAccountsFragment
        }
    }
}