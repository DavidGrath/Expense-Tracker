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
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import java.math.BigDecimal

class AddTransactionDialogFragment: DialogFragment() {

    interface AddTransactionListener {
        fun onAddTransaction(amount: BigDecimal, description: String, category: String)
    }
    var listener: AddTransactionListener? = null
    lateinit var binding: DialogFragmentAddTransactionBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentAddTransactionBinding.inflate(requireActivity().layoutInflater, null, false)
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->
                val amount = BigDecimal(binding.editTextAddTransactionAmount.editableText.toString())
                val description = binding.editTextAddTransactionDescription.editableText.toString()
                val category = binding.editTextAddTransactionCategory.editableText.toString()
                listener?.onAddTransaction(amount, description, category)
                dismiss()
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dismiss()
            })
            .create()

    }
}