package com.davidgrath.expensetracker.ui.main.documents

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.FragmentImageStatsBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewDocumentStatsBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewImageStatsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.views.EvidenceWithTransactionDateAndOrdinal
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.entities.ui.TransactionDetailItemUi
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionGetImageRecyclerAdapter
import com.davidgrath.expensetracker.ui.addtransaction.AddTransactionEvidenceRecyclerAdapter
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.Measure
import com.ibm.icu.util.MeasureUnit
import com.ibm.icu.util.MeasureUnit.BIT
import org.slf4j.LoggerFactory
import systems.uom.quantity.Information
import tech.units.indriya.quantity.Quantities
import tech.units.indriya.unit.Units
import javax.measure.Quantity

class DocumentStatsRecyclerAdapter(private var items: List<EvidenceWithTransactionDateAndOrdinal>, var pdfRenderers: Map<Uri, PdfRenderer>, val timeAndLocaleHandler: TimeAndLocaleHandler): RecyclerView.Adapter<DocumentStatsRecyclerAdapter.DocumentStatsViewHolder>() {

    //TODO Cache bitmaps, too
    private var pageZeroMaps = hashMapOf<PdfRenderer, PdfRenderer.Page>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentStatsViewHolder {
        val height = parent.width / 3
        val width = parent.width / 3
        val layoutParams = LinearLayout.LayoutParams(width, height)
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewDocumentStatsBinding.inflate(inflater, parent, false)
        val imageView = binding.imageViewDocumentStatsImage
        imageView.layoutParams = layoutParams
        val bindingLayoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.root.layoutParams = bindingLayoutParams
        return DocumentStatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentStatsViewHolder, position: Int) {
        val binding = holder.binding
        val evidence = items[position]
        val uri = Uri.parse(evidence.uri)
        val mimeType = evidence.mimeType

        when(mimeType) {
            "image/jpeg", "image/png" -> {
                Glide.with(binding.imageViewDocumentStatsImage.context)
                    .load(uri)
                    .centerCrop()
                    .into(binding.imageViewDocumentStatsImage)
            }
            "application/pdf" -> {

                val renderer = pdfRenderers[uri]
                if(renderer == null) {
                    Glide.with(binding.imageViewDocumentStatsImage.context)
                        .load(R.drawable.baseline_add_24)
                        .into(binding.imageViewDocumentStatsImage)
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
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    Glide.with(binding.imageViewDocumentStatsImage.context)
                        .load(bitmap)
                        .into(binding.imageViewDocumentStatsImage)

                }
            }
        }

        binding.imageViewDocumentStatsImage.setOnClickListener {
            //TODO Shared element transition
        }
        binding.textViewDocumentStatsSizeBytes.text = evidence.sizeBytes.formatBytes(timeAndLocaleHandler.getLocale())
        binding.textViewDocumentStatsDate.text = evidence.transactionDatedAt
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: List<EvidenceWithTransactionDateAndOrdinal>, pdfRenderers: Map<Uri, PdfRenderer>) {
        LOGGER.info("setItems: List size: {}, pdfRenderers size: {}", items.size, pdfRenderers.size)
        this.items = items
        this.pdfRenderers = pdfRenderers
        notifyDataSetChanged()
    }

    class DocumentStatsViewHolder(val binding: RecyclerviewDocumentStatsBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DocumentStatsRecyclerAdapter::class.java)
    }
}