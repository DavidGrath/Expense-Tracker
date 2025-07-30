package com.davidgrath.expensetracker.ui

import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddDetailedTransactionItemBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import com.davidgrath.expensetracker.entities.ui.Category
import java.math.BigDecimal
import java.util.Locale

class AddTransactionPurchaseItemRecyclerAdapter(items: List<AddTransactionPurchaseItem>, var listener: AddTransactionPurchaseItemRecyclerListener? = null): RecyclerView.Adapter<AddTransactionPurchaseItemRecyclerAdapter.AddTransactionPurchaseItemViewHolder>() {

    var _items = items
    var currentItem = -1

    interface AddTransactionPurchaseItemRecyclerListener {
        fun onItemChanged(position: Int, item: AddTransactionPurchaseItem)
        fun onItemDeleted(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionPurchaseItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewAddDetailedTransactionItemBinding.inflate(inflater, parent, false)
        val viewHolder = AddTransactionPurchaseItemViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: AddTransactionPurchaseItemViewHolder, position: Int) {
        holder.binding.let { binding ->
            val spinnerAdapter = SpinnerCategoryAdapter(binding.root.context, R.layout.spinner_item_category, Category.TEMP_DEFAULT_CATEGORIES.toTypedArray())
            _items[position].let { item ->
                var _item = item.copy()
                if(position != 0) {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.VISIBLE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener {
                        listener?.onItemDeleted(position)
                    }
                } else {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.VISIBLE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener(null)
                }
                binding.editTextAddDetailedTransactionItemAmount.setText(String.format(Locale.getDefault(), "%f", item.amount?: BigDecimal.ZERO))
                binding.editTextAddDetailedTransactionItemAmount.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemAmount.hasFocus()) {
                    currentItem = position
                        val amount = try {
                            BigDecimal(text.toString())
                        } catch (e: NumberFormatException) {
                            null
                        }
                        if (item.amount != amount) {
                            _item = _item.copy(amount = amount)
                            Log.d("Amount", _item.amount.toString())
                            Log.d("Amount", "onItemChanged")
                            listener?.onItemChanged(position, _item)
                        }
                    }
                }
                binding.editTextAddDetailedTransactionItemDescription.setText(item.description?:"")
                binding.editTextAddDetailedTransactionItemDescription.addTextChangedListener { text: Editable? ->
                    currentItem = position
                    if(binding.editTextAddDetailedTransactionItemDescription.hasFocus()) {
                        if (item.description != text.toString()) {
                            _item = _item.copy(description = text.toString())
                            Log.d("Desc", _item.description.toString())
                            Log.d("Desc", "onItemChanged")
                            listener?.onItemChanged(position, _item)
                        }
                    }
                }

                var categoryPosition = Category.TEMP_DEFAULT_CATEGORIES.indexOf(item.category)
                if(categoryPosition == -1) categoryPosition = 0
                binding.spinnerAddDetailedTransactionItemCategory.adapter = spinnerAdapter
                binding.spinnerAddDetailedTransactionItemCategory.setSelection(categoryPosition)
                binding.spinnerAddDetailedTransactionItemCategory.onItemSelectedListener = object : OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, _position: Int, id: Long) {
                        currentItem = position
                        if(item.category.id != _position.toLong()) {
                            Log.d("Category", "onItemChanged")
                            _item = _item.copy(category = Category.TEMP_DEFAULT_CATEGORIES[position])
                            listener?.onItemChanged(position, _item)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }

                binding.linearLayoutAddDetailedTransactionDetails.visibility = if(item.showDetails) View.VISIBLE else View.GONE
                binding.textViewAddDetailedTransactionShowDetails.setOnClickListener {
                    currentItem = position
                    binding.linearLayoutAddDetailedTransactionDetails.visibility = if(!item.showDetails) View.VISIBLE else View.GONE
                    Log.d("ShowDetails", "onItemChanged")
                    _item = _item.copy(showDetails = !_item.showDetails)
                    listener?.onItemChanged(position, _item)
                }
                binding.editTextAddDetailedTransactionItemBrand.setText(item.brand?:"")
                binding.editTextAddDetailedTransactionItemBrand.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemBrand.hasFocus()) {
                        currentItem = position
                        if (item.brand != text.toString()) {
                            Log.d("Brand", "onItemChanged")
                            _item = _item.copy(brand = text.toString())
                            listener?.onItemChanged(position, _item)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return _items.size
    }

    fun setItems(items: List<AddTransactionPurchaseItem>) {
        this._items = items
        println("ITEMS: $_items")
        _items.forEachIndexed { index, addTransactionPurchaseItem ->
            if(index != currentItem) {
                notifyItemChanged(index)
            }
        }
    }

    class AddTransactionPurchaseItemViewHolder(val binding: RecyclerviewAddDetailedTransactionItemBinding): ViewHolder(binding.root)

}

