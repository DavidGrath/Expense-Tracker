package com.davidgrath.expensetracker.ui.main.pdfdetails

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityImageDetailsBinding
import com.davidgrath.expensetracker.databinding.ActivityPdfDetailsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.getLocationData
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionViewModel
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsActivity
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsViewModel
import com.davidgrath.expensetracker.ui.main.imagedetails.ImageDetailsViewModelFactory
import com.rajat.pdfviewer.PdfRendererView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import javax.inject.Inject

class PdfDetailsActivity: AppCompatActivity() {

    lateinit var binding: ActivityPdfDetailsBinding
    lateinit var viewModel: PdfDetailsViewModel
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)

        val documentId = intent.getLongExtra(ARG_DOCUMENT_ID, -1)

        viewModel = ViewModelProvider(viewModelStore, PdfDetailsViewModelFactory(documentId, appComponent)).get(
            PdfDetailsViewModel::class.java)
        binding = ActivityPdfDetailsBinding.inflate(layoutInflater)

        binding.progressBarPdfDetails.visibility = View.VISIBLE
        binding.pdfViewPdfDetails.visibility = View.GONE
        viewModel.document.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ document ->
                if(document.mimeType != Constants.MimeTypes.PDF.type) {
                    LOGGER.warn("Mime type is not PDF. Skipping")
                    binding.progressBarPdfDetails.visibility = View.GONE
                    binding.pdfViewPdfDetails.visibility = View.GONE
                    return@subscribe
                }
                binding.textViewPdfDetailsFileSize.text = document.sizeBytes.formatBytes(timeAndLocaleHandler.getLocale())
                val uri = Uri.parse(document.uri)


                val file = uri.toFile() //TODO This approach won't work with ExTrack since the Uri won't be a file

                //I see from the source that this listener only works with initWithUrl
                binding.pdfViewPdfDetails.statusListener = object : PdfRendererView.StatusCallBack {
                    override fun onPdfLoadSuccess(absolutePath: String) {
                        LOGGER.info("onPdfLoadSuccess")
                        binding.progressBarPdfDetails.visibility = View.GONE
                        binding.pdfViewPdfDetails.visibility = View.VISIBLE
                    }

                    override fun onPageChanged(currentPage: Int, totalPage: Int) {
                        LOGGER.info("onPageChanged: currentPage: {}, totalPage: {}", currentPage, totalPage)
                    }

                    override fun onError(error: Throwable) {
                        LOGGER.error("PDF Load error", error)
                    }

                    override fun onPdfLoadStart() {
                        LOGGER.info("onPdfLoadStart")
                    }

                    override fun onPdfLoadProgress(
                        progress: Int,
                        downloadedBytes: Long,
                        totalBytes: Long?
                    ) {
                        LOGGER.info("onPdfLoadProgress")
                    }
                }
                binding.pdfViewPdfDetails.initWithUri(uri)
                binding.progressBarPdfDetails.visibility = View.GONE
                binding.pdfViewPdfDetails.visibility = View.VISIBLE


                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer: PdfRenderer? = try {
                    PdfRenderer(fd)
                    //TODO Handle IO Exception
                } catch (e: SecurityException) {
                    LOGGER.error("Security exception for pdf", e)
                    null
                }
                if (pdfRenderer == null) {
                    return@subscribe
                }
                if (pdfRenderer.pageCount == 0) {
                    LOGGER.info("Zero pages")
                    return@subscribe
                }
                val text = pdfRenderer.pageCount.toString() + " pages" //TODO pluralization
                binding.textViewPdfDetailsPageCount.text = text
                //TODO "Open with" button


            }, {
                LOGGER.error("Unknown error: ", it)
            })


        setContentView(binding.root)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PdfDetailsActivity::class.java)
        private const val ARG_DOCUMENT_ID = "documentId"

        @JvmStatic
        fun addExtras(intent: Intent, documentId: Long) {
            intent.putExtra(ARG_DOCUMENT_ID, documentId)
        }
    }
}