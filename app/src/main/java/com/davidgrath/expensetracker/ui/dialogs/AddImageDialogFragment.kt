package com.davidgrath.expensetracker.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.DialogFragmentAddImageBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.formatDecimal
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import javax.inject.Inject

class AddImageDialogFragment: DialogFragment() {

    interface AddImageDialogListener {
        fun onAddConfirm(sourceHash: String, mimeType: String, itemId: Int?, reduceSize: Boolean, removeGpsData: Boolean)
        fun onDismiss()
    }

    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    private lateinit var binding: DialogFragmentAddImageBinding
    private var listener: AddImageDialogListener? = null
    private var reduceSize = false
    private var removeLocation = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddImageDialogListener) {
            listener = context
        } else if(parentFragment != null && parentFragment is AddImageDialogListener) {
            listener = parentFragment as AddImageDialogListener
        } else {
            LOGGER.warn("No listener attached")
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        
        binding = DialogFragmentAddImageBinding.inflate(requireActivity().layoutInflater, null, false)
        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)
        val args = requireArguments()
        val uri = Uri.parse(args.getString(ARG_IMAGE_URI))
        val drawable = ContextCompat.getDrawable(requireContext(), com.davidgrath.expensetracker.materialdrawables.R.drawable.material_symbols_progress_activity_48px)!!
        drawable.setTint(Color.BLACK)
        Glide.with(requireContext())
            .load(uri)
            .placeholder(drawable)
            .override(Target.SIZE_ORIGINAL)
            .into(binding.photoViewAddImage)

        val imageTooLarge = args.getBoolean(ARG_IMAGE_TOO_LARGE)
        if(imageTooLarge) {
            binding.linearLayoutAddImageReductionSection.visibility = View.VISIBLE
            val originalSize = args.getLong(ARG_IMAGE_FILE_SIZE)
            val reducedSize = args.getLong(ARG_IMAGE_REDUCED_FILE_SIZE)

            val compressionRatio = 100 * (1 - reducedSize.toDouble() / originalSize)
            val originalWidth = args.getInt(ARG_IMAGE_SIZE_WIDTH)
            val originalHeight = args.getInt(ARG_IMAGE_SIZE_HEIGHT)

            val reducedWidth = args.getInt(ARG_IMAGE_REDUCED_SIZE_WIDTH)
            val reducedHeight = args.getInt(ARG_IMAGE_REDUCED_SIZE_HEIGHT)

            val stringBuilder = StringBuilder()
            stringBuilder.apply {
                append("Reduce image size")

                appendLine()
                append("(")
                append(originalWidth).append("x").append(originalHeight)
                append(" -> ")
                append(reducedWidth).append("x").append(reducedHeight)
                append(")")
                appendLine()

                append(originalSize.formatBytes(timeAndLocaleHandler.getLocale()))
                append(" -> ")
                append(reducedSize.formatBytes(timeAndLocaleHandler.getLocale()))
                append(" ")
                append("(")
                append(formatDecimal(BigDecimal(compressionRatio), timeAndLocaleHandler.getLocale())).append("%")
                append(")")
            }
            binding.checkBoxAddImageReduceSize.text = stringBuilder.toString()
            val listener = object: OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                    reduceSize = isChecked
                }
            }
            if(savedInstanceState != null) {
                reduceSize = savedInstanceState.getBoolean(BUNDLE_ARG_REDUCE_SIZE)
                binding.checkBoxAddImageReduceSize.setOnCheckedChangeListener(null)
                binding.checkBoxAddImageReduceSize.isChecked = reduceSize
            }
            binding.checkBoxAddImageReduceSize.setOnCheckedChangeListener(listener)
        } else {
            binding.linearLayoutAddImageReductionSection.visibility = View.GONE
        }

        val imageHasGpsData = args.getBoolean(ARG_IMAGE_HAS_GPS_DATA)
        if(imageHasGpsData) {
            binding.linearLayoutAddImageLocationSection.visibility = View.VISIBLE
            val longitude = args.getDouble(ARG_IMAGE_LONGITUDE)
            val latitude = args.getDouble(ARG_IMAGE_LATITUDE)
            val stringBuilder = StringBuilder()
            stringBuilder.apply {
                append("Remove location").append(" ")
                append("(")
                append(BigDecimal(longitude).toPlainString()) // I'm not sure if gps coords are meant to be formatted with locale settings according to standards
                append(", ")
                append(BigDecimal(latitude).toPlainString())
                append(")")
            }
            binding.checkBoxAddImageRemoveLocation.text = stringBuilder.toString()
            val listener = object: OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                    removeLocation = isChecked
                }
            }
            if(savedInstanceState != null) {
                removeLocation = savedInstanceState.getBoolean(BUNDLE_ARG_REMOVE_LOCATION)
                binding.checkBoxAddImageRemoveLocation.setOnCheckedChangeListener(null)
                binding.checkBoxAddImageRemoveLocation.isChecked = removeLocation
            }
            binding.checkBoxAddImageRemoveLocation.setOnCheckedChangeListener(listener)
        } else {
            binding.linearLayoutAddImageLocationSection.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("Add image")
            .setPositiveButton("Ok", { dialog,which -> })
            .setNegativeButton("Cancel", { dialog,which -> })
            .create()
        dialog!!.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return dialog
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_ARG_REDUCE_SIZE, reduceSize)
        outState.putBoolean(BUNDLE_ARG_REMOVE_LOCATION, removeLocation)
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog
        val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val args = requireArguments()
            val sourceHash = args.getString(ARG_SOURCE_HASH)!!
            val mimeType = args.getString(ARG_MIME_TYPE)!!
            var itemId: Int? = args.getInt(ARG_ITEM_ID, -1)
            if(itemId == -1) {
                itemId = null
            }
            listener?.onAddConfirm(sourceHash, mimeType, itemId, reduceSize, removeLocation)
            dismiss()
        }
    }
    /*override fun onStart() {
        super.onStart()
        if(dialog != null) {
            dialog!!.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }*/

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        LOGGER.info("onDismiss")
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AddImageDialogFragment::class.java)

        private const val ARG_IMAGE_URI = "imageUri"
        private const val ARG_ITEM_ID = "itemid"
        private const val ARG_SOURCE_HASH = "sourceHash"
        private const val ARG_MIME_TYPE = "mimeType"
        private const val ARG_IMAGE_TOO_LARGE = "imageTooLarge"
        private const val ARG_IMAGE_FILE_SIZE = "imageFileSize"
        private const val ARG_IMAGE_SIZE_WIDTH = "imageSizeWidth"
        private const val ARG_IMAGE_SIZE_HEIGHT = "imageSizeHeight"
        private const val ARG_IMAGE_REDUCED_FILE_SIZE = "imageReducedFileSize"
        private const val ARG_IMAGE_REDUCED_SIZE_WIDTH = "imageReducedSizeWidth"
        private const val ARG_IMAGE_REDUCED_SIZE_HEIGHT = "imageReducedSizeHeight"
        private const val ARG_IMAGE_HAS_GPS_DATA = "imageHasGpsData"
        private const val ARG_IMAGE_LONGITUDE = "imageLongitude"
        private const val ARG_IMAGE_LATITUDE = "imageLatitude"

        private const val BUNDLE_ARG_REMOVE_LOCATION = "removeLocation"
        private const val BUNDLE_ARG_REDUCE_SIZE = "reduceSize"

        @JvmStatic
        fun newInstance(imageUri: String, itemId: Int?, sourceHash: String, mimeType: String, imageTooLarge: Boolean, imageFileSize: Long?, imageSizeWidth: Int?, imageSizeHeight: Int?, imageReducedFileSize: Long?, imageReducedSizeWidth: Int?, imageReducedSizeHeight: Int?, imageHasGpsData: Boolean, imageLongitude: Double?, imageLatitude: Double?): AddImageDialogFragment {
            val bundle = bundleOf(
                ARG_IMAGE_URI to imageUri,
                ARG_ITEM_ID to itemId,
                ARG_SOURCE_HASH to sourceHash,
                ARG_MIME_TYPE to mimeType,
                ARG_IMAGE_TOO_LARGE to imageTooLarge,
                ARG_IMAGE_FILE_SIZE to imageFileSize,
                ARG_IMAGE_SIZE_WIDTH to imageSizeWidth,
                ARG_IMAGE_SIZE_HEIGHT to imageSizeHeight,
                ARG_IMAGE_REDUCED_FILE_SIZE to imageReducedFileSize,
                ARG_IMAGE_REDUCED_SIZE_WIDTH to imageReducedSizeWidth,
                ARG_IMAGE_REDUCED_SIZE_HEIGHT to imageReducedSizeHeight,
                ARG_IMAGE_HAS_GPS_DATA to imageHasGpsData,
                ARG_IMAGE_LONGITUDE to imageLongitude,
                ARG_IMAGE_LATITUDE to imageLatitude,
            )

            val fragment = AddImageDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}