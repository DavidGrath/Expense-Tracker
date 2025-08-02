package com.davidgrath.expensetracker.ui.addtransaction

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddDetailedTransactionItemBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionPurchaseItem
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class AddTransactionPurchaseItemRecyclerAdapter(var listener: AddTransactionPurchaseItemRecyclerListener? = null): RecyclerView.Adapter<AddTransactionPurchaseItemRecyclerAdapter.AddTransactionPurchaseItemViewHolder>() {

        var items = listOf<AddTransactionPurchaseItem>()
    fun submitList(submitted: List<AddTransactionPurchaseItem>) {
        this.items = submitted
    }

    override fun getItemCount(): Int {
        return items.size
    }


    private var currentItem = -1
    private val textWatcherAmountMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherDescriptionMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherBrandMap = mutableMapOf<Int, TextWatcher>()

    interface AddTransactionPurchaseItemRecyclerListener {
        fun onItemChanged(position: Int, item: AddTransactionPurchaseItem)
        fun onItemDeleted(position: Int)
        fun onRequestAddImage(position: Int, itemId: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionPurchaseItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewAddDetailedTransactionItemBinding.inflate(inflater, parent, false)
        val viewHolder = AddTransactionPurchaseItemViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: AddTransactionPurchaseItemViewHolder, position: Int) {
        holder.binding.let { binding ->
            val spinnerAdapter = SpinnerCategoryAdapter(binding.root.context, R.layout.spinner_item_category, CategoryUi.TEMP_DEFAULT_CATEGORIES.toTypedArray())
            val absPosition = position
//            val absPosition = holder.absoluteAdapterPosition
            items[absPosition].let { item ->
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
                binding.editTextAddDetailedTransactionItemAmount.setText(String.format(Locale.getDefault(), "%.2f", _item.amount?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)))
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

                var categoryPosition = CategoryUi.TEMP_DEFAULT_CATEGORIES.indexOf(_item.category)
                if(categoryPosition == -1) categoryPosition = 0
                binding.spinnerAddDetailedTransactionItemCategory.adapter = spinnerAdapter
                binding.spinnerAddDetailedTransactionItemCategory.setSelection(categoryPosition)
                binding.spinnerAddDetailedTransactionItemCategory.onItemSelectedListener = object : OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, _position: Int, id: Long) {
                        currentItem = absPosition
                        if(_item.category.id != _position.toLong()) {
                            _item = _item.copy(category = CategoryUi.TEMP_DEFAULT_CATEGORIES[absPosition])
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

                if(_item.images.size > 0) {
                    val imageUri = _item.images[0]
                    Glide.with(holder.binding.root)
                        .load(Uri.parse(imageUri))
                        .centerCrop()
                        .into(binding.tempImageViewAddDetailedTransactionItemFirstImage)
                }
                binding.imageViewAddDetailedTransactionItemAddImage.setOnClickListener {
                    listener?.onRequestAddImage(absPosition, _item.id)
                }

            }
        }
    }


    class AddTransactionPurchaseItemViewHolder(val binding: RecyclerviewAddDetailedTransactionItemBinding): ViewHolder(binding.root)
}

