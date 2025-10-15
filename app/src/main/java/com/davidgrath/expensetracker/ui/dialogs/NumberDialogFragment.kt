package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.slf4j.LoggerFactory

class NumberDialogFragment: DialogFragment() {

    interface NumberDialogListener {
        fun onNumberPicked(number: Int)
    }

    private lateinit var editText: EditText
    var listener: NumberDialogListener? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val number = try {
            Integer.valueOf(editText.text.toString())
        } catch (e: NumberFormatException) {
            LOGGER.warn("Error parsing number", e)
            null
        }
        outState.putString(BUNDLE_ARG_NUMBER_VALUE, number?.toString())
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        editText = EditText(requireContext())
        val layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        editText.layoutParams = layoutParams
        editText.inputType = InputType.TYPE_CLASS_NUMBER //Might be extended to include other number types if needed
        editText.hint = "Greater than 0"
        if(savedInstanceState != null) {
            val existingNumber = savedInstanceState.getString(BUNDLE_ARG_NUMBER_VALUE)
            LOGGER.info("Restored numeric value from savedInstanceState")
            editText.setText(existingNumber)
        } else {
            val args = requireArguments()
            val number = args.getInt(ARG_INITIAL_NUMBER_VALUE, -1)
            if(number != -1) {
                editText.setText(number.toString())
            }
        }
        return AlertDialog.Builder(requireContext())
            .setView(editText)
            .setPositiveButton("Ok") { dialog, which -> }
            .setNegativeButton("Cancel") { dialog, which -> }
            .create()
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val number = try {
                Integer.valueOf(editText.text.toString())
            } catch (e: NumberFormatException) {
                LOGGER.warn("Error parsing number", e)
                null
            }
            if(number == null) {
                editText.error = "Invalid number"
                LOGGER.info("Invalid number")
                return@setOnClickListener
            }
            if(number < 1) {
                editText.error = "Invalid number"
                LOGGER.info("Invalid number")
                return@setOnClickListener
            }
            listener?.onNumberPicked(number)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        LOGGER.info("onDismiss")
    }

    companion object {
        private const val BUNDLE_ARG_NUMBER_VALUE = "numberValue"
        private const val ARG_INITIAL_NUMBER_VALUE = "initialNumberValue"
        private val LOGGER = LoggerFactory.getLogger(NumberDialogFragment::class.java)

        fun newInstance(initialNumber: Int? = null): NumberDialogFragment {
            val bundle = bundleOf(ARG_INITIAL_NUMBER_VALUE to initialNumber)
            val fragment = NumberDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}