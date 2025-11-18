package com.davidgrath.expensetracker.ui.dialogs

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.MaxCodePointWatcher
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.databinding.DialogFragmentCreateAccountBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.CurrencyAdapter
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import com.davidgrath.expensetracker.ui.main.accounts.AccountsFragment
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject

class AddAccountDialogFragment : DialogFragment() {

    interface AddAccountListener {
        fun onAddAccount(name: String, currency: Currency)
    }

    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    private var listener: AddAccountListener? = null
    lateinit var binding: DialogFragmentCreateAccountBinding
    private lateinit var currencies: List<Currency>
    var accountName: String? = null
    var currencyPosition = Spinner.INVALID_POSITION

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddAccountListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is AddAccountListener) {
            listener = parentFragment as AddAccountListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentCreateAccountBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)
        currencies = Currency.getAvailableCurrencies().sortedBy { it.currencyCode }
        LOGGER.info("Loaded {} currencies", currencies.size)
        if(savedInstanceState != null) {
            accountName = savedInstanceState.getString(ARG_NAME)
            currencyPosition = savedInstanceState.getInt(ARG_CURRENCY_POSITION, Spinner.INVALID_POSITION)
        }

        if(currencyPosition != Spinner.INVALID_POSITION) {
            binding.spinnerCreateAccountCurrency.setSelection(currencyPosition)
        }
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currencyPosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        val spinnerAdapter = CurrencyAdapter(
            binding.root.context,
            currencies.toTypedArray(),
            timeAndLocaleHandler
        )
        binding.spinnerCreateAccountCurrency.onItemSelectedListener = spinnerListener
        binding.spinnerCreateAccountCurrency.adapter = spinnerAdapter

        binding.textViewCreateAccountNameIndicator.text = (accountName?:"").codePointCount(0, (accountName?:"").length).toString() + "/" +  MAX_NAME_LENGTH
        binding.editTextCreateAccountName.setText(accountName)
        val textWatcher = MaxCodePointWatcher(binding.editTextCreateAccountName, MAX_NAME_LENGTH, binding.textViewCreateAccountNameIndicator) { text ->
            this.accountName = text
        }
        binding.editTextCreateAccountName.addTextChangedListener(textWatcher)
        return AlertDialog.Builder(requireContext())
            .setTitle("Add new account")
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
        outState.putString(ARG_NAME, accountName)
        outState.putInt(ARG_CURRENCY_POSITION, currencyPosition)
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

    companion object {
        private const val MAX_NAME_LENGTH = 50
        private const val ARG_NAME = "name"
        private const val ARG_CURRENCY_POSITION = "currencyPosition"
        private val LOGGER = LoggerFactory.getLogger(AddAccountDialogFragment::class.java)
    }
}