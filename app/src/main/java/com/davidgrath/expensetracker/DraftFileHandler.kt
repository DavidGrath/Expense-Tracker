package com.davidgrath.expensetracker

import androidx.lifecycle.LiveData
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft
import io.reactivex.rxjava3.core.Single
import java.io.File

interface DraftFileHandler {
    fun saveDraft(draft: AddDetailedTransactionDraft)
    fun draftExists(): Boolean
    fun createDraft(): Boolean
    fun deleteDraft(): Boolean
    fun getDraft(): LiveData<AddDetailedTransactionDraft>
    fun getDraftValue(): AddDetailedTransactionDraft
    fun moveFileToMain(file: File): Single<File>
}