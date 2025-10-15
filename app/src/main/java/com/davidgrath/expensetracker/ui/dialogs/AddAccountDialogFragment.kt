package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.databinding.DialogFragmentCreateAccountBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.CurrencyAdapter
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject

class AddAccountDialogFragment : DialogFragment() {

    interface AddAccountListener {
        fun onAddAccount(name: String, currency: Currency)
    }

    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    var listener: AddAccountListener? = null
    lateinit var binding: DialogFragmentCreateAccountBinding
    lateinit var currencies: List<Currency>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentCreateAccountBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)
        val spinnerAdapter = CurrencyAdapter(
            binding.root.context,
            currencies.toTypedArray(),
            timeAndLocaleHandler
        )
        binding.spinnerCreateAccountCurrency.adapter = spinnerAdapter
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->

            })
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
            val text = binding.editTextCreateAccountName.editableText.toString()
            if(text.isBlank()) {
                binding.editTextCreateAccountName.error = "Invalid"
            }
            val selectedPosition = binding.spinnerCreateAccountCurrency.selectedItemPosition
            val currency = if (selectedPosition == Spinner.INVALID_POSITION) {
                null
            } else {
                currencies[selectedPosition]
            }
            if(currency != null && text.isNotBlank()) {
                listener?.onAddAccount(text, currency)
                dismiss()
            }
        }
    }
}