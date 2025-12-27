package com.davidgrath.expensetracker.ui.main.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.davidgrath.expensetracker.databinding.RecyclerviewCategoryBinding
import com.davidgrath.expensetracker.entities.ui.CategoryWithStatsUi

class CategoriesRecyclerAdapter(private var _items: List<CategoryWithStatsUi>, var listener: CategoriesClickListener? = null): RecyclerView.Adapter<CategoriesRecyclerAdapter.CategoriesViewHolder>() {

    interface CategoriesClickListener {
        fun onEditClicked(categoryId: Long, categoryName: String, categoryIcon: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewCategoryBinding.inflate(inflater, parent, false)
        val viewHolder = CategoriesViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: CategoriesViewHolder, position: Int) {
        holder.binding.let { binding ->
            _items[position].let { category ->
                binding.textViewCategoryName.text = category.name
                binding.textViewCategoriesTransactionCount.text = "${category.transactionCount} transactions"
                binding.textViewCategoriesItemCount.text = "${category.itemCount} items"
                binding.imageViewCategoriesIcon.setImageResource(category.iconId)
                binding.imageViewCategoriesEdit.setOnClickListener {
                    listener?.onEditClicked(category.id, category.name, category.icon)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return _items.size
    }


    fun setItems(items: List<CategoryWithStatsUi>) {
        this._items = items
        notifyDataSetChanged()
    }

    class CategoriesViewHolder(val binding: RecyclerviewCategoryBinding): RecyclerView.ViewHolder(binding.root)
}