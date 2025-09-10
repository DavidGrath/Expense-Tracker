package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionOtherDetailsBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionEvidence
import com.ibm.icu.text.BreakIterator

class AddDetailedTransactionOtherDetailsFragment: Fragment(), OnClickListener {

    lateinit var binding: FragmentAddDetailedTransactionOtherDetailsBinding
    lateinit var viewModel: AddDetailedTransactionViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider.create(requireActivity()).get(AddDetailedTransactionViewModel::class.java)
        binding = FragmentAddDetailedTransactionOtherDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    var items = emptyList<AddTransactionEvidence>()
    var renderers = emptyMap<Uri, PdfRenderer>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewAddDetailedTransactionAddEvidence.setOnClickListener(this)
        val adapter = AddTransactionEvidenceRecyclerAdapter(emptyList(), emptyMap())
        binding.recyclerviewAddDetailedTransactionEvidence.adapter = adapter
        binding.recyclerviewAddDetailedTransactionEvidence.layoutManager = LinearLayoutManager(requireContext())
        viewModel.transactionItemsLiveData.observe(viewLifecycleOwner) { (draft, _, _) ->
            items = draft.evidence
            adapter.setItems(items, renderers)
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
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.type = "*/*"
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
                    requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_OPEN_DOCUMENT)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): AddDetailedTransactionOtherDetailsFragment {
            val fragment = AddDetailedTransactionOtherDetailsFragment()
            return fragment
        }
    }
}