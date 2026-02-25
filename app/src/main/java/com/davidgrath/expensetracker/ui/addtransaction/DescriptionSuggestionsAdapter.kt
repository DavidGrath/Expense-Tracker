package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.davidgrath.expensetracker.repositories.TransactionRepository

class DescriptionSuggestionsAdapter(context: Context, val repository: TransactionRepository): ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,) {
    private var array = listOf<String>()

    override fun getItem(position: Int): String? {
        return array[position]
    }

    override fun getCount(): Int {
        return array.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                if(constraint == null) {
                    return FilterResults()
                }
                val suggestions = repository.getGenericSuggestions(TransactionRepository.SuggestionsField.Description, constraint.toString()).blockingGet()
                val results = FilterResults()
                results.values = suggestions
                results.count = suggestions.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if(results == null) {
                    this@DescriptionSuggestionsAdapter.array = emptyList()
                } else {
                    if(results.values != null) {
                        this@DescriptionSuggestionsAdapter.array = results.values as List<String>
                    } else {
                        this@DescriptionSuggestionsAdapter.array = emptyList()
                    }
                }
            }
        }
    }
}