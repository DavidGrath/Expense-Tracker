package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionOtherDetailsBinding
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.ui.AccountAdapter
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
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import javax.inject.Inject

class AddDetailedTransactionOtherDetailsFragment: Fragment(), OnClickListener, OnCheckedChangeListener, MaterialPickerOnPositiveButtonClickListener<Long> {

    private lateinit var binding: FragmentAddDetailedTransactionOtherDetailsBinding
    private lateinit var viewModel: AddDetailedTransactionViewModel
    private var datePicker: MaterialDatePicker<Long>? = null
    private var timePicker: MaterialTimePicker? = null
    private var customLocalTime: LocalTime? = null
    private var customLocalDate: LocalDate? = null
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    @Inject
    lateinit var timeHandler: TimeHandler

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

        binding.checkBoxAddDetailedTransactionUseCustomDateTime.setOnCheckedChangeListener(this)
        val adapter = AddTransactionEvidenceRecyclerAdapter(emptyList(), emptyMap())
        binding.recyclerviewAddDetailedTransactionEvidence.adapter = adapter
        binding.recyclerviewAddDetailedTransactionEvidence.layoutManager = LinearLayoutManager(requireContext())
        val mode = viewModel.mode
        viewModel.transactionItemsLiveData.observe(viewLifecycleOwner) { (draft, _, _) ->
            items = draft.evidence
            adapter.setItems(items, renderers)
            binding.checkBoxAddDetailedTransactionUseCustomDateTime.setOnCheckedChangeListener(null)
            binding.checkBoxAddDetailedTransactionUseCustomDateTime.isChecked = draft.useCustomDateTime
            if(mode == "edit") {
                binding.checkBoxAddDetailedTransactionUseCustomDateTime.visibility = View.GONE
                binding.linearLayoutAddDetailedTransactionDateTime.visibility = View.VISIBLE
            } else {
                binding.checkBoxAddDetailedTransactionUseCustomDateTime.visibility = View.VISIBLE
                binding.linearLayoutAddDetailedTransactionDateTime.visibility = if(draft.useCustomDateTime) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            binding.checkBoxAddDetailedTransactionUseCustomDateTime.setOnCheckedChangeListener(this)
            customLocalTime = draft.customTime
            customLocalDate = draft.customDate

            if(draft.customDate == null) {
                if(mode == "edit") {
                    binding.textViewAddDetailedTransactionCustomDate.text = dateFormat.format(draft.dbOriginalDate!!)
                    val zone = draft.dbOriginalZoneId
                    if(zone != timeHandler.getZone()) {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    } else {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    }
                } else {
                    binding.textViewAddDetailedTransactionCustomDate.text = ""
                }
                binding.imageViewAddDetailedTransactionCustomDateRemove.visibility = View.GONE
            } else {
                binding.textViewAddDetailedTransactionCustomDate.text = dateFormat.format(draft.customDate)
                binding.imageViewAddDetailedTransactionCustomDateRemove.visibility = View.VISIBLE
                if(mode == "edit") {
                    val zone = draft.dbOriginalZoneId
                    if(zone != timeHandler.getZone()) {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    } else {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    }
                }
            }
            if(draft.customTime == null) {
                if(mode == "edit") {
                    binding.textViewAddDetailedTransactionCustomTime.text = timeFormat.format(draft.dbOriginalTime!!)
                    val zone = draft.dbOriginalZoneId
                    if(zone != timeHandler.getZone()) {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    } else {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    }
                } else {
                    binding.textViewAddDetailedTransactionCustomTime.text = ""
                }
                binding.imageViewAddDetailedTransactionCustomTimeRemove.visibility = View.GONE
            } else {
                binding.textViewAddDetailedTransactionCustomTime.text = timeFormat.format(draft.customTime)
                binding.imageViewAddDetailedTransactionCustomTimeRemove.visibility = View.VISIBLE
                if(mode == "edit") {
                    val zone = draft.dbOriginalZoneId
                    if(zone != timeHandler.getZone()) {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    } else {
                        binding.textViewAddDetailedTransactionZoneDifferenceNotice.visibility = View.VISIBLE
                    }
                }
            }
        }
        val accountAdapter = AccountAdapter(requireContext(), mutableListOf<AccountUi>())

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val account = accountAdapter._objects[position]
                viewModel.setAccountId(account.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerAddDetailedTransactionAccount.adapter = accountAdapter
        binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = listener
        viewModel.accountsLiveData.observe(viewLifecycleOwner) {
            LOGGER.debug("Accounts list size: {}", it.size)
            accountAdapter.setItems(it)
            if(binding.spinnerAddDetailedTransactionAccount.selectedItemPosition == Spinner.INVALID_POSITION) {
                binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = null
                binding.spinnerAddDetailedTransactionAccount.setSelection(0)
                binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = listener
            }
        }
        viewModel.currentAccount.observe(viewLifecycleOwner) { (account, total) ->
            val accountPosition = accountAdapter._objects.indexOfFirst { it.id == account.id }
            binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = null

            if(accountPosition == -1) {
                LOGGER.warn("Current Account not available in spinner. Changing selection to first item")
                binding.spinnerAddDetailedTransactionAccount.setSelection(0)
            } else {
                LOGGER.info("Current Account of spinner changed")
                binding.spinnerAddDetailedTransactionAccount.setSelection(accountPosition)
            }
            binding.spinnerAddDetailedTransactionAccount.onItemSelectedListener = listener
        }
        viewModel.rendererLiveData.observe(viewLifecycleOwner) { map ->
            renderers = map
            adapter.setItems(items, renderers)
        }
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
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
        }
        val noteText = binding.editTextAddDetailedTransactionNote.text.toString()
        binding.textViewAddDetailedTransactionNoteLengthIndicator.text = noteText.codePointCount(0, noteText.length).toString() + "/" + Constants.MAX_NOTE_CODEPOINT_LENGTH
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
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.type = "*/*"
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
                    requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_OPEN_DOCUMENT)
                    LOGGER.info("startedActivityForResult called to get evidence")
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
                            val millis = Instant.now().toEpochMilli()
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
                        if(customLocalTime != null) { //TODO Default date and time to now if Material doesn't do that already
                            builder.setHour(customLocalTime!!.hour)
                            builder.setMinute(customLocalTime!!.minute)
                            LOGGER.info("Using existing time for timePicker")
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
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when(buttonView) {
            binding.checkBoxAddDetailedTransactionUseCustomDateTime -> {
                viewModel.setUseCustomDateTime(isChecked)
            }
        }
    }

    override fun onPositiveButtonClick(selection: Long?) {
        selection?.let {
            LOGGER.info("datePicker date picked")
            val instant = Instant.ofEpochMilli(it)
            val localDate = instant.atZone(timeHandler.getZone()).toLocalDate()
            viewModel.setCustomDate(localDate)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): AddDetailedTransactionOtherDetailsFragment {
            val fragment = AddDetailedTransactionOtherDetailsFragment()
            return fragment
        }
        
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionOtherDetailsFragment::class.java)
        private val DIALOG_TAG_DATE = "dateDialog"
        private val DIALOG_TAG_TIME = "timeDialog"
    }
}