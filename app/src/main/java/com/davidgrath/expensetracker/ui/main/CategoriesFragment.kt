package com.davidgrath.expensetracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.davidgrath.expensetracker.databinding.FragmentCategoriesBinding
import com.davidgrath.expensetracker.databinding.FragmentDocumentStatsBinding
import com.davidgrath.expensetracker.ui.main.documents.DocumentStatsFragment

class CategoriesFragment: Fragment() {

    lateinit var binding: FragmentCategoriesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCategoriesBinding.inflate(layoutInflater, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
    companion object {
        @JvmStatic
        fun newInstance(): CategoriesFragment {
            val categoriesFragment = CategoriesFragment()
            return categoriesFragment
        }
    }
}