package com.davidgrath.expensetracker.utils

import io.reactivex.rxjava3.core.Single
import java.io.File

interface ImageHelper {
    /**
     * @return The reduced file's size
     */
    fun compressImage(input: File, output: File, targetSize: Int): Single<Long>
    fun removeLocationData(folder: File, file: File): Single<Unit>
}