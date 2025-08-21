package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionBinding
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.Clock
import java.math.BigDecimal
import javax.inject.Inject

class AddDetailedTransactionActivity : FragmentActivity(),
    AddDetailedTransactionMainFragment.AddDetailedTransactionMainListener {

    lateinit var binding: ActivityAddDetailedTransactionBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailedTransactionBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        app.appComponent.inject(this)
        val extras = intent.extras
        var amount: BigDecimal? = null
        var description: String? = null
        var categoryId: Long? = null
        if (extras != null) {
            val amountString = extras.getString(ARG_INITIAL_AMOUNT)
            amount = if (amountString != null) BigDecimal(amountString) else null
            description = extras.getString(ARG_INITIAL_DESCRIPTION)
            categoryId = extras.getLong(ARG_INITIAL_CATEGORY_ID)
        }
        viewModel = ViewModelProvider.create(
            viewModelStore,
            AddDetailedTransactionViewModelFactory(app, addDetailedTransactionRepository, categoryRepository, clock, amount, description, categoryId)
        ).get(AddDetailedTransactionViewModel::class.java)
        setContentView(binding.root)

        binding.viewPagerAddDetailedTransaction.adapter = AddDetailedTransactionFragmentStateAdapter(this)
        TabLayoutMediator(binding.tabLayoutAddDetailedTransaction, binding.viewPagerAddDetailedTransaction) { tab, position ->
            if(position == 0) {
                tab.text = "Items"
            } else {
                tab.text = "Other Details"
            }
        }.attach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                when (requestCode) {
                    REQUEST_CODE_ITEM_OPEN_IMAGE -> {
                        val uri = data!!.data!!
                        val liveData = viewModel.addItemFile(uri)
                        liveData.observe(this, object: Observer<Unit> {
                            override fun onChanged(value: Unit) {
                                Log.i("AddDetailTransActivity", "File add done")
                                liveData.removeObserver(this)
                            }
                        })
                    }
                    REQUEST_CODE_OPEN_DOCUMENT -> {
                        val uri = data!!.data!!
                        val liveData = viewModel.addEvidence(uri)
                        liveData.observe(this, object: Observer<Unit> {
                            override fun onChanged(value: Unit) {
                                Log.i("AddDetailTransActivity", "Add evidence done")
                                liveData.removeObserver(this)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onFinished() {
        finish()
    }

    class AddDetailedTransactionFragmentStateAdapter(addDetailedTransactionActivity: AddDetailedTransactionActivity): FragmentStateAdapter(addDetailedTransactionActivity) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            if(position == 0) {
                return AddDetailedTransactionMainFragment.newInstance()
            } else {
                return AddDetailedTransactionOtherDetailsFragment.newInstance()
            }
        }
    }

    companion object {

        val ARG_INITIAL_AMOUNT = "initialAmount"
        val ARG_INITIAL_DESCRIPTION = "initialDescription"
        val ARG_INITIAL_CATEGORY_ID = "initialCategoryId"
        val REQUEST_CODE_ITEM_OPEN_IMAGE = 100
        val REQUEST_CODE_OPEN_DOCUMENT = 101

        fun createBundle(
            initialAmount: String?,
            initialDescription: String?,
            initialCategoryId: Long?
        ): Bundle {
            return bundleOf(
                ARG_INITIAL_AMOUNT to initialAmount,
                ARG_INITIAL_DESCRIPTION to initialDescription,
                ARG_INITIAL_CATEGORY_ID to initialCategoryId
            )
        }
    }
}