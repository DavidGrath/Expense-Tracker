package com.davidgrath.expensetracker

import java.io.File

fun getHashCount(sha256: String, folder: File): Int {
    if(!folder.exists() || !folder.isDirectory) {
        return 0
    }
    var hashCount = 0
    for(f in folder.listFiles()) {
        println("F: ${f.absolutePath}")
        val inputStream = f.inputStream()
        if(getSha256(inputStream) == sha256) {
            hashCount++
        }
    }
    return hashCount
}

fun copyResourceToFile(classLoader: ClassLoader, resourceName: String, file: File) {
    val resourceInputStream = classLoader.getResourceAsStream(resourceName)
    val outputStream = file.outputStream()
    resourceInputStream.copyTo(outputStream)
    resourceInputStream.close()
    outputStream.close()
}