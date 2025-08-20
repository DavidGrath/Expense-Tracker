package com.davidgrath.expensetracker

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

fun getHashCount(sha256: String, folder: File): Single<Int> {
    if(!folder.exists() || !folder.isDirectory) {
        return Single.just(0)
    }
    val hashCountSingle = Single.fromCallable {
        var hashCount = 0
        for(f in folder.listFiles()) {
            println("F: ${f.absolutePath}")
            val inputStream = f.inputStream()
            if(getSha256(inputStream).blockingGet() == sha256) {
                hashCount++
            }
        }
        hashCount
    }

    return hashCountSingle.subscribeOn(Schedulers.io())
}

fun copyResourceToFile(classLoader: ClassLoader, resourceName: String, file: File) {
    val resourceInputStream = classLoader.getResourceAsStream(resourceName)
    val outputStream = file.outputStream()
    resourceInputStream.copyTo(outputStream)
    resourceInputStream.close()
    outputStream.close()
}