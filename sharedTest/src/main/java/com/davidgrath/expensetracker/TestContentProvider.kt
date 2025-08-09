package com.davidgrath.expensetracker

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class TestContentProvider: ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return "image/jpeg"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val path = uri.path
        val tempDir = File(context!!.filesDir, "temp")
        val file = File(tempDir, path!!)
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(fd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }


    companion object {
        const val AUTHORITY = "expensetracker.test"
    }
}