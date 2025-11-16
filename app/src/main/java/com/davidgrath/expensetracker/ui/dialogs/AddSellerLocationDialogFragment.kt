package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
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

class AddSellerLocationDialogFragment : DialogFragment() {

    interface AddSellerLocationListener {
        fun onAddSellerLocation(location: String, sellerId: Long)
    }

    private var listener: AddSellerLocationListener? = null
    lateinit var binding: DialogFragmentCreateSellerBinding
    private var location: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddSellerLocationListener) {
            this.listener = context
        } else if(parentFragment != null && parentFragment is AddSellerLocationListener) {
            listener = parentFragment as AddSellerLocationListener
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
            location = savedInstanceState.getString(ARG_LOCATION)
        }

        binding.textViewCreateSellerNameIndicator.text = (location?:"").codePointCount(0, (location?:"").length).toString() + "/" + MAX_NAME_LENGTH
        binding.editTextCreateSellerName.setText(location)
        val textWatcher = MaxCodePointWatcher(binding.editTextCreateSellerName, MAX_NAME_LENGTH, binding.textViewCreateSellerNameIndicator) { text ->
            this.location = text
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
        outState.putString(ARG_LOCATION, location)
    }

    override fun onResume() {
        super.onResume()

        val args = requireArguments()
        val sellerId = args.getLong(ARG_SELLER_ID)

        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val text = binding.editTextCreateSellerName.editableText.toString()
            if(text.isBlank()) {
                binding.editTextCreateSellerName.error = "Invalid"
            }

            if(text.isNotBlank()) {
                listener?.onAddSellerLocation(text, sellerId)
                dismiss()
            }
        }
    }

    companion object {
        private const val ARG_SELLER_ID = "sellerId"
        private const val ARG_LOCATION = "location"
        private const val MAX_NAME_LENGTH = 50
        private val LOGGER = LoggerFactory.getLogger(AddSellerLocationDialogFragment::class.java)

        @JvmStatic
        fun createDialog(sellerId: Long): AddSellerLocationDialogFragment {
            val args = bundleOf(ARG_SELLER_ID to sellerId)
            val dialog = AddSellerLocationDialogFragment()
            dialog.arguments = args
            return dialog
        }
    }
}