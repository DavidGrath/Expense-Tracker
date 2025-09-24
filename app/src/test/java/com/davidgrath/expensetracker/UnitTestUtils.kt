package com.davidgrath.expensetracker

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.io.File

private val LOGGER = LoggerFactory.getLogger("com.davidgrath.expensetracker.UnitTestUtils")

fun getHashCount(sha256: String, folder: File): Single<Int> {
    if(!folder.exists() || !folder.isDirectory) {
        LOGGER.info("getHashCount: Invalid folder")
        return Single.just(0)
    }
    val hashCountSingle = Single.fromCallable {
        var hashCount = 0
        for(f in folder.listFiles()) {
            LOGGER.info("F: ${f.absolutePath}")
            val inputStream = f.inputStream()
            val h = getSha256(inputStream).blockingGet()
            if(h == sha256) {
                hashCount++
            }
            inputStream.close()
        }
        hashCount
    }

    return hashCountSingle.subscribeOn(Schedulers.io())
}

fun copyResourceToFile(classLoader: ClassLoader, resourceName: String, file: File) {
    val resourceInputStream = classLoader.getResourceAsStream(resourceName)
    val outputStream = file.outputStream()
    val copied = resourceInputStream.copyTo(outputStream)
    resourceInputStream.close()
    outputStream.close()
    LOGGER.info("copyResourceToFile: copied {} bytes", copied)
}