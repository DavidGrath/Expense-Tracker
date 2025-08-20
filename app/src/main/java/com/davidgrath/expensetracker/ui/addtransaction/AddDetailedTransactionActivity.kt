package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionBinding
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import javax.inject.Inject

class AddDetailedTransactionActivity : FragmentActivity(),
    AddDetailedTransactionMainFragment.AddDetailedTransactionMainListener {

    private val TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN = "addDetailedItemsMain"

    lateinit var binding: ActivityAddDetailedTransactionBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    lateinit var mainFragment: AddDetailedTransactionMainFragment
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository

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
            AddDetailedTransactionViewModelFactory(app, addDetailedTransactionRepository, categoryRepository, amount, description, categoryId)
        ).get(AddDetailedTransactionViewModel::class.java)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            mainFragment = AddDetailedTransactionMainFragment.newInstance()
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
                        val liveData = viewModel.addItemFile(uri)
                        liveData.observe(this, object: Observer<Unit> {
                            override fun onChanged(value: Unit) {
                                Log.i("AddDetailTransActivity", "File add done")
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