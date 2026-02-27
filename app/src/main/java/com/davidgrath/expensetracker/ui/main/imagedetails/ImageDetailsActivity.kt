package com.davidgrath.expensetracker.ui.main.imagedetails

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityImageDetailsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.getLocationData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import javax.inject.Inject

class ImageDetailsActivity: AppCompatActivity() {

    lateinit var binding: ActivityImageDetailsBinding
    lateinit var viewModel: ImageDetailsViewModel
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)

        val entityId = intent.getLongExtra(ARG_IMAGE_ID, -1)
        val documentOrImage = intent.getBooleanExtra(ARG_DOCUMENT_OR_IMAGE, false)
        val imageId = if(documentOrImage) {
            null
        } else {
            entityId
        }
        val documentId = if(documentOrImage) {
            entityId
        } else {
            null
        }
        viewModel = ViewModelProvider(viewModelStore, ImageDetailsViewModelFactory(documentOrImage, imageId, documentId, appComponent)).get(ImageDetailsViewModel::class.java)
        binding = ActivityImageDetailsBinding.inflate(layoutInflater)

        binding.progressBarImageDetails.visibility = View.VISIBLE
        binding.photoViewImageDetails.visibility = View.GONE
        viewModel.uriSizeType.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (uriString, size, type) ->
                binding.textViewImageDetailsFileSize.text = size.formatBytes(timeAndLocaleHandler.getLocale())
                val uri = Uri.parse(uriString)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                val file = uri.toFile()
                val bitmapStream = file.inputStream() //TODO This approach won't work with ExTrack since the Uri won't be a file
                val bufBitmapStream = BufferedInputStream(bitmapStream)
                BitmapFactory.decodeStream(bufBitmapStream, null, options)
                bufBitmapStream.close()
                bitmapStream.close()
                val text = options.outWidth.toString() + "x" + options.outHeight
                binding.textViewImageDetailsDimensions.text = text

                val locationData = getLocationData(file).blockingGet()
                if(locationData != null) {
                    binding.linearLayoutImageDetailsLocation.visibility = View.VISIBLE
                    binding.textViewImageDetailsLocation.text = locationData.first.toString() + "," + locationData.second //TODO Google Maps intent button
                } else {
                    binding.linearLayoutImageDetailsLocation.visibility = View.GONE

                }

                Glide.with(this)
                    .load(uri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            LOGGER.info("Image load failed")
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBarImageDetails.visibility = View.GONE
                            binding.photoViewImageDetails.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .override(Target.SIZE_ORIGINAL)
                    .into(binding.photoViewImageDetails)
            }, {
                LOGGER.error("Unknown error: ", it)
            })


        setContentView(binding.root)
    }


    companion object {

        private val LOGGER = LoggerFactory.getLogger(ImageDetailsActivity::class.java)
        private const val ARG_IMAGE_ID = "imageId"
        private const val ARG_DOCUMENT_OR_IMAGE = "documentOrImage"

        @JvmStatic
        fun addExtras(intent: Intent, imageId: Long, documentOrImage: Boolean) {
            intent.putExtra(ARG_IMAGE_ID, imageId)
            intent.putExtra(ARG_DOCUMENT_OR_IMAGE, documentOrImage)
        }
    }
}