package com.davidgrath.expensetracker.ui.dialogs

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.davidgrath.expensetracker.databinding.DialogFragmentAddExternalMediaBinding
import org.slf4j.LoggerFactory

class AddExternalMediaDialogFragment: DialogFragment() {

    interface ExternalMediaListener {
        fun onSelectionMade(selection: Selection, itemOrEvidence: Boolean, itemId: Int?)
    }

    private var selection: Selection? = null
    private lateinit var binding: DialogFragmentAddExternalMediaBinding
    private val background = ColorDrawable(Color.RED)
    private var listener: ExternalMediaListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is ExternalMediaListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is ExternalMediaListener) {
            listener = parentFragment as ExternalMediaListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selection = try {
            Selection.valueOf(savedInstanceState?.getString(BUNDLE_ARG_SELECTION)?:"")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFragmentAddExternalMediaBinding.inflate(requireActivity().layoutInflater, null, false)
        binding.imageViewAddExternalMediaCamera.setOnClickListener {
            val oldSelection = selection
            selection = Selection.Camera
            when(oldSelection) {
                Selection.Camera,null -> {

                }
                Selection.LocalImage -> {
                    binding.imageViewAddExternalMediaLocalImage.background = null
                }
                Selection.DevicePicker -> {
                    binding.imageViewAddExternalMediaDeviceFile.background = null
                }
            }
            binding.imageViewAddExternalMediaCamera.background = background

        }
        binding.imageViewAddExternalMediaDeviceFile.setOnClickListener {
            val oldSelection = selection
            selection = Selection.DevicePicker
            when(oldSelection) {
                Selection.DevicePicker,null -> {

                }
                Selection.LocalImage -> {
                    binding.imageViewAddExternalMediaLocalImage.background = null
                }
                Selection.Camera -> {
                    binding.imageViewAddExternalMediaCamera.background = null
                }
            }
            binding.imageViewAddExternalMediaDeviceFile.background = background

        }
        val args = requireArguments()
        val itemOrEvidence = args.getBoolean(ARG_ITEM_OR_EVIDENCE)
        val atLeastOneImageExists = args.getBoolean(ARG_ITEM_AT_LEAST_ONE_IMAGE_EXISTS)
        if(itemOrEvidence && atLeastOneImageExists) {
            binding.imageViewAddExternalMediaLocalImage.visibility = View.VISIBLE
            binding.imageViewAddExternalMediaLocalImage.setOnClickListener {
                val oldSelection = selection
                selection = Selection.LocalImage
                when(oldSelection) {
                    Selection.LocalImage,null -> {

                    }
                    Selection.Camera -> {
                        binding.imageViewAddExternalMediaCamera.background = null
                    }
                    Selection.DevicePicker -> {
                        binding.imageViewAddExternalMediaDeviceFile.background = null
                    }
                }
                binding.imageViewAddExternalMediaLocalImage.background = background

            }
        } else {
            binding.imageViewAddExternalMediaLocalImage.setOnClickListener(null)
            binding.imageViewAddExternalMediaLocalImage.visibility = View.GONE
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("Choose a media source")
            .setPositiveButton("Ok", { dialog,which -> })
            .setNegativeButton("Cancel", { dialog,which -> })
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_ARG_SELECTION, selection?.name)
    }
    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            if(selection == null) {
                return@setOnClickListener
            }
            val args = requireArguments()
            var transactionItemId: Int? = args.getInt(ARG_TRANSACTION_ITEM_ID, -1)
            if(transactionItemId == -1) {
                transactionItemId = null
            }
            val itemOrEvidence = args.getBoolean(ARG_ITEM_OR_EVIDENCE)
            listener?.onSelectionMade(selection!!, itemOrEvidence, transactionItemId)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        LOGGER.info("onDismiss")
    }

    enum class Selection {
        Camera,
        LocalImage,
        DevicePicker
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AddExternalMediaDialogFragment::class.java)
        private const val ARG_ITEM_OR_EVIDENCE = "itemOrEvidence"
        private const val ARG_TRANSACTION_ITEM_ID = "transactionItemId"
        private const val ARG_ITEM_AT_LEAST_ONE_IMAGE_EXISTS = "itemAtLeastOneImageExists"
        private const val BUNDLE_ARG_SELECTION = "selection"

        fun newInstance(itemOrEvidence: Boolean, transactionItemId: Int? = null, itemAtLeastOneImageExists: Boolean = false, ): AddExternalMediaDialogFragment {
            val bundle = bundleOf(
                ARG_ITEM_OR_EVIDENCE to itemOrEvidence,
                ARG_TRANSACTION_ITEM_ID to transactionItemId,
                ARG_ITEM_AT_LEAST_ONE_IMAGE_EXISTS to itemAtLeastOneImageExists
            )
            val fragment = AddExternalMediaDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}