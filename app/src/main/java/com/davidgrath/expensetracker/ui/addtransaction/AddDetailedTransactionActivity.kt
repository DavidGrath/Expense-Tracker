package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionBinding
import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.getSha256
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID

class AddDetailedTransactionActivity : FragmentActivity(),
    AddDetailedTransactionMainFragment.AddDetailedTransactionMainListener {

    private val TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN = "addDetailedItemsMain"

    lateinit var binding: ActivityAddDetailedTransactionBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    lateinit var mainFragment: AddDetailedTransactionMainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailedTransactionBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        val repository = app.addDetailedTransactionRepository()
        viewModel = ViewModelProvider.create(
            viewModelStore,
            AddDetailedTransactionViewModelFactory(app, repository)
        ).get(AddDetailedTransactionViewModel::class.java)
        setContentView(binding.root)
        val extras = intent.extras
        if (savedInstanceState == null) {
            if (extras != null) {
                val amount = extras.getString(ARG_INITIAL_AMOUNT)
                val bd = if (amount != null) BigDecimal(amount) else null
                mainFragment = AddDetailedTransactionMainFragment.newInstance(
                    bd, extras.getString(
                        ARG_INITIAL_DESCRIPTION
                    ), extras.getInt(ARG_INITIAL_CATEGORY_ID)
                )
            } else {
                mainFragment = AddDetailedTransactionMainFragment.newInstance()
            }
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.frame_add_detailed_transaction,
                    mainFragment,
                    TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN
                )
                .show(mainFragment)
                .commit()
        } else {
            mainFragment =
                supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN) as AddDetailedTransactionMainFragment
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                when (requestCode) {
                    REQUEST_CODE_ITEM_OPEN_IMAGE -> {
                        val uri = data!!.data!!
                        viewModel.addItemFile(uri)
                    }
                }
            }
        }
    }

    override fun onFinished() {
        finish()
    }

    companion object {

        val ARG_INITIAL_AMOUNT = "initialAmount"
        val ARG_INITIAL_DESCRIPTION = "initialDescription"
        val ARG_INITIAL_CATEGORY_ID = "initialCategoryId"
        val REQUEST_CODE_ITEM_OPEN_IMAGE = 100
        val REQUEST_CODE_OPEN_DOCUMENT = 101

        fun createBundle(
            initialAmount: String?,
            initialDescription: String,
            initialCategoryId: Int
        ): Bundle {
            return bundleOf(
                ARG_INITIAL_AMOUNT to initialAmount,
                ARG_INITIAL_DESCRIPTION to initialDescription,
                ARG_INITIAL_CATEGORY_ID to initialCategoryId
            )
        }
    }
}