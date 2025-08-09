package com.davidgrath.expensetracker

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivityTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TestContentProviderTest {

    private val breadFileName = "pexels-pixabay-209206.jpg"
    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val temp = File(app.filesDir, "temp")
        temp.mkdir()
        val bread = File(temp, "bread.jpg")
        val inputStream =
            TestContentProviderTest::class.java.classLoader.getResourceAsStream(breadFileName)
        val outputStream = bread.outputStream()
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
    }

    @After
    fun tearDown() {
        val app = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val temp = File(app.filesDir, "temp")
        temp.deleteRecursively()
    }

    @Test
    fun contentProviderTest() {
        val fileHash = "ad94e42e0323ccba436e70ebc3cbbfca6a54469c2058a782b5ee14c90ae637a4"

        val context = ApplicationProvider.getApplicationContext<ExpenseTracker>()
        val inputStream =
            context.contentResolver.openInputStream(Uri.parse("content://expensetracker.test/bread.jpg"))
        val computedHash = getSha256(inputStream!!)
//            inputStream.close() // Crashes for some reason
        Assert.assertEquals(fileHash, computedHash)
    }
}