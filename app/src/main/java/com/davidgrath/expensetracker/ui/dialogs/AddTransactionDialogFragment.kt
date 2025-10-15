package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.AccountAdapter
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import java.math.BigDecimal

class AddTransactionDialogFragment : DialogFragment() {

    interface AddTransactionListener {
        fun onAddTransaction(accountId: Long, amount: BigDecimal, description: String, categoryId: Long) //TODO debitOrCredit
        fun onGoToDetails(accountId: Long, amount: BigDecimal?, description: String?, categoryId: Long?)
    }

    var listener: AddTransactionListener? = null
    lateinit var binding: DialogFragmentAddTransactionBinding
    lateinit var categories: List<CategoryUi>
    lateinit var accounts: MutableList<AccountUi>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentAddTransactionBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        val spinnerAdapter = SpinnerCategoryAdapter(
            binding.root.context,
            R.layout.spinner_item_category,
            categories.toTypedArray()
        )
        binding.spinnerAddTransactionCategory.adapter = spinnerAdapter
        val accountsAdapter = AccountAdapter(binding.root.context, accounts)
        binding.spinnerAddTransactionAccount.adapter = accountsAdapter
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->

            })
            .setNeutralButton("Details") { dialog, which ->
                var amount: BigDecimal? = null
                try {
                    amount =
                        BigDecimal(binding.editTextAddTransactionAmount.editableText.toString())
                } catch (e: NumberFormatException) {
                    binding.editTextAddTransactionAmount.error = "Invalid"
                }
                val description = binding.editTextAddTransactionDescription.editableText.toString()
                val selectedCategoryPosition = binding.spinnerAddTransactionCategory.selectedItemPosition
                val categoryId = if (selectedCategoryPosition == Spinner.INVALID_POSITION) {
                    null
                } else {
                    categories[selectedCategoryPosition].id
                }
                val selectedAccountPosition = binding.spinnerAddTransactionAccount.selectedItemPosition
                val accountId = if(selectedAccountPosition == Spinner.INVALID_POSITION) {
                    accounts.first().id
                } else {
                    accounts[selectedAccountPosition].id
                }
                listener?.onGoToDetails(accountId, amount, description, categoryId)
                dismiss()
            }
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dismiss()
            })
            .create()

    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            var amount: BigDecimal? = null
            try {
                amount = BigDecimal(binding.editTextAddTransactionAmount.editableText.toString())
            } catch (e: NumberFormatException) {
                binding.editTextAddTransactionAmount.error = "Invalid"
            }

            val description = binding.editTextAddTransactionDescription.editableText.toString()
            if (description.isBlank()) {
                binding.editTextAddTransactionDescription.error = "Empty"
            }
            val selectedPosition = binding.spinnerAddTransactionCategory.selectedItemPosition
            val selectedAccountPosition = binding.spinnerAddTransactionAccount.selectedItemPosition
            if (amount != null) {
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    binding.editTextAddTransactionAmount.error = "Invalid"
                } else {
                    if(description.isNotBlank() && selectedPosition != Spinner.INVALID_POSITION && selectedAccountPosition != Spinner.INVALID_POSITION) {
                        val categoryId = categories[selectedPosition].id
                        val accountId = accounts[selectedAccountPosition].id
                        listener?.onAddTransaction(accountId, amount, description, categoryId)
                        dismiss()
                    }
                }
            }
        }
    }
}