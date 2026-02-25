package com.davidgrath.expensetracker

import android.os.Environment
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class UtilsRobolectricTest {

    @Test
    fun basicLocationCheckTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val classLoader = UtilsTest::class.java.classLoader!!

        val fileName = "geotagged-pexels-ivan-samkov-4164765.jpg"
        val inStream = classLoader.getResourceAsStream(fileName)
        val locationFile = File(folder, fileName)
        val outStream = locationFile.outputStream()
        inStream.copyTo(outStream)
        inStream.close()
        outStream.close()

        assertTrue(getLocationData(locationFile).blockingGet() != null)
    }
}