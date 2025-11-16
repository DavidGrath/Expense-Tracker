package com.davidgrath.expensetracker.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.MaxCodePointWatcher
import com.davidgrath.expensetracker.databinding.DialogFragmentEditAccountBinding
import org.slf4j.LoggerFactory

class EditAccountDialogFragment : DialogFragment() {

    interface EditAccountListener {
        fun onEditAccount(accountId: Long, name: String)
    }

    private var listener: EditAccountListener? = null
    lateinit var binding: DialogFragmentEditAccountBinding
    var accountName: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is EditAccountListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is EditAccountListener) {
            listener = parentFragment as EditAccountListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentEditAccountBinding.inflate(
            requireActivity().layoutInflater,
            null,
            false
        )
        if(savedInstanceState != null) {
            accountName = savedInstanceState.getString(ARG_SAVED_ACCOUNT_NAME)
        } else {
            val args = requireArguments()
            accountName = args.getString(BUNDLE_ARG_ACCOUNT_NAME)

        }

        binding.editTextEditAccountName.setText(accountName)
        binding.textViewEditAccountNameIndicator.text = (accountName?:"").codePointCount(0, (accountName?:"").length).toString() + "/" + MAX_NAME_LENGTH
        val textWatcher = MaxCodePointWatcher(binding.editTextEditAccountName, MAX_NAME_LENGTH, binding.textViewEditAccountNameIndicator) { text ->
            this.accountName = text
        }
        binding.editTextEditAccountName.addTextChangedListener(textWatcher)
        return AlertDialog.Builder(requireContext())
            .setTitle("Edit Account Name")
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
        outState.putString(ARG_SAVED_ACCOUNT_NAME, accountName)
    }

    override fun onResume() {
        super.onResume()

        val args = requireArguments()
        val accountId = args.getLong(BUNDLE_ARG_ACCOUNT_ID)

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
        private const val BUNDLE_ARG_ACCOUNT_ID = "accountId"
        private const val BUNDLE_ARG_ACCOUNT_NAME = "accountName"
        private const val ARG_SAVED_ACCOUNT_NAME = "savedAccountName"
        private const val MAX_NAME_LENGTH = 50
        private val LOGGER = LoggerFactory.getLogger(EditAccountDialogFragment::class.java)

        fun createDialog(accountId: Long, initialName: String): EditAccountDialogFragment {
            val args = bundleOf(BUNDLE_ARG_ACCOUNT_ID to accountId, BUNDLE_ARG_ACCOUNT_NAME to initialName)
            val dialog = EditAccountDialogFragment()
            dialog.arguments = args
            return dialog
        }
    }
}