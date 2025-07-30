package com.davidgrath.expensetracker.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.entities.ui.Category
import java.math.BigDecimal

class AddTransactionDialogFragment: DialogFragment() {

    interface AddTransactionListener {
        fun onAddTransaction(amount: BigDecimal, description: String, categoryId: Int)
        fun onGoToDetails(amount: BigDecimal?, description: String, categoryId: Int)
    }
    var listener: AddTransactionListener? = null
    lateinit var binding: DialogFragmentAddTransactionBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentAddTransactionBinding.inflate(requireActivity().layoutInflater, null, false)
        val spinnerAdapter = SpinnerCategoryAdapter(binding.root.context, R.layout.spinner_item_category, Category.TEMP_DEFAULT_CATEGORIES.toTypedArray())
        binding.spinnerAddTransactionCategory.adapter = spinnerAdapter
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->
                var amount: BigDecimal? = null
                try {
                    amount = BigDecimal(binding.editTextAddTransactionAmount.editableText.toString())
                } catch (e: NumberFormatException) {
                    binding.editTextAddTransactionAmount.error = "Invalid"
                }

                val description = binding.editTextAddTransactionDescription.editableText.toString()
                if(description.isBlank()) {
                    binding.editTextAddTransactionDescription.error = "Empty"
                }
                val categoryId = binding.spinnerAddTransactionCategory.selectedItemPosition
                println("SelectedPos: $categoryId")
                if(amount != null && description.isNotBlank()) {
                    listener?.onAddTransaction(amount, description, categoryId)
                    dismiss()
                }
            })
            .setNeutralButton("Details") { dialog, which ->
                var amount: BigDecimal? = null
                try {
                    amount = BigDecimal(binding.editTextAddTransactionAmount.editableText.toString())
                } catch (e: NumberFormatException) {
                    binding.editTextAddTransactionAmount.error = "Invalid"
                }
                val description = binding.editTextAddTransactionDescription.editableText.toString()
                val categoryId = binding.spinnerAddTransactionCategory.selectedItemPosition
                listener?.onGoToDetails(amount, description, categoryId)
                dismiss()
            }
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dismiss()
            })
            .create()

    }
}