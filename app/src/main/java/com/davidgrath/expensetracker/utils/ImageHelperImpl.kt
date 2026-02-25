package com.davidgrath.expensetracker.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.attributes
import com.davidgrath.expensetracker.gpsTags
import io.reactivex.rxjava3.core.Single
import org.slf4j.LoggerFactory
import java.io.File

class ImageHelperImpl: ImageHelper {

    override fun compressImage(input: File, output: File, targetSize: Int): Single<Long> {
        //Fail on image load fail
        return Single.fromCallable {
            val options = BitmapFactory.Options().apply {
                inSampleSize = targetSize
            }
            var quality = 100
            val inStream = input.inputStream()
            val bitmap = BitmapFactory.decodeStream(inStream, null, options)!!
            inStream.close()
            var targetFileSize = input.length()
            while(quality >= 70) {

                val outputStream = output.outputStream()
                val compress = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                LOGGER.info("Compress: $compress")
                outputStream.close()

                val newInStream = input.inputStream()
                val exif = ExifInterface(newInStream)
                val newExif = ExifInterface(output)
                var attributeCount = 0
                for(attr in attributes) {
                    if(exif.hasAttribute(attr)) {
                        attributeCount++
                        newExif.setAttribute(attr, exif.getAttribute(attr))
                    }
                }
                newExif.saveAttributes()
                LOGGER.info("Copied over $attributeCount attributes")

                targetFileSize = output.length()
                val originalSize = input.length()
                val ratio = targetFileSize.toDouble() / originalSize
                if(ratio > 1) {
                    LOGGER.warn("Something is off with the ratio")
                }
                val percentage = 100.0 * (1 - ratio)
                LOGGER.info("Wrote new file of size $targetFileSize. Ratio $percentage %")

                if(targetFileSize <= Constants.IMAGE_SIZE_THRESHOLD) {
                    LOGGER.info("File is small enough using quality $quality")
                    break
                }

                quality -= 10
            }
            if(targetFileSize > Constants.IMAGE_SIZE_THRESHOLD) {
                LOGGER.error("Resized down to quality 70 and file is still too large")
                throw RuntimeException()
            }

            targetFileSize
        }
    }
    override fun removeLocationData(folder: File, file: File): Single<Unit> {


        return Single.fromCallable {
            val exif = ExifInterface(file)
            val newFile = File(folder, "nogps-${file.name}")
            file.copyTo(newFile)
            val newExif = ExifInterface(newFile)
            var hasAnyGpsTag = false
            for(attr in gpsTags) {
                if(exif.hasAttribute(attr)) {
                    hasAnyGpsTag = true
                    newExif.setAttribute(attr, null)
                }
            }
            if(hasAnyGpsTag) {
                newExif.saveAttributes()
                LOGGER.info("At least one GPS tag was found and removed")
            } else {
                LOGGER.info("No GPS tags found. File remains unchanged")
            }
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageHelperImpl::class.java)
    }
}