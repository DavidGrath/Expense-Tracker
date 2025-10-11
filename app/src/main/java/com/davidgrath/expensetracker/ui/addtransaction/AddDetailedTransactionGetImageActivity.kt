package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionGetImageBinding
import org.slf4j.LoggerFactory

class AddDetailedTransactionGetImageActivity: ComponentActivity(), AddDetailedTransactionGetImageRecyclerAdapter.OnImageClickListener {

    private lateinit var binding: ActivityAddDetailedTransactionGetImageBinding
    private lateinit var viewModel: AddDetailedTransactionGetImageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailedTransactionGetImageBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        viewModel = ViewModelProvider(this ,AddDetailedTransactionGetImageViewModelFactory(app.appComponent)).get(AddDetailedTransactionGetImageViewModel::class.java)
        val adapter = AddDetailedTransactionGetImageRecyclerAdapter(emptyList(), this)
        val layoutManager = GridLayoutManager(this, 3)
        binding.recyclerViewAddDetailedTransactionGetImage.adapter = adapter
        binding.recyclerViewAddDetailedTransactionGetImage.layoutManager = layoutManager
        viewModel.getImages().observe(this) {
            adapter.setItems(it)
        }
        setContentView(binding.root)
    }

    override fun onImageClicked(uri: Uri) {
        LOGGER.info("Image selected. Finishing")
        setResult(RESULT_OK, Intent().setData(uri))
        finish()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionGetImageActivity::class.java)
    }
}