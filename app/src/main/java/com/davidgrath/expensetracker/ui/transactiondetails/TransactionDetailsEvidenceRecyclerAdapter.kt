package com.davidgrath.expensetracker.ui.transactiondetails

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toFile
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.AddTransactionEvidence
import com.davidgrath.expensetracker.entities.ui.EvidenceUi

class TransactionDetailsEvidenceRecyclerAdapter(private var evidenceList: List<EvidenceUi>, private var pdfRenderers: Map<Uri, PdfRenderer>): RecyclerView.Adapter<TransactionDetailsEvidenceRecyclerAdapter.TransactionDetailsEvidenceViewHolder>() {

    private var pageZeroMaps = hashMapOf<PdfRenderer, Page>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionDetailsEvidenceViewHolder {
        val height = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_evidence_height)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        val imageView = ImageView(parent.context)
        imageView.layoutParams = layoutParams
        return TransactionDetailsEvidenceViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: TransactionDetailsEvidenceViewHolder, position: Int) {
        val image = holder.imageView
        val evidence = evidenceList[position]
        val uri = evidence.uri
        val mimeType = evidence.mimeType
        when(mimeType) {
            "image/jpeg", "image/png" -> {
                Glide.with(image.context)
                    .load(uri)
                    .centerCrop()
                    .into(image)
            }
            "application/pdf" -> {

                val renderer = pdfRenderers[uri]
                if(renderer == null) {
                    Glide.with(image.context)
                        .load(R.drawable.baseline_add_24)
                        .into(image)
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

    class TransactionDetailsEvidenceViewHolder(val imageView: ImageView): ViewHolder(imageView) {

    }
}