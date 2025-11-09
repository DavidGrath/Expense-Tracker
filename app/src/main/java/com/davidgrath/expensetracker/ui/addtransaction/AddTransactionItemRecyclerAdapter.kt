package com.davidgrath.expensetracker.ui.addtransaction

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddDetailedTransactionItemBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class AddTransactionItemRecyclerAdapter(private var categories: List<CategoryUi>, val timeAndLocaleHandler: TimeAndLocaleHandler, var listener: AddTransactionItemRecyclerListener? = null): RecyclerView.Adapter<AddTransactionItemRecyclerAdapter.AddTransactionItemViewHolder>() {

    var _items = listOf<AddTransactionItem>()


    private var currentItem = -1
    private val textWatcherAmountMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherDescriptionMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherBrandMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherVariationMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherQuantityMap = mutableMapOf<Int, TextWatcher>()
    private val textWatcherReferenceMap = mutableMapOf<Int, TextWatcher>()

    interface AddTransactionItemRecyclerListener {
        fun onItemChanged(position: Int, item: AddTransactionItem)
        fun onItemChangedInvalidate(position: Int, item: AddTransactionItem)
        fun onDeleteItem(position: Int)
        fun onRequestAddImage(position: Int, itemId: Int)
        fun onDeleteItemImage(position: Int, imagePosition: Int)
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
            _items[absPosition].let { cachedItem ->
                if(absPosition != 0) {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.VISIBLE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener {
                        listener?.onDeleteItem(absPosition)
                    }
                } else {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.GONE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener(null)
                }

                val oldAmountWatcher = textWatcherAmountMap[binding.editTextAddDetailedTransactionItemAmount.hashCode()]
                if(oldAmountWatcher != null) {
                    binding.editTextAddDetailedTransactionItemAmount.removeTextChangedListener(oldAmountWatcher)
                }
                binding.editTextAddDetailedTransactionItemAmount.setText(if(cachedItem.amount == null) "" else String.format(timeAndLocaleHandler.getLocale(), "%.2f", cachedItem.amount))
                val newAmountWatcher = binding.editTextAddDetailedTransactionItemAmount.addTextChangedListener { text: Editable? ->
//                    if(binding.editTextAddDetailedTransactionItemAmount.hasFocus()) { //Commented out because of Robolectric
                    var latestItem = _items[absPosition]
                    currentItem = absPosition
                        val amount = try {
                            BigDecimal(text.toString()).setScale(2, RoundingMode.HALF_UP)
                        } catch (e: NumberFormatException) {
                            null
                        }
                        if (latestItem.amount != amount) {
                            listener?.onItemChanged(absPosition, latestItem.copy(amount = amount))
                        }
//                    }
                }
                textWatcherAmountMap[binding.editTextAddDetailedTransactionItemAmount.hashCode()] = newAmountWatcher

                val oldDescriptionWatcher = textWatcherDescriptionMap[binding.editTextAddDetailedTransactionItemDescription.hashCode()]
                if(oldDescriptionWatcher != null) {
                    binding.editTextAddDetailedTransactionItemDescription.removeTextChangedListener(oldDescriptionWatcher)
                }
                binding.editTextAddDetailedTransactionItemDescription.setText(cachedItem.description?:"")
                val newDescriptionWatcher = binding.editTextAddDetailedTransactionItemDescription.addTextChangedListener { text: Editable? ->
                    currentItem = absPosition
                    var latestItem = _items[absPosition]
                    if(binding.editTextAddDetailedTransactionItemDescription.hasFocus()) {
                        if (latestItem.description != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(description = text.toString()))
                        }
                    }
                }
                textWatcherDescriptionMap[binding.editTextAddDetailedTransactionItemDescription.hashCode()] = newDescriptionWatcher

                var categoryPosition = categories.indexOf(cachedItem.category)
                if(categoryPosition == -1) categoryPosition = 0
                binding.spinnerAddDetailedTransactionItemCategory.adapter = spinnerAdapter
                binding.spinnerAddDetailedTransactionItemCategory.setSelection(categoryPosition)
                binding.spinnerAddDetailedTransactionItemCategory.onItemSelectedListener = object : OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, _position: Int, id: Long) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        val category = categories[_position]
                        if(latestItem.category.id != category.id) {
                            latestItem = latestItem.copy(category = category)
                            listener?.onItemChangedInvalidate(absPosition, latestItem)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }
                binding.linearLayoutAddDetailedTransactionDetails.visibility = if(cachedItem.showDetails) View.VISIBLE else View.GONE
                val showDrawable = AppCompatResources.getDrawable(binding.root.context, R.drawable.baseline_keyboard_arrow_right_24)
                val hideDrawable = AppCompatResources.getDrawable(binding.root.context, R.drawable.baseline_keyboard_arrow_down_24)
                binding.textViewAddDetailedTransactionShowDetails.setCompoundDrawables(
                    if(cachedItem.showDetails) {
                        hideDrawable
                    } else {
                        showDrawable
                    }, null, null, null
                )
                binding.textViewAddDetailedTransactionShowDetails.setOnClickListener {
                    currentItem = absPosition
                    var latestItem = _items[position]
                    binding.linearLayoutAddDetailedTransactionDetails.visibility = if(!latestItem.showDetails) View.VISIBLE else View.GONE
                    latestItem = latestItem.copy(showDetails = !latestItem.showDetails)
                    listener?.onItemChangedInvalidate(absPosition, latestItem)
                }

                binding.checkBoxAddDetailedTransactionIsReduction.setOnCheckedChangeListener(null)
                binding.checkBoxAddDetailedTransactionIsReduction.isChecked = cachedItem.isReduction
                binding.checkBoxAddDetailedTransactionIsReduction.text = "Is Discount/Deduction" //TODO Pass debitOrCredit into this adapter
                binding.checkBoxAddDetailedTransactionIsReduction.setOnCheckedChangeListener { buttonView, isChecked ->
                    currentItem = absPosition
                    var latestItem = _items[absPosition]
                    latestItem = latestItem.copy(isReduction = isChecked)
                    listener?.onItemChangedInvalidate(absPosition, latestItem)
                }

                val oldBrandWatcher = textWatcherBrandMap[binding.editTextAddDetailedTransactionItemBrand.hashCode()]
                if(oldBrandWatcher != null) {
                    binding.editTextAddDetailedTransactionItemBrand.removeTextChangedListener(oldBrandWatcher)
                }
                binding.editTextAddDetailedTransactionItemBrand.setText(cachedItem.brand?:"")
                val newBrandWatcher = binding.editTextAddDetailedTransactionItemBrand.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemBrand.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.brand != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(brand = text.toString()))
                        }
                    }
                }
                textWatcherBrandMap[binding.editTextAddDetailedTransactionItemBrand.hashCode()] = newBrandWatcher

                val oldVariationWatcher = textWatcherVariationMap[binding.editTextAddDetailedTransactionItemVariation.hashCode()]
                if(oldVariationWatcher != null) {
                    binding.editTextAddDetailedTransactionItemVariation.removeTextChangedListener(oldVariationWatcher)
                }
                binding.editTextAddDetailedTransactionItemVariation.setText(cachedItem.variation?:"")
                val newVariationWatcher = binding.editTextAddDetailedTransactionItemVariation.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemVariation.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.variation != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(variation = text.toString()))
                        }
                    }
                }
                textWatcherVariationMap[binding.editTextAddDetailedTransactionItemVariation.hashCode()] = newVariationWatcher

                val oldQuantityWatcher = textWatcherQuantityMap[binding.editTextAddDetailedTransactionItemQuantity.hashCode()]
                if(oldQuantityWatcher != null) {
                    binding.editTextAddDetailedTransactionItemQuantity.removeTextChangedListener(oldQuantityWatcher)
                }
                binding.editTextAddDetailedTransactionItemQuantity.setText(cachedItem.quantity.toString())
                val newQuantityWatcher = binding.editTextAddDetailedTransactionItemQuantity.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemQuantity.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.quantity.toString() != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(quantity = text.toString().toInt()))
                        }
                    }
                }
                textWatcherQuantityMap[binding.editTextAddDetailedTransactionItemQuantity.hashCode()] = newQuantityWatcher

                val oldReferenceListener = textWatcherReferenceMap[binding.editTextAddDetailedTransactionItemReference.hashCode()]
                if(oldReferenceListener != null) {
                    binding.editTextAddDetailedTransactionItemReference.removeTextChangedListener(oldReferenceListener)
                }
                binding.editTextAddDetailedTransactionItemReference.setText(cachedItem.referenceNumber)
                val newReferenceWatcher = binding.editTextAddDetailedTransactionItemReference.addTextChangedListener { text: Editable? ->
                    if(binding.editTextAddDetailedTransactionItemReference.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.referenceNumber != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(referenceNumber = text?.toString()))
                        }
                    }
                }
                textWatcherReferenceMap[binding.editTextAddDetailedTransactionItemReference.hashCode()] = newReferenceWatcher
                //dd

                val adapter = AddTransactionItemImagesRecyclerAdapter(cachedItem.images, object: AddTransactionItemImagesRecyclerAdapter.ItemImageClickListener {
                    override fun onDeleteImage(position: Int) {
                        listener?.onDeleteItemImage(absPosition, position)
                    }
                })
                val layoutManager = LinearLayoutManager(binding.root.context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                binding.recyclerviewAddDetailedTransactionItemImages.adapter = adapter
                binding.recyclerviewAddDetailedTransactionItemImages.layoutManager = layoutManager
                if(cachedItem.images.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM) {
                    binding.imageViewAddDetailedTransactionItemAddImage.isEnabled = false
                    binding.imageViewAddDetailedTransactionItemAddImage.setOnClickListener(null)
                } else {
                    binding.imageViewAddDetailedTransactionItemAddImage.isEnabled = true
                    binding.imageViewAddDetailedTransactionItemAddImage.setOnClickListener {
                        val latestItem = _items[absPosition]
                        listener?.onRequestAddImage(absPosition, latestItem.id)
                    }
                }

            }
        }
    }


    override fun getItemCount(): Int {
        return _items.size
    }

    fun setItems(items: List<AddTransactionItem>) {
        LOGGER.info("setItems: List size: {}", items.size)
        this._items = items
    }

    fun setCategories(categories: List<CategoryUi>) {
        this.categories = categories
        notifyDataSetChanged()
    }


    class AddTransactionItemViewHolder(val binding: RecyclerviewAddDetailedTransactionItemBinding): ViewHolder(binding.root)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddTransactionItemRecyclerAdapter::class.java)
    }
}

