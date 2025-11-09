package com.davidgrath.expensetracker.ui.addtransaction

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddEditEvidenceBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewAddEditItemImageBinding
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import org.slf4j.LoggerFactory

class AddTransactionEvidenceRecyclerAdapter(private var evidenceList: List<AddEditTransactionFile>, var pdfRenderers: Map<Uri, PdfRenderer>, private var listener: EvidenceClickListener? = null): RecyclerView.Adapter<AddTransactionEvidenceRecyclerAdapter.AddTransactionEvidenceViewHolder>() {

    private var selectedItemPosition = -1

    interface EvidenceClickListener {
        fun onDeleteEvidence(position: Int, uri: Uri)
    }

    //TODO Cache bitmaps, too
    private var pageZeroMaps = hashMapOf<PdfRenderer, Page>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionEvidenceViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewAddEditEvidenceBinding.inflate(inflater, parent, false)
        val viewHolder =
            AddTransactionEvidenceViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: AddTransactionEvidenceViewHolder, position: Int) {
        val image = holder.binding.imageViewAddEditEvidenceMain
        val evidence = evidenceList[position]
        val uri = evidence.uri
        val mimeType = evidence.mimeType
        holder.binding.root.setOnClickListener {
            val pos = holder.absoluteAdapterPosition
            val xEvidence = evidenceList[pos]
            val xMimeType = xEvidence.mimeType
            val xUri = xEvidence.uri
            if(selectedItemPosition == pos) {
                if(xMimeType == "application/pdf") {
                    val xRenderer = pdfRenderers[xUri]
                    val pageZero = pageZeroMaps.remove(xRenderer)
                    if(pageZero != null) {
                        LOGGER.info("Removed PdfRenderer first page")
                        pageZero.close()
                        LOGGER.info("Closed PdfRenderer first page")
                    }
                    //TODO Release Glide resources, too
                }
                selectedItemPosition = -1
                listener?.onDeleteEvidence(pos, xEvidence.uri)
            } else {
                val oldSelection = selectedItemPosition
                selectedItemPosition = pos
                notifyItemChanged(pos)
                if(oldSelection != -1) {
                    notifyItemChanged(oldSelection)
                }
            }
        }
        if(selectedItemPosition == position) {
            holder.binding.imageViewAddEditEvidenceSelectedIndicator.visibility = View.VISIBLE
        } else {
            holder.binding.imageViewAddEditEvidenceSelectedIndicator.visibility = View.GONE
        }
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

    class AddTransactionEvidenceViewHolder(val binding: RecyclerviewAddEditEvidenceBinding): ViewHolder(binding.root) {

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddTransactionEvidenceRecyclerAdapter::class.java)
    }
}