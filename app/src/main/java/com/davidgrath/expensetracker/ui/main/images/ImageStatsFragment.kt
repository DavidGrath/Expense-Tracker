package com.davidgrath.expensetracker.ui.main.images

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentImageStatsBinding
import com.davidgrath.expensetracker.databinding.FragmentStatisticsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.ui.main.MainViewModel
import javax.inject.Inject

class ImageStatsFragment: Fragment() {

    lateinit var binding: FragmentImageStatsBinding
    lateinit var viewModel: ImageStatsViewModel
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val app = requireContext().applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)
        viewModel = ViewModelProvider(viewModelStore, ImageStatsViewModelFactory(appComponent)).get(ImageStatsViewModel::class.java)
        binding = FragmentImageStatsBinding.inflate(layoutInflater, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ImageStatsRecyclerAdapter(emptyList(), timeAndLocaleHandler)
        val layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewImages.adapter = adapter
        binding.recyclerViewImages.layoutManager = layoutManager
        viewModel.imageStats.observe(viewLifecycleOwner) {
            adapter.setItems(it)
        }
        viewModel.imageCount.observe(viewLifecycleOwner) {
            binding.textViewImageStatsOverallCount.text = "$it images" //TODO Localization, Pluralization
        }
        viewModel.totalSize.observe(viewLifecycleOwner) {
            binding.textViewImageStatsOverallSize.text = it.formatBytes(timeAndLocaleHandler.getLocale())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): ImageStatsFragment {
            val imageStatsFragment = ImageStatsFragment()
            return imageStatsFragment
        }
    }
}