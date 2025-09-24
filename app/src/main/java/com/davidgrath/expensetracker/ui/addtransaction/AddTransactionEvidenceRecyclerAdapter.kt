package com.davidgrath.expensetracker.ui.addtransaction

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import org.slf4j.LoggerFactory

class AddTransactionEvidenceRecyclerAdapter(private var evidenceList: List<AddEditTransactionFile>, var pdfRenderers: Map<Uri, PdfRenderer>): RecyclerView.Adapter<AddTransactionEvidenceRecyclerAdapter.AddTransactionEvidenceViewHolder>() {

    //TODO Cache bitmaps, too
    private var pageZeroMaps = hashMapOf<PdfRenderer, Page>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionEvidenceViewHolder {
        val height = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_evidence_height)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        val imageView = ImageView(parent.context)
        imageView.layoutParams = layoutParams
        return AddTransactionEvidenceViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: AddTransactionEvidenceViewHolder, position: Int) {
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

    fun setItems(evidenceList: List<AddEditTransactionFile>, pdfRenderers: Map<Uri, PdfRenderer>) {
        LOGGER.info("setItems: List size: {}, pdfRenderers size: {}", evidenceList.size, pdfRenderers.size)
        this.evidenceList = evidenceList
        this.pdfRenderers = pdfRenderers
        notifyDataSetChanged()
    }

    class AddTransactionEvidenceViewHolder(val imageView: ImageView): ViewHolder(imageView) {

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddTransactionEvidenceRecyclerAdapter::class.java)
    }
}