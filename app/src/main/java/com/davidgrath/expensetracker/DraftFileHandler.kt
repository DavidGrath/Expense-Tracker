package com.davidgrath.expensetracker

import android.net.Uri
import com.davidgrath.expensetracker.entities.ui.AddEditDetailedTransactionDraft
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.io.File

interface DraftFileHandler {
    fun saveDraft(draft: AddEditDetailedTransactionDraft): Single<Unit>
    fun draftExists(): Boolean
    fun createDraft(): Single<Boolean>
    fun deleteDraftFiles(): Single<Boolean>
    fun getDraft(): Maybe<AddEditDetailedTransactionDraft>
    fun moveFileToMain(sourceFile: File, subfolder: String): Single<File>
    fun getFileHash(uri: Uri): Single<String>

    /**
     * @return The Uri of the created local file
     */
    fun copyUriToDraft(uri: Uri, mimeType: String, subfolder: String): Single<Uri>
}