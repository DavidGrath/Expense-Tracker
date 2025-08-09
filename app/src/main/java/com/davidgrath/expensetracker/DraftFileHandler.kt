package com.davidgrath.expensetracker

import androidx.lifecycle.LiveData
import com.davidgrath.expensetracker.entities.ui.AddDetailedTransactionDraft

interface DraftFileHandler {
    fun saveDraft(draft: AddDetailedTransactionDraft)
    fun draftExists(): Boolean
    fun createDraft(): Boolean
    fun deleteDraft(): Boolean
    fun getDraft(): LiveData<AddDetailedTransactionDraft>
    fun getDraftValue(): AddDetailedTransactionDraft
}