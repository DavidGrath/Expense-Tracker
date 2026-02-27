package com.davidgrath.expensetracker.ui.transactiondetails

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.databinding.FragmentTransactionDetailsEvidenceBinding
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.loadRenderer
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsActivity
import com.davidgrath.expensetracker.ui.main.pdfdetails.PdfDetailsActivity
import com.davidgrath.expensetracker.utils.DocumentClickListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException

class TransactionDetailsEvidenceFragment: Fragment() {

    lateinit var binding: FragmentTransactionDetailsEvidenceBinding
    lateinit var viewModel: TransactionDetailsViewModel
    private var renderersMap = mutableMapOf<Uri, PdfRenderer>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionDetailsEvidenceBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(TransactionDetailsViewModel::class.java)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TransactionDetailsEvidenceRecyclerAdapter(emptyList(), emptyMap(), object: DocumentClickListener {
            override fun onDocumentClicked(documentId: Long, mimeType: String) {
                val intent: Intent
                if(mimeType == Constants.MimeTypes.PDF.type) {
                    intent = Intent(requireActivity(), PdfDetailsActivity::class.java)
                    PdfDetailsActivity.addExtras(intent, documentId)
                    startActivity(intent)
                } else {
                    intent = Intent(requireActivity(), ImageDetailsActivity::class.java)
                    ImageDetailsActivity.addExtras(intent, documentId, true)
                    startActivity(intent)
                }
            }
        })
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewTransactionDetailsEvidence.adapter = adapter
        binding.recyclerviewTransactionDetailsEvidence.layoutManager = layoutManager
        var disposable: Disposable? = null
        viewModel.evidence.observe(viewLifecycleOwner) { evidence ->
            if(disposable?.isDisposed == false) {
                disposable?.dispose()
            }
            disposable = loadRenderers(evidence).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe ({
                adapter.changeRenderers(renderersMap as Map<Uri, PdfRenderer>)
            }, {})
            adapter.changeItems(evidence)
        }
    }

    fun loadRenderers(evidenceList: List<EvidenceUi>): Single<Unit> {
        return Single.fromCallable {
            val newMap = mutableMapOf<Uri, PdfRenderer>()
            for(evidence in evidenceList) {
                val existingRenderer = renderersMap[evidence.uri]
                if(existingRenderer != null) {
                    newMap[evidence.uri] = existingRenderer
                    continue
                }
                val renderer = loadRenderer(evidence.uri).blockingGet()
                if(renderer != null) {
                    newMap[evidence.uri] = renderer
                }
            }
            renderersMap = newMap
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(): TransactionDetailsEvidenceFragment {
            val fragment = TransactionDetailsEvidenceFragment()
            return fragment
        }
    }
}