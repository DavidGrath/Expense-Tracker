package com.davidgrath.expensetracker.ui.dialogs

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.Constants.Companion.MAX_CODEPOINT_LENGTH_MEDIUM
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.DialogFragmentAddTransactionBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.formatDecimal
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.ui.AccountAdapter
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import com.davidgrath.expensetracker.utils.MaxCodePointWatcher
import com.davidgrath.expensetracker.utils.NumberFormatTextWatcher
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import javax.inject.Inject

class AddTransactionDialogFragment : DialogFragment() {

    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var accountRepository: AccountRepository

    interface AddTransactionListener {
        fun onAddTransaction(accountId: Long, debitOrCredit: Boolean, amount: BigDecimal, description: String, categoryId: Long)
        fun onGoToDetails(accountId: Long, debitOrCredit: Boolean, amount: BigDecimal?, description: String?, categoryId: Long?)
    }

    private var listener: AddTransactionListener? = null
    private lateinit var categories: List<CategoryUi>
    private lateinit var accounts: MutableList<AccountUi>
    private lateinit var binding: DialogFragmentAddTransactionBinding

    var amount: BigDecimal? = null
    var description: String? = null
    var debitOrCredit: Boolean = true
    var accountPosition = -1
    var categoryPosition = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddTransactionListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is AddTransactionListener) {
            listener = parentFragment as AddTransactionListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)
        val args = requireArguments()
        val profileId = args.getLong(BUNDLE_ARG_PROFILE_ID)
        categories = categoryRepository.getCategoriesSingle(profileId).blockingGet().map { categoryDbToCategoryUi(it) }
        accounts = accountRepository.getAccountsForProfileSingle(profileId).map { accounts ->
            accounts.map { accountDbToAccountUi(it, timeAndLocaleHandler.getLocale()) }
        }.blockingGet().toMutableList()
        binding = DialogFragmentAddTransactionBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        if(savedInstanceState != null) {
            amount = if(savedInstanceState.getString(ARG_AMOUNT) == null) {
                null
            } else {
                BigDecimal(savedInstanceState.getString(ARG_AMOUNT))
            }
            description = savedInstanceState.getString(ARG_DESCRIPTION)
            debitOrCredit = savedInstanceState.getBoolean(ARG_DEBIT_OR_CREDIT)
            categoryPosition = savedInstanceState.getInt(ARG_CATEGORY, -1)
            accountPosition = savedInstanceState.getInt(ARG_ACCOUNT, -1)
        }

        if(categoryPosition != Spinner.INVALID_POSITION) {
            binding.spinnerAddTransactionCategory.setSelection(categoryPosition)
        }
        val spinnerAdapter = SpinnerCategoryAdapter(
            binding.root.context,
            R.layout.spinner_item_category,
            categories.toTypedArray()
        )
        val categoryListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                categoryPosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerAddTransactionCategory.adapter = spinnerAdapter
        binding.spinnerAddTransactionCategory.onItemSelectedListener = categoryListener

        if(accountPosition != Spinner.INVALID_POSITION) {
            binding.spinnerAddTransactionAccount.setSelection(accountPosition)
        }
        val accountListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                accountPosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        val accountsAdapter = AccountAdapter(binding.root.context, accounts)
        binding.spinnerAddTransactionAccount.adapter = accountsAdapter
        binding.spinnerAddTransactionAccount.onItemSelectedListener = accountListener

        binding.editTextAddTransactionAmount.setText(formatDecimal(amount?: BigDecimal.ZERO, timeAndLocaleHandler.getLocale()))
        val amountWatcher = NumberFormatTextWatcher(binding.editTextAddTransactionAmount, BigDecimal(1_000_000), timeAndLocaleHandler.getLocale()) { amount ->
            this.amount = amount
        }
        binding.editTextAddTransactionAmount.addTextChangedListener(amountWatcher)

        binding.textViewAddTransactionDescriptionIndicator.text = (description?:"").codePointCount(0, (description?:"").length).toString() + "/" + MAX_CODEPOINT_LENGTH_MEDIUM
        binding.editTextAddTransactionDescription.setText(description)
        val descriptionWatcher = MaxCodePointWatcher(binding.editTextAddTransactionDescription, MAX_CODEPOINT_LENGTH_MEDIUM, binding.textViewAddTransactionDescriptionIndicator) { text ->
            this.description = text
        }
        binding.editTextAddTransactionDescription.addTextChangedListener(descriptionWatcher)

        if(this.debitOrCredit) {
            binding.imageViewAddTransactionDebitOrCredit.setImageResource(R.drawable.baseline_remove_24)
        } else {
            binding.imageViewAddTransactionDebitOrCredit.setImageResource(R.drawable.baseline_add_24)
        }
        binding.imageViewAddTransactionDebitOrCredit.setOnClickListener {
            this.debitOrCredit = !this.debitOrCredit
            if(this.debitOrCredit) {
                binding.imageViewAddTransactionDebitOrCredit.setImageResource(R.drawable.baseline_remove_24)
            } else {
                binding.imageViewAddTransactionDebitOrCredit.setImageResource(R.drawable.baseline_add_24)
            }
        }

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
                listener?.onGoToDetails(accountId, debitOrCredit, amount, description, categoryId)
                dismiss()
            }
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dismiss()
            })
            .create()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_AMOUNT, amount?.toPlainString())
        outState.putString(ARG_DESCRIPTION, description)
        outState.putBoolean(ARG_DEBIT_OR_CREDIT, debitOrCredit)
        outState.putInt(ARG_CATEGORY, categoryPosition)
        outState.putInt(ARG_ACCOUNT, accountPosition)
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {

            val description = binding.editTextAddTransactionDescription.editableText.toString()
            if (description.isBlank()) {
                binding.editTextAddTransactionDescription.error = "Empty"
            }
            val selectedPosition = binding.spinnerAddTransactionCategory.selectedItemPosition
            val selectedAccountPosition = binding.spinnerAddTransactionAccount.selectedItemPosition
            if(amount == null) {
                binding.editTextAddTransactionAmount.error = "Invalid"
            } else {
                if (amount!!.compareTo(BigDecimal.ZERO) == 0) {
                    LOGGER.info("Zero amount")
                    binding.editTextAddTransactionAmount.error = "Invalid"
                } else {
                    if(description.isNotBlank() && selectedPosition != Spinner.INVALID_POSITION && selectedAccountPosition != Spinner.INVALID_POSITION) {
                        val categoryId = categories[selectedPosition].id
                        val accountId = accounts[selectedAccountPosition].id
                        listener?.onAddTransaction(accountId, debitOrCredit, amount!!, description, categoryId)
                        dismiss()
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddTransactionDialogFragment::class.java)
        private const val ARG_AMOUNT = "amount"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_DEBIT_OR_CREDIT = "debitOrCredit"
        private const val ARG_CATEGORY = "category"
        private const val ARG_ACCOUNT = "account"
        private const val BUNDLE_ARG_PROFILE_ID = "profileId"

        @JvmStatic
        fun createDialog(profileId: Long): AddTransactionDialogFragment {
            val args = bundleOf(BUNDLE_ARG_PROFILE_ID to profileId)
            val dialog = AddTransactionDialogFragment()
            dialog.arguments = args
            return dialog
        }
    }
}