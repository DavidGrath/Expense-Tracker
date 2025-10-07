package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.databinding.DialogFragmentCreateAccountBinding
import com.davidgrath.expensetracker.databinding.DialogFragmentEditAccountBinding
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.CurrencyAdapter
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import java.math.BigDecimal
import java.util.Currency

class EditAccountDialogFragment : DialogFragment() {

    interface EditAccountListener {
        fun onEditAccount(accountId: Long, name: String)
    }

    var listener: EditAccountListener? = null
    lateinit var binding: DialogFragmentEditAccountBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentEditAccountBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        val args = requireArguments()
        val accountName = args.getString(ARG_ACCOUNT_NAME)
        binding.editTextEditAccountName.setText(accountName)
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

        val args = requireArguments()
        val accountId = args.getLong(ARG_ACCOUNT_ID)

        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val text = binding.editTextEditAccountName.editableText.toString()
            if(text.isBlank()) {
                binding.editTextEditAccountName.error = "Invalid"
            } else {
                listener?.onEditAccount(accountId, text)
                dismiss()
            }
        }
    }

    companion object {
        private const val ARG_ACCOUNT_ID = "accountId"
        private const val ARG_ACCOUNT_NAME = "accountName"

        fun createDialog(accountId: Long, initialName: String): EditAccountDialogFragment {
            val args = bundleOf(ARG_ACCOUNT_ID to accountId, ARG_ACCOUNT_NAME to initialName)
            val dialog = EditAccountDialogFragment()
            dialog.arguments = args
            return dialog
        }
    }
}