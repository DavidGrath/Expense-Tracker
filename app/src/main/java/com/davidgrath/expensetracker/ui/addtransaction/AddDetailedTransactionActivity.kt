package com.davidgrath.expensetracker.ui.addtransaction

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionBinding
import java.math.BigDecimal

class AddDetailedTransactionActivity: FragmentActivity(), AddDetailedTransactionMainFragment.AddDetailedTransactionMainListener {

    val TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN = "addDetailedItemsMain"
    lateinit var binding: ActivityAddDetailedTransactionBinding
    lateinit var mainFragment: AddDetailedTransactionMainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailedTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val extras = intent.extras
        if(savedInstanceState == null) {
            if(extras != null) {
                val amount = extras.getString(ARG_INITIAL_AMOUNT)
                val bd = if(amount != null) BigDecimal(amount) else null
                mainFragment = AddDetailedTransactionMainFragment.newInstance(bd, extras.getString(
                    ARG_INITIAL_DESCRIPTION
                ), extras.getInt(ARG_INITIAL_CATEGORY_ID))
            } else {
                mainFragment = AddDetailedTransactionMainFragment.newInstance()
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_add_detailed_transaction, mainFragment, TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN)
                .show(mainFragment)
                .commit()
        } else {
            mainFragment = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_ADD_DETAILED_ITEMS_MAIN) as AddDetailedTransactionMainFragment
        }
    }

    override fun tempOnFinished() {
        finish()
    }

    companion object {

        val ARG_INITIAL_AMOUNT = "initialAmount"
        val ARG_INITIAL_DESCRIPTION = "initialDescription"
        val ARG_INITIAL_CATEGORY_ID = "initialCategoryId"

        fun createBundle(initialAmount: String?, initialDescription: String, initialCategoryId: Int): Bundle {
            return bundleOf(
                ARG_INITIAL_AMOUNT to initialAmount,
                ARG_INITIAL_DESCRIPTION to initialDescription,
                ARG_INITIAL_CATEGORY_ID to initialCategoryId
            )
        }
    }
}