package com.davidgrath.expensetracker.ui.addtransaction

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddDetailedTransactionItemBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class AddTransactionItemRecyclerAdapter(private var categories: List<CategoryUi>, var listener: AddTransactionItemRecyclerListener? = null): RecyclerView.Adapter<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>() {

    var _items = listOf<AddTransactionItem>()


    private var currentItem = -1
    private val textWatcherAmountMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherDescriptionMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherBrandMap = mutableMapOf<Int, TextWatcher>()

    interface AddTransactionItemRecyclerListener {
        fun onItemChanged(position: Int, item: AddTransactionItem)
        fun onItemDeleted(position: Int)
        fun onRequestAddImage(position: Int, itemId: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewAddDetailedTransactionItemBinding.inflate(inflater, parent, false)
        val viewHolder = AddTransactionItemViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: AddTransactionItemViewHolder, position: Int) {
        holder.binding.let { binding ->
            val spinnerAdapter = SpinnerCategoryAdapter(binding.root.context, R.layout.spinner_item_category, categories.toTypedArray())
            val absPosition = position
//            val absPosition = holder.absoluteAdapterPosition
            _items[absPosition].let { item ->
                var _item = item.copy()
                if(absPosition != 0) {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.VISIBLE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener {
                        listener?.onItemDeleted(absPosition)
                    }
                } else {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.GONE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener(null)
                }

                val oldAmountWatcher = textWatcherAmountMap[binding.editTextAddDetailedTransactionItemAmount.hashCode()]
                if(oldAmountWatcher != null) {
                    binding.editTextAddDetailedTransactionItemAmount.removeTextChangedListener(oldAmountWatcher)
                }
                binding.editTextAddDetailedTransactionItemAmount.setText(if(_item.amount == null) "" else String.format(Locale.getDefault(), "%.2f", _item.amount))
                val newAmountWatcher = binding.editTextAddDetailedTransactionItemAmount.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemAmount.hasFocus()) {
                    currentItem = absPosition
                        val amount = try {
                            BigDecimal(text.toString()).setScale(2, RoundingMode.HALF_UP)
                        } catch (e: NumberFormatException) {
                            null
                        }
                        if (_item.amount != amount) {
                            _item = _item.copy(amount = amount)
                            listener?.onItemChanged(absPosition, _item)
                        }
                    }
                }
                textWatcherAmountMap[binding.editTextAddDetailedTransactionItemAmount.hashCode()] = newAmountWatcher

                val oldDescriptionWatcher = textWatcherDescriptionMap[binding.editTextAddDetailedTransactionItemDescription.hashCode()]
                if(oldDescriptionWatcher != null) {
                    binding.editTextAddDetailedTransactionItemDescription.removeTextChangedListener(oldDescriptionWatcher)
                }
                binding.editTextAddDetailedTransactionItemDescription.setText(_item.description?:"")
                val newDescriptionWatcher = binding.editTextAddDetailedTransactionItemDescription.addTextChangedListener { text: Editable? ->
                    currentItem = absPosition
                    if(binding.editTextAddDetailedTransactionItemDescription.hasFocus()) {
                        if (_item.description != text.toString()) {
                            _item = _item.copy(description = text.toString())
                            listener?.onItemChanged(absPosition, _item)
                        }
                    }
                }
                textWatcherDescriptionMap[binding.editTextAddDetailedTransactionItemDescription.hashCode()] = newDescriptionWatcher

                var categoryPosition = categories.indexOf(_item.category)
                if(categoryPosition == -1) categoryPosition = 0
                binding.spinnerAddDetailedTransactionItemCategory.adapter = spinnerAdapter
                binding.spinnerAddDetailedTransactionItemCategory.setSelection(categoryPosition)
                binding.spinnerAddDetailedTransactionItemCategory.onItemSelectedListener = object : OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, _position: Int, id: Long) {
                        currentItem = absPosition
                        if(_item.category.id != _position.toLong()) {
                            _item = _item.copy(category = categories[absPosition])
                            listener?.onItemChanged(absPosition, _item)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }
                binding.linearLayoutAddDetailedTransactionDetails.visibility = if(_item.showDetails) View.VISIBLE else View.GONE
                binding.textViewAddDetailedTransactionShowDetails.setOnClickListener {
                    currentItem = absPosition
                    binding.linearLayoutAddDetailedTransactionDetails.visibility = if(!_item.showDetails) View.VISIBLE else View.GONE
                    _item = _item.copy(showDetails = !_item.showDetails)
                    listener?.onItemChanged(absPosition, _item)
                }


                val oldBrandWatcher = textWatcherBrandMap[binding.editTextAddDetailedTransactionItemBrand.hashCode()]
                if(oldBrandWatcher != null) {
                    binding.editTextAddDetailedTransactionItemBrand.removeTextChangedListener(oldBrandWatcher)
                }
                binding.editTextAddDetailedTransactionItemBrand.setText(_item.brand?:"")
                binding.editTextAddDetailedTransactionItemBrand.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemBrand.hasFocus()) {
                        currentItem = absPosition
                        if (_item.brand != text.toString()) {
                            _item = _item.copy(brand = text.toString())
                            listener?.onItemChanged(absPosition, _item)
                        }
                    }
                }
                textWatcherBrandMap[binding.editTextAddDetailedTransactionItemBrand.hashCode()] = newDescriptionWatcher
                val adapter = AddTransactionItemImagesRecyclerAdapter(_item.images)
                val layoutManager = LinearLayoutManager(binding.root.context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                binding.recyclerviewAddDetailedTransactionItemImages.adapter = adapter
                binding.recyclerviewAddDetailedTransactionItemImages.layoutManager = layoutManager
                binding.imageViewAddDetailedTransactionItemAddImage.setOnClickListener {
                    listener?.onRequestAddImage(absPosition, _item.id)
                }

            }
        }
    }


    override fun getItemCount(): Int {
        return _items.size
    }

    fun setItems(items: List<AddTransactionItem>) {
        this._items = items
    }

    fun setCategories(categories: List<CategoryUi>) {
        this.categories = categories
        notifyDataSetChanged()
    }


    class AddTransactionItemViewHolder(val binding: RecyclerviewAddDetailedTransactionItemBinding): ViewHolder(binding.root)
}

