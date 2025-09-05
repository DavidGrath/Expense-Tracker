package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionOtherDetailsBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionEvidence

class AddDetailedTransactionOtherDetailsFragment: Fragment(), OnClickListener {

    lateinit var binding: FragmentAddDetailedTransactionOtherDetailsBinding
    lateinit var viewModel: AddDetailedTransactionViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider.create(requireActivity()).get(AddDetailedTransactionViewModel::class.java)
        binding = FragmentAddDetailedTransactionOtherDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    var items = emptyList<AddTransactionEvidence>()
    var renderers = emptyMap<Int, PdfRenderer>()

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