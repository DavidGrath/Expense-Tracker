package com.davidgrath.expensetracker.ui.main.categories

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.categoryWithStatsToCategoryWithStatsUi
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.views.EvidenceWithTransactionDateAndOrdinal
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.entities.ui.CategoryWithStatsUi
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import io.reactivex.rxjava3.core.BackpressureStrategy
import javax.inject.Inject

class CategoriesViewModel
@Inject
    constructor(
        val application: Application,
        val categoryRepository: CategoryRepository
    )
    : ViewModel() {

        val categories: LiveData<List<CategoryWithStatsUi>>


        init {
            val profileObservable = (application as ExpenseTracker).profileObservable

            categories = profileObservable.switchMap {
                categoryRepository.getCategoriesWithStats(it.id!!).map { list ->
                    list.map {
                        categoryWithStatsToCategoryWithStatsUi(application, it)
                    }
                }
            }.toFlowable(BackpressureStrategy.BUFFER).toLiveData()

        }
}