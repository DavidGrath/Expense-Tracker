package com.davidgrath.expensetracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment

class GenericDialogFragment: DialogFragment() {

    interface GenericDialogListener {
        fun onPositiveButton(disambiguationTag: String, data: String?)
        fun onNegativeButton(disambiguationTag: String, data: String?)
        fun onNeutralButton(disambiguationTag: String, data: String?)
    }

    var listener: GenericDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val args = requireArguments()
        val disambiguationTag = args.getString(ARG_DISAMBIGUATION_TAG)?:""
        val data = args.getString(ARG_DATA)?:""
        args.getString(ARG_TITLE)?.let {
            builder.setTitle(it)
        }
        args.getString(ARG_MESSAGE)?.let {
            builder.setMessage(it)
        }
        args.getString(ARG_POSITIVE_BUTTON)?.let {
            builder.setPositiveButton(it) { dialog, which ->
                listener?.onPositiveButton(disambiguationTag, data)
            }
        }
        args.getString(ARG_NEGATIVE_BUTTON)?.let {
            builder.setNegativeButton(it) { dialog, which ->
                listener?.onNegativeButton(disambiguationTag, data)
            }
        }
        args.getString(ARG_NEUTRAL_BUTTON)?.let {
            builder.setNeutralButton(it) { dialog, which ->
                listener?.onNeutralButton(disambiguationTag, data)
            }
        }
        return builder.create()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE_BUTTON = "positive"
        private const val ARG_NEGATIVE_BUTTON = "negative"
        private const val ARG_NEUTRAL_BUTTON = "neutral"
        private const val ARG_DATA = "data"
        private const val ARG_DISAMBIGUATION_TAG = "disambiguationTag"

        /**
         * @param disambiguationTag For when multiple instances are attached with their listeners to
         * the same Fragment host
         */
        fun newInstance(title: String?, message: String?, positiveButton: String, negativeButton: String?, neutralButton: String?, data: String? = null, disambiguationTag: String = ""): GenericDialogFragment {
            val bundle = bundleOf(
                ARG_TITLE to title, ARG_MESSAGE to message, ARG_POSITIVE_BUTTON to positiveButton,
                ARG_NEGATIVE_BUTTON to negativeButton, ARG_NEUTRAL_BUTTON to neutralButton, ARG_DATA to data,
                ARG_DISAMBIGUATION_TAG to disambiguationTag
                )
            val fragment = GenericDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}