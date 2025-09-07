package com.davidgrath.expensetracker.ui.transactiondetails

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.davidgrath.expensetracker.databinding.ActivityTransactionDetailsBinding

class TransactionDetailsActivity: AppCompatActivity() {

    lateinit var binding: ActivityTransactionDetailsBinding
    var transactionId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
    }
}