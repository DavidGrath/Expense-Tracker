package com.davidgrath.expensetracker.utils

import com.davidgrath.expensetracker.ImageHelperTestImpl
import com.davidgrath.expensetracker.TestData
import com.davidgrath.expensetracker.UtilsTest
import com.davidgrath.expensetracker.getSha256
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class ImageHelperImplTest {

    val imageHelper = ImageHelperImpl()
    @Test
    fun givenNoLocationWhenRemoveLocationThenNoChangesMadeToFile() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val classLoader = ImageHelperTestImpl::class.java.classLoader!!

        val resource = TestData.Resource.Images.DUMBBELLS_2
        val fileName = resource.fileName
        val resourceHash = resource.sha256
        val inStream = classLoader.getResourceAsStream(fileName)
        val noLocationFile = File(folder, fileName)
        val outStream = noLocationFile.outputStream()
        inStream.copyTo(outStream)
        inStream.close()
        outStream.close()

        imageHelper.removeLocationData(folder, noLocationFile).blockingSubscribe()
        val newFile = File(folder, "nogps-$fileName")
        val newFileStream = newFile.inputStream()
        val newFileHash = getSha256(newFileStream).blockingGet()
        newFileStream.close()
        Assert.assertEquals(resourceHash, newFileHash)
    }
}