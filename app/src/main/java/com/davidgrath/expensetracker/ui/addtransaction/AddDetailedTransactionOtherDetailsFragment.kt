package com.davidgrath.expensetracker.ui.addtransaction

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.MaxCodePointWatcher
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionOtherDetailsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.entities.ui.SellerLocationUi
import com.davidgrath.expensetracker.entities.ui.SellerUi
import com.davidgrath.expensetracker.ui.AccountAdapter
import com.davidgrath.expensetracker.ui.SellerAdapter
import com.davidgrath.expensetracker.ui.SellerLocationAdapter
import com.davidgrath.expensetracker.ui.TransactionModeAdapter
import com.davidgrath.expensetracker.ui.dialogs.AddExternalMediaDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.AddSellerDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.AddSellerLocationDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.ibm.icu.text.BreakIterator
import org.slf4j.LoggerFactory
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.io.File
import javax.inject.Inject

class AddDetailedTransactionOtherDetailsFragment: Fragment(), OnClickListener,
    MaterialPickerOnPositiveButtonClickListener<Long>, AddTransactionEvidenceRecyclerAdapter.EvidenceClickListener, AddExternalMediaDialogFragment.ExternalMediaListener,
    AddSellerDialogFragment.AddSellerListener, AddSellerLocationDialogFragment.AddSellerLocationListener {

    private lateinit var binding: FragmentAddDetailedTransactionOtherDetailsBinding
    private lateinit var viewModel: AddDetailedTransactionViewModel
    private var datePicker: MaterialDatePicker<Long>? = null
    private var timePicker: MaterialTimePicker? = null
    private var customLocalTime: LocalTime? = null
    private var customLocalDate: LocalDate? = null
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
//    private var addSellerDialogFragment: AddSellerDialogFragment? = null
//    private var addSellerLocationDialogFragment: AddSellerLocationDialogFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider.create(requireActivity()).get(AddDetailedTransactionViewModel::class.java)
        binding = FragmentAddDetailedTransactionOtherDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    var items = emptyList<AddEditTransactionFile>()
    var renderers = emptyMap<Uri, PdfRenderer>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireContext().applicationContext as ExpenseTracker).appComponent.inject(this)
        binding.textViewAddDetailedTransactionAddEvidence.setOnClickListener(this)
        binding.textViewAddDetailedTransactionCustomDate.setOnClickListener(this)
        binding.textViewAddDetailedTransactionCustomTime.setOnClickListener(this)
        binding.imageViewAddDetailedTransactionCustomDateRemove.setOnClickListener(this)
        binding.imageViewAddDetailedTransactionCustomTimeRemove.setOnClickListener(this)
        binding.imageViewAddDetailedTransactionAddSeller.setOnClickListener(this)

        val adapter = AddTransactionEvidenceRecyclerAdapter(emptyList(), emptyMap(), this)
        binding.recyclerviewAddDetailedTransactionEvidence.adapter = adapter
        binding.recyclerviewAddDetailedTransactionEvidence.layoutManager = LinearLayoutManager(requireContext())
        val mode = viewModel.mode

        val modeAdapter = TransactionModeAdapter(requireContext(), TransactionMode.values().toList())

        val modeListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val transactionMode = modeAdapter._objects[position]
                viewModel.setTransactionMode(transactionMode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerAddDetailedTransactionMode.adapter = modeAdapter
        binding.spinnerAddDetailedTransactionMode.onItemSelectedListener = modeListener

        /*val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                LOGGER.debug("afterTextChanged")
                val text = s!!.toString()
                val length = text.length
                val codePointCount = text.codePointCount(0, length)
                binding.textViewAddDetailedTransactionNoteLengthIndicator.text = codePointCount.toString() + "/" + Constants.MAX_NOTE_CODEPOINT_LENGTH
                if(codePointCount > Constants.MAX_NOTE_CODEPOINT_LENGTH) {
                    LOGGER.info("afterTextChanged: Reached max code point count")
                    val breakIterator = BreakIterator.getCharacterInstance()
                    breakIterator.setText(text)
                    val lastGraphemePosition = if(breakIterator.isBoundary(text.offsetByCodePoints(0, Constants.MAX_NOTE_CODEPOINT_LENGTH))) {
                        text.offsetByCodePoints(0, Constants.MAX_NOTE_CODEPOINT_LENGTH)
                    } else {
                        breakIterator.preceding(text.offsetByCodePoints(0, Constants.MAX_NOTE_CODEPOINT_LENGTH))
                    }
                    if(lastGraphemePosition != BreakIterator.DONE) {
                        binding.editTextAddDetailedTransactionNote.removeTextChangedListener(this)
                        val substring = text.substring(0, lastGraphemePosition)
                        binding.editTextAddDetailedTransactionNote.setText(
                            substring
                        )
                        //TODO 2-way binding
                        viewModel.setNote(text)
                        binding.textViewAddDetailedTransactionNoteLengthIndicator.text = substring.codePointCount(0, substring.length).toString() + "/" + Constants.MAX_NOTE_CODEPOINT_LENGTH
                        binding.editTextAddDetailedTransactionNote.setSelection(lastGraphemePosition)
                        binding.editTextAddDetailedTransactionNote.addTextChangedListener(this)
                    }
                } else {
                    viewModel.setNote(text)
                }
            }
        }*/
        val textWatcher = MaxCodePointWatcher(binding.editTextAddDetailedTransactionNote, Constants.MAX_NOTE_CODEPOINT_LENGTH, binding.textViewAddDetailedTransactionNoteLengthIndicator) {
            LOGGER.debug("Description: \"{}\"", it)
            viewModel.setNote(it)
        }

        viewModel.transactionItemsLiveData.observe(viewLifecycleOwner) { (draft, _, _) ->
            items = draft.evidence
            adapter.setItems(items, renderers)


            customLocalTime = draft.customTime
            customLocalDate = draft.customDate

            if(draft.customDate == null) {
                if(mode == "edit") {
                    binding.textViewAddDetailedTransactionCustomDate.text = dateFormat.format(draft.dbOriginalDate!!)
                } else {
                    binding.textViewAddDetailedTransactionCustomDate.text = ""
                }
                binding.imageViewAddDetailedTransactionCustomDateRemove.visibility = View.GONE
            } else {
                binding.textViewAddDetailedTransactionCustomDate.text = dateFormat.format(draft.customDate)
                binding.imageViewAddDetailedTransactionCustomDateRemove.visibility = View.VISIBLE
            }
            if(draft.customTime == null) {
                if(mode == "edit") {
                    binding.textViewAddDetailedTransactionCustomTime.text = if(draft.dbOriginalTime != null) {
                        timeFormat.format(draft.dbOriginalTime!!)
                    } else {
                        ""
                    }
                } else {
                    binding.textViewAddDetailedTransactionCustomTime.text = ""
                }
                binding.imageViewAddDetailedTransactionCustomTimeRemove.visibility = View.GONE
            } else {
                binding.textViewAddDetailedTransactionCustomTime.text = timeFormat.format(draft.customTime)
                binding.imageViewAddDetailedTransactionCustomTimeRemove.visibility = View.VISIBLE
            }

            if(draft.sellerId != null) {
                binding.linearLayoutAddDetailedTransactionSellerLocation.visibility = View.VISIBLE
                binding.imageViewAddDetailedTransactionAddSellerLocation.setOnClickListener(this)
            } else {
                binding.linearLayoutAddDetailedTransactionSellerLocation.visibility = View.GONE
                binding.imageViewAddDetailedTransactionAddSellerLocation.setOnClickListener(null)
            }

            val transactionMode = draft.mode
            if(binding.spinnerAddDetailedTransactionMode.selectedItemPosition == Spinner.INVALID_POSITION) {
                binding.spinnerAddDetailedTransactionMode.onItemSelectedListener = null
                binding.spinnerAddDetailedTransactionMode.setSelection(0)
                binding.spinnerAddDetailedTransactionMode.onItemSelectedListener = modeListener
            }

            val modePosition = modeAdapter._objects.indexOfFirst { it == transactionMode }
            binding.spinnerAddDetailedTransactionMode.onItemSelectedListener = null

            if(modePosition == -1) {
                LOGGER.warn("Current mode not available in spinner")
                binding.spinnerAddDetailedTransactionMode.setSelection(0)
            } else {
                LOGGER.info("Current mode of spinner changed")
                binding.spinnerAddDetailedTransactionMode.setSelection(modePosition)
            }
            binding.spinnerAddDetailedTransactionMode.onItemSelectedListener = modeListener
            if(!binding.editTextAddDetailedTransactionNote.hasFocus()) {
                binding.editTextAddDetailedTransactionNote.removeTextChangedListener(textWatcher)
                binding.editTextAddDetailedTransactionNote.setText(draft.note)
                binding.editTextAddDetailedTransactionNote.addTextChangedListener(textWatcher)
            }
            val noteText = draft.note?:""
            binding.textViewAddDetailedTransactionNoteLengthIndicator.text = noteText.codePointCount(0, noteText.length).toString() + "/" + Constants.MAX_NOTE_CODEPOINT_LENGTH
        }
        val accountAdapter = AccountAdapter(requireContext(), mutableListOf<AccountUi>())

        val accountListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val account = accountAdapter._objects[position]
                viewModel.setAccountId(account.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerAddDetailedTransactionAccount.adapter = accountAdapter
        binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = accountListener

//        viewModel.currentAccount.observe(viewLifecycleOwner) { (accounts, account, total) ->
        viewModel.mediator.observe(viewLifecycleOwner) { (accounts, account, total) ->
            accountAdapter.setItems(accounts.map { accountDbToAccountUi(it, timeAndLocaleHandler.getLocale()) })
            if(binding.spinnerAddDetailedTransactionAccount.selectedItemPosition == Spinner.INVALID_POSITION) {
                binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = null
                binding.spinnerAddDetailedTransactionAccount.setSelection(0)
                binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = accountListener
            }

            val accountPosition = accountAdapter._objects.indexOfFirst { it.id == account.id }
            binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = null

            if(accountPosition == -1) {
                LOGGER.warn("Current Account not available in spinner. Changing selection to first item")
                binding.spinnerAddDetailedTransactionAccount.setSelection(0)
            } else {
                LOGGER.info("Current Account of spinner changed")
                binding.spinnerAddDetailedTransactionAccount.setSelection(accountPosition)
            }
            binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = accountListener
        }

        val sellerAdapter = SellerAdapter(requireContext(), mutableListOf<SellerUi>())

        val sellerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position == 0) {
                    viewModel.setSeller(null)
                } else {
                    val seller = sellerAdapter._objects[position]
                    viewModel.setSeller(seller?.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerAddDetailedTransactionSeller.adapter = sellerAdapter
        binding.spinnerAddDetailedTransactionSeller.onItemSelectedListener = sellerListener

        viewModel.sellersMediatorLiveData.observe(viewLifecycleOwner) { (seller, sellers) ->
            sellerAdapter.setItems(sellers)
            if(binding.spinnerAddDetailedTransactionSeller.selectedItemPosition == Spinner.INVALID_POSITION) {
                binding.spinnerAddDetailedTransactionSeller.onItemSelectedListener = null
                binding.spinnerAddDetailedTransactionSeller.setSelection(0)
                binding.spinnerAddDetailedTransactionSeller.onItemSelectedListener = sellerListener
            }

            val sellerPosition = sellerAdapter._objects.indexOfFirst { it != null && it.id == seller }
            binding.spinnerAddDetailedTransactionSeller.onItemSelectedListener = null

            if(sellerPosition == -1) {
                LOGGER.warn("Current Seller not available in spinner. Changing selection to the null item")
                binding.spinnerAddDetailedTransactionSeller.setSelection(0)
            } else {
                LOGGER.info("Current seller of spinner changed")
                binding.spinnerAddDetailedTransactionSeller.setSelection(sellerPosition)
            }
            binding.spinnerAddDetailedTransactionSeller.onItemSelectedListener = sellerListener
        }

        val sellerLocationAdapter = SellerLocationAdapter(requireContext(), mutableListOf<SellerLocationUi>())

        val sellerLocationListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position == 0) {
                    viewModel.setSellerLocation(null)
                } else {
                    val sellerLocation = sellerLocationAdapter._objects[position]
                    viewModel.setSellerLocation(sellerLocation)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerAddDetailedTransactionSellerLocation.adapter = sellerLocationAdapter
        binding.spinnerAddDetailedTransactionSellerLocation.onItemSelectedListener = sellerLocationListener

        viewModel.sellerLocationsMediatorLiveData.observe(viewLifecycleOwner) { (sellerLocationUi, sellers) ->
            sellerLocationAdapter.setItems(sellers)
            if(binding.spinnerAddDetailedTransactionSellerLocation.selectedItemPosition == Spinner.INVALID_POSITION) {
                binding.spinnerAddDetailedTransactionSellerLocation.onItemSelectedListener = null
                binding.spinnerAddDetailedTransactionSellerLocation.setSelection(0)
                binding.spinnerAddDetailedTransactionSellerLocation.onItemSelectedListener = sellerLocationListener
            }

            val sellerLocationPosition = sellerLocationAdapter._objects.indexOfFirst { it.id == sellerLocationUi?.id }
            binding.spinnerAddDetailedTransactionSellerLocation.onItemSelectedListener = null

            if(sellerLocationPosition == -1) {
                LOGGER.warn("Current Seller location not available in spinner. Changing selection to the null item")
                binding.spinnerAddDetailedTransactionSellerLocation.setSelection(0)
            } else {
                LOGGER.info("Current seller location of spinner changed")
                binding.spinnerAddDetailedTransactionSellerLocation.setSelection(sellerLocationPosition)
            }
            binding.spinnerAddDetailedTransactionSellerLocation.onItemSelectedListener = sellerLocationListener
        }

        viewModel.rendererLiveData.observe(viewLifecycleOwner) { map ->
            renderers = map
            adapter.setItems(items, renderers)
        }

        binding.editTextAddDetailedTransactionNote.addTextChangedListener(textWatcher)
    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.textViewAddDetailedTransactionAddEvidence -> {
                    if(items.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_EVIDENCE) {
                        LOGGER.info("Reached max evidence count")
                        return
                    }
                    val externalMediaDialog = AddExternalMediaDialogFragment.newInstance(false)
                    externalMediaDialog.show(childFragmentManager,
                        DIALOG_TAG_EXTERNAL_MEDIA_PICKER
                    )
                    LOGGER.info("Opened dialog externalMediaPicker")
                }
                binding.textViewAddDetailedTransactionCustomDate -> {
                    datePicker = childFragmentManager.findFragmentByTag(DIALOG_TAG_DATE) as MaterialDatePicker<Long>?
                    if(datePicker == null) {
                        LOGGER.info("datePicker is null. Creating")
                        val calendarConstraints = CalendarConstraints.Builder()
                            .setValidator(DateValidatorPointBackward.now())
                            .build()
                        val builder = MaterialDatePicker.Builder.datePicker()
                            .setCalendarConstraints(calendarConstraints)
                        if(customLocalDate != null) {
                            val millis = customLocalDate!!.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                            builder.setSelection(millis)
                            LOGGER.info("Using existing date for datePicker")
                        } else {
                            val millis = Instant.now(timeAndLocaleHandler.getClock()).toEpochMilli()
                            builder.setSelection(millis)
                            LOGGER.info("Using current date for datePicker")
                        }
                        datePicker = builder
                            .build()
                        datePicker!!.addOnPositiveButtonClickListener(this)
                    }
                    if(!(datePicker?.dialog?.isShowing?: false)) {
                        datePicker!!.show(childFragmentManager, DIALOG_TAG_DATE)
                        LOGGER.info("Showed datePicker")
                    }
                }
                binding.textViewAddDetailedTransactionCustomTime -> {
                    timePicker = childFragmentManager.findFragmentByTag(DIALOG_TAG_TIME) as MaterialTimePicker?
                    if(timePicker == null) {
                        LOGGER.info("timePicker is null. Creating")
                        val builder = MaterialTimePicker.Builder()
                        if(customLocalTime != null) {
                            builder.setHour(customLocalTime!!.hour)
                            builder.setMinute(customLocalTime!!.minute)
                            LOGGER.info("Using existing custom time for timePicker")
                        } else {
//                            val localTime = LocalTime.now(timeAndLocaleHandler.getClock())
//                            builder.setHour(localTime!!.hour)
//                            builder.setMinute(localTime!!.minute)
//                            LOGGER.info("Using current time for timePicker")
                            //TODO When the date is "today", the time should display the current time by default and setting a custom time fixes it to that time
                            // When the date is not "today", then instead the time should display nothing by default and setting a custom time still fixes it
                            // Take care of that when I change things such that time is used to determine ordinals
                        }
                        val is24Hour = DateFormat.is24HourFormat(requireContext())
                        val format = if(is24Hour) {
                            TimeFormat.CLOCK_24H
                        } else {
                            TimeFormat.CLOCK_12H
                        }
                        timePicker = builder.setTimeFormat(format)
                            .build()
                        timePicker!!.addOnPositiveButtonClickListener {
                            //TODO Check for impossible future time
                            LOGGER.info("timePicker time set")
                            val time = LocalTime.of(timePicker!!.hour, timePicker!!.minute)
                            viewModel.setCustomTime(time)
                        }
                    }
                    if(!(timePicker?.dialog?.isShowing?: false)) {
                        timePicker!!.show(childFragmentManager, DIALOG_TAG_TIME)
                        LOGGER.info("Showed timePicker")
                    }
                }
                binding.imageViewAddDetailedTransactionCustomDateRemove -> {
                    viewModel.setCustomDate(null)
                }
                binding.imageViewAddDetailedTransactionCustomTimeRemove -> {
                    viewModel.setCustomTime(null)
                }
                binding.imageViewAddDetailedTransactionAddSeller -> {
//                    if(addSellerDialogFragment == null) {
//                        LOGGER.info("AddSeller dialog is null, creating")
//                    }
                    val  addSellerDialogFragment = AddSellerDialogFragment()
//                    if(!(addSellerDialogFragment?.dialog?.isShowing?:false)) {
                        addSellerDialogFragment?.show(childFragmentManager,
                            DIALOG_TAG_ADD_SELLER
                        )
                        LOGGER.info("Showed addSellerDialog")
//                    }
                }
                binding.imageViewAddDetailedTransactionAddSellerLocation -> {
//                    if(addSellerLocationDialogFragment == null) {
//                        LOGGER.info("AddSellerLocation dialog is null, creating")
//                    }
                    val addSellerLocationDialogFragment = AddSellerLocationDialogFragment.createDialog(viewModel.getSellerId()!!)
//                    if(!(addSellerLocationDialogFragment?.dialog?.isShowing?:false)) {
                        addSellerLocationDialogFragment?.show(childFragmentManager,
                            DIALOG_TAG_ADD_SELLER_LOCATION
                        )
                        LOGGER.info("Showed addSellerLocationDialog")
//                    }
                }
            }
        }
    }

    override fun onSelectionMade(selection: AddExternalMediaDialogFragment.Selection, itemOrEvidence: Boolean, itemId: Int?) {
        when(selection) {
            AddExternalMediaDialogFragment.Selection.LocalImage -> {}
            AddExternalMediaDialogFragment.Selection.Camera -> {
                if(items.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_EVIDENCE) {
                    LOGGER.info("Reached max evidence count")
                    return
                }

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val cameraDirectory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
                if(cameraFile.exists()) {
                    LOGGER.info("Camera Intent file already exists")
                    val delete = cameraFile.delete()
                    LOGGER.info("Delete existing camera file: $delete")
                }
                val uri = FileProvider.getUriForFile(requireContext().applicationContext, requireContext().applicationContext.packageName + ".provider", cameraFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                try {
                    requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_DOCUMENT_CAPTURE_IMAGE)
                    LOGGER.info("startedActivityForResult called to open camera evidence")
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "Unable to open camera", Toast.LENGTH_SHORT).show()
                    LOGGER.error("Unable to open camera", e)
                }
            }
            AddExternalMediaDialogFragment.Selection.DevicePicker -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
                requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_OPEN_DOCUMENT)
                LOGGER.info("startedActivityForResult called to get evidence")
            }
        }
    }

    override fun onPositiveButtonClick(selection: Long?) {
        selection?.let {
            LOGGER.info("datePicker date picked")
            val instant = Instant.ofEpochMilli(it)
            val localDate = instant.atZone(timeAndLocaleHandler.getZone()).toLocalDate()
            viewModel.setCustomDate(localDate)
        }
    }

    override fun onDeleteEvidence(position: Int, uri: Uri) {
        viewModel.onDeleteEvidence(position, uri)
    }

    override fun onAddSeller(name: String) {
        viewModel.addSeller(name)
    }

    override fun onAddSellerLocation(location: String, sellerId: Long) {
        viewModel.addSellerLocation(location, sellerId)
    }

    companion object {
        @JvmStatic
        fun newInstance(): AddDetailedTransactionOtherDetailsFragment {
            val fragment = AddDetailedTransactionOtherDetailsFragment()
            return fragment
        }
        
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionOtherDetailsFragment::class.java)
        private const val DIALOG_TAG_DATE = "dateDialog"
        private const val DIALOG_TAG_TIME = "timeDialog"
        private const val DIALOG_TAG_EXTERNAL_MEDIA_PICKER = "externalMediaPicker"
        private const val DIALOG_TAG_ADD_SELLER = "addSeller"
        private const val DIALOG_TAG_ADD_SELLER_LOCATION = "addSellerLocation"
    }
}