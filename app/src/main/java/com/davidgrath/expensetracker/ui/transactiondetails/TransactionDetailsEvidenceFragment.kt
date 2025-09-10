package com.davidgrath.expensetracker.ui.transactiondetails

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
import com.davidgrath.expensetracker.databinding.FragmentTransactionDetailsEvidenceBinding
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
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
        val adapter = TransactionDetailsEvidenceRecyclerAdapter(emptyList(), emptyMap())
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
                Log.d("ChangeRenderers", renderersMap.toString())
            }, {})
            adapter.changeItems(evidence)
            Log.d("Evidence", evidence.toString())
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
                val renderer = loadRenderer(evidence).blockingGet()
                if(renderer != null) {
                    newMap[evidence.uri] = renderer
                }
            }
            renderersMap = newMap
        }
    }

    fun loadRenderer(evidence: EvidenceUi): Maybe<PdfRenderer> {
        return Maybe.fromCallable {
            val file = evidence.uri.toFile()
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer: PdfRenderer? = try {
                PdfRenderer(fd)
            } catch (e: SecurityException) {
                null
            } catch (e: IOException) {
                null
            }
            return@fromCallable pdfRenderer
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