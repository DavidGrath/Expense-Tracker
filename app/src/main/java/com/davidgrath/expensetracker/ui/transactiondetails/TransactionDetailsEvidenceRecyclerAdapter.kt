package com.davidgrath.expensetracker.ui.transactiondetails

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionDetailsDocumentBinding
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.utils.DocumentClickListener

class TransactionDetailsEvidenceRecyclerAdapter(private var evidenceList: List<EvidenceUi>, private var pdfRenderers: Map<Uri, PdfRenderer>, private val listener: DocumentClickListener? = null): RecyclerView.Adapter<TransactionDetailsEvidenceRecyclerAdapter.TransactionDetailsEvidenceViewHolder>() {

    private var pageZeroMaps = hashMapOf<PdfRenderer, Page>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionDetailsEvidenceViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewTransactionDetailsDocumentBinding.inflate(inflater, parent, false)
        val viewHolder =
            TransactionDetailsEvidenceViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: TransactionDetailsEvidenceViewHolder, position: Int) {
        val binding = holder.binding
        val image = binding.imageViewTransactionDetailsDocument
        val evidence = evidenceList[position]
        val uri = evidence.uri
        val mimeType = evidence.mimeType
        image.setOnClickListener {
            listener?.onDocumentClicked(evidence.id, mimeType)
        }
        when(mimeType) {
            "image/jpeg", "image/png" -> {
                binding.progressBarTransactionDetailsDocument.visibility = View.VISIBLE
                binding.imageViewTransactionDetailsDocument.visibility = View.GONE
                Glide.with(image.context)
                    .load(uri)
                    .listener(object: RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBarTransactionDetailsDocument.visibility = View.GONE
                            binding.imageViewTransactionDetailsDocument.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .centerCrop()
                    .into(image)
            }
            "application/pdf" -> {

                val renderer = pdfRenderers[uri]
                if(renderer == null) {
                    binding.progressBarTransactionDetailsDocument.visibility = View.VISIBLE
                    binding.imageViewTransactionDetailsDocument.visibility = View.GONE
                } else {


                    val mapPage = pageZeroMaps[renderer]
                    val page = if(mapPage == null){
                        val p = renderer.openPage(0)
                        pageZeroMaps[renderer] = p
                        p
                    } else {
                        mapPage
                    }
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY)
                    Glide.with(image.context)
                        .load(bitmap)
                        .listener(object: RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.progressBarTransactionDetailsDocument.visibility = View.GONE
                                binding.imageViewTransactionDetailsDocument.visibility = View.VISIBLE
                                return false
                            }
                        })
                        .into(image)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return evidenceList.size
    }

    fun changeItems(evidenceList: List<EvidenceUi>) {
        this.evidenceList = evidenceList
        notifyDataSetChanged()
    }

    fun changeRenderers(pdfRenderers: Map<Uri, PdfRenderer>) {
        this.pdfRenderers = pdfRenderers
        notifyDataSetChanged()
    }

    class TransactionDetailsEvidenceViewHolder(val binding: RecyclerviewTransactionDetailsDocumentBinding): ViewHolder(binding.root) {

    }
}