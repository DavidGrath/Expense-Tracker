package com.davidgrath.expensetracker.ui.main.categories

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentCategoriesBinding
import com.davidgrath.expensetracker.databinding.FragmentDocumentStatsBinding
import com.davidgrath.expensetracker.ui.main.MainActivity
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsFragment
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsViewModel
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsViewModelFactory

class CategoriesFragment: Fragment(), CategoriesRecyclerAdapter.CategoriesClickListener, OnClickListener {

    lateinit var binding: FragmentCategoriesBinding
    lateinit var viewModel: CategoriesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCategoriesBinding.inflate(layoutInflater, null, false)
        val app = requireContext().applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        viewModel = ViewModelProvider(viewModelStore, CategoriesViewModelFactory(appComponent)).get(
            CategoriesViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = CategoriesRecyclerAdapter(emptyList(), this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewCategories.adapter = adapter
        binding.recyclerviewCategories.layoutManager = layoutManager
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.setItems(categories)
        }
        binding.fabCategories.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        v?.let {  view ->
            when(view) {
                binding.fabCategories -> {
                    val intent = Intent(requireContext(), AddCategoryActivity::class.java)
                    requireActivity().startActivityForResult(intent, MainActivity.REQUEST_CODE_ADD_CATEGORY)
                }
            }
        }
    }

    override fun onEditClicked(categoryId: Long, categoryName: String, categoryIcon: String) {

    }


    companion object {
        @JvmStatic
        fun newInstance(): CategoriesFragment {
            val categoriesFragment = CategoriesFragment()
            return categoriesFragment
        }
    }
}