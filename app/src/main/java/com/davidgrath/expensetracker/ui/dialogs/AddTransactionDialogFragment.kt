package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import java.math.BigDecimal

class AddTransactionDialogFragment : DialogFragment() {

    interface AddTransactionListener {
        fun onAddTransaction(amount: BigDecimal, description: String, categoryId: Long)
        fun onGoToDetails(amount: BigDecimal?, description: String?, categoryId: Long?)
    }

    var listener: AddTransactionListener? = null
    lateinit var binding: DialogFragmentAddTransactionBinding
    lateinit var categories: List<CategoryUi>

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
                val selectedPosition = binding.spinnerAddTransactionCategory.selectedItemPosition
                val categoryId = if (selectedPosition == Spinner.INVALID_POSITION) {
                    null
                } else {
                    categories[selectedPosition].id
                }
                listener?.onGoToDetails(amount, description, categoryId)
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

            if (amount != null) {
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    binding.editTextAddTransactionAmount.error = "Invalid"
                } else {
                    if(description.isNotBlank() && selectedPosition != Spinner.INVALID_POSITION) {
                        val categoryId = categories[selectedPosition].id
                        listener?.onAddTransaction(amount, description, categoryId)
                        dismiss()
                    }
                }
            }
        }
    }
}