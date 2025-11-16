package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.MaxCodePointWatcher
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.databinding.DialogFragmentCreateAccountBinding
import com.davidgrath.expensetracker.databinding.DialogFragmentCreateSellerBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.CurrencyAdapter
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject

class AddSellerDialogFragment : DialogFragment() {

    interface AddSellerListener {
        fun onAddSeller(name: String)
    }

    private var listener: AddSellerListener? = null
    lateinit var binding: DialogFragmentCreateSellerBinding
    private var name: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddSellerListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is AddSellerListener) {
            listener = parentFragment as AddSellerListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentCreateSellerBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        if(savedInstanceState != null) {
            name = savedInstanceState.getString(ARG_NAME)
        }

        binding.textViewCreateSellerNameIndicator.text = (name?:"").codePointCount(0, (name?:"").length).toString() + "/" + MAX_NAME_LENGTH
        binding.editTextCreateSellerName.setText(name)
        val textWatcher = MaxCodePointWatcher(binding.editTextCreateSellerName, MAX_NAME_LENGTH, binding.textViewCreateSellerNameIndicator) { text ->
            this.name = text
        }
        binding.editTextCreateSellerName.addTextChangedListener(textWatcher)
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->

            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dismiss()
            })
            .create()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_NAME, name)
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val text = binding.editTextCreateSellerName.editableText.toString()
            if(text.isBlank()) {
                binding.editTextCreateSellerName.error = "Invalid"
            }

            if(text.isNotBlank()) {
                listener?.onAddSeller(text)
                dismiss()
            }
        }
    }

    companion object {
        private const val MAX_NAME_LENGTH = 50
        private const val ARG_NAME = "name"
        private val LOGGER = LoggerFactory.getLogger(AddSellerDialogFragment::class.java)
    }
}