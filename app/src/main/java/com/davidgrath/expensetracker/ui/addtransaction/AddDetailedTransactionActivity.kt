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
import com.davidgrath.expensetracker.ui.dialogs.GenericDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.Clock
import java.math.BigDecimal
import javax.inject.Inject

class AddDetailedTransactionActivity : FragmentActivity(),
    AddDetailedTransactionMainFragment.AddDetailedTransactionMainListener, GenericDialogFragment.GenericDialogListener {

    lateinit var binding: ActivityAddDetailedTransactionBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var clock: Clock
    var noPagesDialogFragment: GenericDialogFragment? = null
    var passwordDialogFragment: GenericDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailedTransactionBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        app.appComponent.inject(this)
        val extras = intent.extras
        var amount: BigDecimal? = null
        var description: String? = null
        var categoryId: Long? = null
        var mode = "add"
        var transactionId: Long? = null
        if (extras != null) {
            mode = extras.getString(ARG_MODE)?: "add"
            if(mode == "edit") {
                transactionId = extras.getLong(ARG_EDIT_TRANSACTION_ID)
            } else if(mode == "add"){
                val amountString = extras.getString(ARG_INITIAL_AMOUNT)
                amount = if (amountString != null) BigDecimal(amountString) else null
                description = extras.getString(ARG_INITIAL_DESCRIPTION)
                categoryId = extras.getLong(ARG_INITIAL_CATEGORY_ID)
            }
        }
        viewModel = ViewModelProvider.create(
            viewModelStore,
            AddDetailedTransactionViewModelFactory(app, mode, addDetailedTransactionRepository, categoryRepository, clock, transactionId, amount, description, categoryId)
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
                        liveData.observe(this, object: Observer<AddDetailedTransactionViewModel.PdfState> {
                            override fun onChanged(value: AddDetailedTransactionViewModel.PdfState) {
                                when(value) {
                                    AddDetailedTransactionViewModel.PdfState.NOT_PDF -> {
                                        Log.i("AddDetailTransActivity", "Add evidence done")
                                    }
                                    AddDetailedTransactionViewModel.PdfState.ALL_GOOD -> {
                                        Log.i("AddDetailTransActivity", "Add evidence done")
                                    }
                                    AddDetailedTransactionViewModel.PdfState.PASSWORD_PROTECTED -> {
                                        if(passwordDialogFragment == null) {
                                            passwordDialogFragment = GenericDialogFragment.newInstance(
                                                "Password Protected",
                                                "This PDF is password protected", "Ok", null, null, null, DIALOG_TAG_PASSWORD_PROTECTED
                                                )
                                            passwordDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_PASSWORD_PROTECTED)
                                        } else {
                                            if(!passwordDialogFragment!!.dialog!!.isShowing) {
                                                passwordDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_PASSWORD_PROTECTED)
                                            }
                                        }
                                    }
                                    AddDetailedTransactionViewModel.PdfState.ZERO_PAGES -> {
                                        if(noPagesDialogFragment == null) {
                                            noPagesDialogFragment = GenericDialogFragment.newInstance(
                                                "Zero pages",
                                                "Somehow, this PDF has zero pages", ":-)", null, null, null, DIALOG_TAG_NO_PAGES
                                            )
                                            noPagesDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_NO_PAGES)
                                        } else {
                                            if(!noPagesDialogFragment!!.dialog!!.isShowing) {
                                                noPagesDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_NO_PAGES)
                                            }
                                        }
                                    }
                                }
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

    override fun onPositiveButton(disambiguationTag: String, data: String?) {
        when(disambiguationTag) {
            DIALOG_TAG_NO_PAGES -> {

            }
            DIALOG_TAG_PASSWORD_PROTECTED -> {

            }
        }
    }

    override fun onNegativeButton(disambiguationTag: String, data: String?) {

    }

    override fun onNeutralButton(disambiguationTag: String, data: String?) {

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

        const val ARG_INITIAL_AMOUNT = "initialAmount"
        const val ARG_INITIAL_DESCRIPTION = "initialDescription"
        const val ARG_INITIAL_CATEGORY_ID = "initialCategoryId"
        const val ARG_MODE = "mode"
        const val ARG_EDIT_TRANSACTION_ID = "editTransactionId"
        const val REQUEST_CODE_ITEM_OPEN_IMAGE = 100
        const val REQUEST_CODE_OPEN_DOCUMENT = 101
        const val DIALOG_TAG_NO_PAGES = "noPages"
        const val DIALOG_TAG_PASSWORD_PROTECTED = "passwordProtected"

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