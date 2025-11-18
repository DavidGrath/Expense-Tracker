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
import com.davidgrath.expensetracker.Constants.Companion.ALPHA_DISABLED
import com.davidgrath.expensetracker.MaxCodePointWatcher
import com.davidgrath.expensetracker.NumberFormatTextWatcher
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddDetailedTransactionItemBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.formatDecimal
import com.davidgrath.expensetracker.ui.SpinnerCategoryAdapter
import com.davidgrath.expensetracker.ui.dialogs.NumberDialogFragment
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
            _items[position].let { cachedItem ->
                if(position != 0) {
                    binding.imageViewAddDetailedTransactionItemDelete.visibility = View.VISIBLE
                    binding.imageViewAddDetailedTransactionItemDelete.setOnClickListener {
                        val absPosition = holder.absoluteAdapterPosition
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
                binding.editTextAddDetailedTransactionItemAmount.setText(if(cachedItem.amount == null) {
                    ""
                } else {
                    formatDecimal(cachedItem.amount, timeAndLocaleHandler.getLocale())
                })
                /*val newAmountWatcher = binding.editTextAddDetailedTransactionItemAmount.addTextChangedListener { text: Editable? ->
//                    if(binding.editTextAddDetailedTransactionItemAmount.hasFocus()) { //Commented out because of Robolectric
                    val absPosition = holder.absoluteAdapterPosition
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
                }*/
                val newAmountWatcher = NumberFormatTextWatcher(binding.editTextAddDetailedTransactionItemAmount, BigDecimal(1_000_000), timeAndLocaleHandler.getLocale()) { amount ->
                    val absPosition = holder.absoluteAdapterPosition
                    var latestItem = _items[absPosition]
                    currentItem = absPosition
                    if (latestItem.amount != amount) {
                        listener?.onItemChanged(absPosition, latestItem.copy(amount = amount))
                    }
                }
                binding.editTextAddDetailedTransactionItemAmount.addTextChangedListener(newAmountWatcher)
                textWatcherAmountMap[binding.editTextAddDetailedTransactionItemAmount.hashCode()] = newAmountWatcher

                val oldDescriptionWatcher = textWatcherDescriptionMap[binding.editTextAddDetailedTransactionItemDescription.hashCode()]
                if(oldDescriptionWatcher != null) {
                    binding.editTextAddDetailedTransactionItemDescription.removeTextChangedListener(oldDescriptionWatcher)
                }
                val cachedDescription = cachedItem.description?:""
                binding.editTextAddDetailedTransactionItemDescription.setText(cachedDescription)
                binding.textViewAddDetailedTransactionDescriptionIndicator.text = cachedDescription.codePointCount(0, cachedDescription.length).toString() + "/" + MAX_TEXT_LENGTH
                /*val newDescriptionWatcher = binding.editTextAddDetailedTransactionItemDescription.addTextChangedListener { text: Editable? ->
                    val absPosition = holder.absoluteAdapterPosition
                    currentItem = absPosition
                    var latestItem = _items[absPosition]
                    if(binding.editTextAddDetailedTransactionItemDescription.hasFocus()) {
                        if (latestItem.description != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(description = text.toString()))
                        }
                    }
                }*/
                val newDescriptionWatcher = MaxCodePointWatcher(binding.editTextAddDetailedTransactionItemDescription, MAX_TEXT_LENGTH, binding.textViewAddDetailedTransactionDescriptionIndicator) { text ->
                    val absPosition = holder.absoluteAdapterPosition
                    currentItem = absPosition
                    var latestItem = _items[absPosition]
                    if(binding.editTextAddDetailedTransactionItemDescription.hasFocus()) {
                        if (latestItem.description != text) {
                            listener?.onItemChanged(absPosition, latestItem.copy(description = text))
                        }
                    }
                }
                binding.editTextAddDetailedTransactionItemDescription.addTextChangedListener(newDescriptionWatcher)
                textWatcherDescriptionMap[binding.editTextAddDetailedTransactionItemDescription.hashCode()] = newDescriptionWatcher

                var categoryPosition = categories.indexOf(cachedItem.category)
                if(categoryPosition == -1) categoryPosition = 0
                binding.spinnerAddDetailedTransactionItemCategory.adapter = spinnerAdapter
                binding.spinnerAddDetailedTransactionItemCategory.setSelection(categoryPosition)
                binding.spinnerAddDetailedTransactionItemCategory.onItemSelectedListener = object : OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, _position: Int, id: Long) {
                        val absPosition = holder.absoluteAdapterPosition
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


                binding.textViewAddDetailedTransactionShowDetails.setOnClickListener {
                    val absPosition = holder.absoluteAdapterPosition
                    currentItem = absPosition
                    var latestItem = _items[absPosition]
                    binding.linearLayoutAddDetailedTransactionDetails.visibility = if(!latestItem.showDetails) View.VISIBLE else View.GONE
                    latestItem = latestItem.copy(showDetails = !latestItem.showDetails)
                    listener?.onItemChangedInvalidate(absPosition, latestItem)
                    if(latestItem.showDetails) {
                        binding.imageViewAddDetailedTransactionItemShowDetails.animate().setDuration(500L).rotation(0f).start()
                    } else {
                        binding.imageViewAddDetailedTransactionItemShowDetails.animate().setDuration(500L).rotation(90f).start()
                    }
                }

                binding.checkBoxAddDetailedTransactionIsReduction.setOnCheckedChangeListener(null)
                binding.checkBoxAddDetailedTransactionIsReduction.isChecked = cachedItem.isReduction
                binding.checkBoxAddDetailedTransactionIsReduction.text = "Is Discount/Deduction" //TODO Pass debitOrCredit into this adapter
                binding.checkBoxAddDetailedTransactionIsReduction.setOnCheckedChangeListener { buttonView, isChecked ->
                    val absPosition = holder.absoluteAdapterPosition
                    currentItem = absPosition
                    var latestItem = _items[absPosition]
                    latestItem = latestItem.copy(isReduction = isChecked)
                    listener?.onItemChangedInvalidate(absPosition, latestItem)
                }

                //region Brand
                val oldBrandWatcher = textWatcherBrandMap[binding.editTextAddDetailedTransactionItemBrand.hashCode()]
                if(oldBrandWatcher != null) {
                    binding.editTextAddDetailedTransactionItemBrand.removeTextChangedListener(oldBrandWatcher)
                }

                val cachedBrand = cachedItem.brand?:""
                binding.editTextAddDetailedTransactionItemBrand.setText(cachedBrand)
                binding.textViewAddDetailedTransactionItemBrandIndicator.text = cachedBrand.codePointCount(0, cachedBrand.length).toString() + "/" + MAX_TEXT_LENGTH

                val newBrandWatcher = MaxCodePointWatcher(binding.editTextAddDetailedTransactionItemBrand, MAX_TEXT_LENGTH, binding.textViewAddDetailedTransactionItemBrandIndicator) { text ->
                    val absPosition = holder.absoluteAdapterPosition
                    if(binding.editTextAddDetailedTransactionItemBrand.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.brand != text) {
                            listener?.onItemChanged(absPosition, latestItem.copy(brand = text))
                        }
                    }
                }
                binding.editTextAddDetailedTransactionItemBrand.addTextChangedListener(newBrandWatcher)
                textWatcherBrandMap[binding.editTextAddDetailedTransactionItemBrand.hashCode()] = newBrandWatcher
                //endregion

                //region Variation

                val oldVariationWatcher = textWatcherVariationMap[binding.editTextAddDetailedTransactionItemVariation.hashCode()]
                if(oldVariationWatcher != null) {
                    binding.editTextAddDetailedTransactionItemVariation.removeTextChangedListener(oldVariationWatcher)
                }

                val cachedVariation = cachedItem.variation
                binding.editTextAddDetailedTransactionItemVariation.setText(cachedVariation)
                binding.textViewAddDetailedTransactionItemVariationIndicator.text = cachedVariation.codePointCount(0, cachedVariation.length).toString() + "/" + MAX_TEXT_LENGTH


                val newVariationWatcher = MaxCodePointWatcher(binding.editTextAddDetailedTransactionItemVariation, MAX_TEXT_LENGTH, binding.textViewAddDetailedTransactionItemVariationIndicator) { text ->
                    if(binding.editTextAddDetailedTransactionItemVariation.hasFocus()) {
                        val absPosition = holder.absoluteAdapterPosition
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.variation != text.toString()) {
                            listener?.onItemChanged(absPosition, latestItem.copy(variation = text.toString()))
                        }
                    }
                }
                binding.editTextAddDetailedTransactionItemVariation.addTextChangedListener(newVariationWatcher)
                textWatcherVariationMap[binding.editTextAddDetailedTransactionItemVariation.hashCode()] = newVariationWatcher
                //endregion

                val oldQuantityWatcher = textWatcherQuantityMap[binding.editTextAddDetailedTransactionItemQuantity.hashCode()]
                if(oldQuantityWatcher != null) {
                    binding.editTextAddDetailedTransactionItemQuantity.removeTextChangedListener(oldQuantityWatcher)
                }

                binding.editTextAddDetailedTransactionItemQuantity.setText(cachedItem.quantity.toString())

                val newQuantityWatcher = NumberFormatTextWatcher(binding.editTextAddDetailedTransactionItemQuantity, BigDecimal(100), timeAndLocaleHandler.getLocale()) { amount ->
                    val absPosition = holder.absoluteAdapterPosition
                    if(binding.editTextAddDetailedTransactionItemQuantity.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]

                        if (latestItem.quantity != (amount?.toInt() ?: 1)) {
                            listener?.onItemChanged(absPosition, latestItem.copy(quantity = amount?.toInt() ?: 1))
                        }
                    }
                }
                binding.editTextAddDetailedTransactionItemQuantity.addTextChangedListener(newQuantityWatcher)
                textWatcherQuantityMap[binding.editTextAddDetailedTransactionItemQuantity.hashCode()] = newQuantityWatcher

                //region RefNum
                val oldReferenceListener = textWatcherReferenceMap[binding.editTextAddDetailedTransactionItemReference.hashCode()]
                if(oldReferenceListener != null) {
                    binding.editTextAddDetailedTransactionItemReference.removeTextChangedListener(oldReferenceListener)
                }
                val cachedRefNum = cachedItem.referenceNumber?:""
                binding.editTextAddDetailedTransactionItemReference.setText(cachedRefNum)
                binding.textViewAddDetailedTransactionItemRefNumIndicator.text = cachedRefNum.codePointCount(0, cachedRefNum.length).toString() + "/" + MAX_TEXT_LENGTH

                val newReferenceWatcher = MaxCodePointWatcher(binding.editTextAddDetailedTransactionItemReference, MAX_TEXT_LENGTH, binding.textViewAddDetailedTransactionItemRefNumIndicator) { text ->
                    val absPosition = holder.absoluteAdapterPosition
                    if(binding.editTextAddDetailedTransactionItemReference.hasFocus()) {
                        currentItem = absPosition
                        var latestItem = _items[absPosition]
                        if (latestItem.referenceNumber != text) {
                            listener?.onItemChanged(absPosition, latestItem.copy(referenceNumber = text))
                        }
                    }
                }
                binding.editTextAddDetailedTransactionItemReference.addTextChangedListener(newReferenceWatcher)
                textWatcherReferenceMap[binding.editTextAddDetailedTransactionItemReference.hashCode()] = newReferenceWatcher

                //endregion
                binding.textViewAddDetailedTransactionItemImageCount.text = cachedItem.images.size.toString() + "/" + Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM
                val adapter = AddTransactionItemImagesRecyclerAdapter(cachedItem.images, object: AddTransactionItemImagesRecyclerAdapter.ItemImageClickListener {

                    override fun onDeleteImage(position: Int) {
                        val absPosition = holder.absoluteAdapterPosition
                        listener?.onDeleteItemImage(absPosition, position)
                    }
                })
                val layoutManager = LinearLayoutManager(binding.root.context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                binding.recyclerviewAddDetailedTransactionItemImages.adapter = adapter
                binding.recyclerviewAddDetailedTransactionItemImages.layoutManager = layoutManager
                if(cachedItem.images.size >= Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM) {
                    binding.imageViewAddDetailedTransactionItemAddImage.isEnabled = false
                    binding.imageViewAddDetailedTransactionItemAddImage.alpha = ALPHA_DISABLED
                    binding.imageViewAddDetailedTransactionItemAddImage.setOnClickListener(null)
                } else {
                    binding.imageViewAddDetailedTransactionItemAddImage.isEnabled = true
                    binding.imageViewAddDetailedTransactionItemAddImage.alpha = 1f
                    binding.imageViewAddDetailedTransactionItemAddImage.setOnClickListener {
                        val absPosition = holder.absoluteAdapterPosition
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
        const val MAX_TEXT_LENGTH = 100
    }
}

