package com.davidgrath.expensetracker.ui.main.documents

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentDocumentStatsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.views.EvidenceWithTransactionDateAndOrdinal
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.loadRenderer
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsActivity
import com.davidgrath.expensetracker.ui.main.pdfdetails.PdfDetailsActivity
import com.davidgrath.expensetracker.utils.DocumentClickListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class DocumentStatsFragment: Fragment() {

    lateinit var binding: FragmentDocumentStatsBinding
    lateinit var viewModel: DocumentStatsViewModel
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    private var renderersMap = mutableMapOf<Uri, PdfRenderer>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val app = requireContext().applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)
        viewModel = ViewModelProvider(viewModelStore, DocumentStatsViewModelFactory(appComponent)).get(DocumentStatsViewModel::class.java)
        binding = FragmentDocumentStatsBinding.inflate(layoutInflater, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = DocumentStatsRecyclerAdapter(emptyList(), emptyMap(), timeAndLocaleHandler, object : DocumentClickListener {
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
        val layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewDocumentStats.adapter = adapter
        binding.recyclerViewDocumentStats.layoutManager = layoutManager
        var disposable: Disposable? = null
        viewModel.documents.observe(viewLifecycleOwner) { documents ->
            if(disposable?.isDisposed == false) {
                disposable?.dispose()
            }
            disposable = loadRenderers(documents).subscribeOn(Schedulers.io()).observeOn(
                AndroidSchedulers.mainThread()).subscribe ({
                adapter.setItems(documents, renderersMap as Map<Uri, PdfRenderer>)
            }, {})
            adapter.setItems(documents, renderersMap)
            binding.textViewDocumentStatsOverallCount.text = "${documents.size} images" //TODO Localization, Pluralization
        }
        viewModel.totalSize.observe(viewLifecycleOwner) {
            binding.textViewDocumentStatsOverallSize.text = it.formatBytes(timeAndLocaleHandler.getLocale())
        }
    }

    fun loadRenderers(evidenceList: List<EvidenceWithTransactionDateAndOrdinal>): Single<Unit> {
        return Single.fromCallable {
            val newMap = mutableMapOf<Uri, PdfRenderer>()
            for(evidence in evidenceList) {
                val uri = Uri.parse(evidence.uri)
                val existingRenderer = renderersMap[uri]
                if(existingRenderer != null) {
                    newMap[uri] = existingRenderer
                    continue
                }
                val renderer = loadRenderer(uri).blockingGet()
                if(renderer != null) {
                    newMap[uri] = renderer
                }
            }
            renderersMap = newMap
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): DocumentStatsFragment {
            val documentStatsFragment = DocumentStatsFragment()
            return documentStatsFragment
        }
    }
}