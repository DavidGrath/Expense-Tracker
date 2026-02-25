package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.utils.ImageHelper
import io.reactivex.rxjava3.core.Single
import java.io.File

/**
 * I can't trust exactly how Robolectric will implement `Bitmap.compress`, and I also don't know
 * enough about Exif to guarantee that simply nullifying the GPS tags will yield the same hash as
 * the original location-less file, so that's why I made this class.
 *
 * This class should only be used with `DUMBBELLS_1` and variants
 */
class ImageHelperTestImpl: ImageHelper {

    override fun compressImage(input: File, output: File, targetSize: Int): Single<Long> {
        return Single.fromCallable {
            val classLoader = ImageHelperTestImpl::class.java.classLoader!!
            val inputStream = classLoader.getResourceAsStream(TestData.Resource.Images.DUMBBELLS_1_BELOW_1600.resourceName)
            val outputStream = output.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            188_605
        }
    }

    override fun removeLocationData(folder: File, file: File): Single<Unit> {
        return Single.fromCallable {
            val newFile = File(folder, "nogps-${file.name}")
            val classLoader = ImageHelperTestImpl::class.java.classLoader!!
            val resource = if(file.name.contains("resized")) {
                TestData.Resource.Images.DUMBBELLS_1.resourceName
            } else {
                TestData.Resource.Images.DUMBBELLS_1_BELOW_1600.resourceName
            }
            val inputStream = classLoader.getResourceAsStream(resource)
            val outputStream = newFile.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        }
    }
}