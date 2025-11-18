package com.davidgrath.expensetracker.ui.transactiondetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionDetailsItemBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.TransactionDetailItemUi
import com.davidgrath.expensetracker.formatDecimal

class TransactionDetailsItemRecyclerAdapter(private var items: List<TransactionDetailItemUi>, val timeAndLocaleHandler: TimeAndLocaleHandler): RecyclerView.Adapter<TransactionDetailsItemRecyclerAdapter.TransactionDetailsItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionDetailsItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewTransactionDetailsItemBinding.inflate(inflater, parent, false)
        val viewHolder = TransactionDetailsItemViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: TransactionDetailsItemViewHolder, position: Int) {
        items[position].let { item ->
            holder.binding.let { binding ->
                binding.textViewTransactionDetailsItemDescription.text = item.description
                binding.imageViewTransactionDetailsItemCategory.setImageResource(item.primaryCategory.iconId)
                //TODO Currency symbol, maybe
                binding.textViewTransactionDetailsItemPrice.text = if(item.isReduction) {
                    "${item.accountCurrencyCode} -${formatDecimal(item.amount, timeAndLocaleHandler.getLocale())}"
                } else {
                    "${item.accountCurrencyCode} ${formatDecimal(item.amount, timeAndLocaleHandler.getLocale())}"
                }
                binding.textViewTransactionDetailsItemVariation.text = item.variation
                binding.textViewTransactionDetailsItemReferenceNumber.text = item.referenceNumber
                binding.textViewTransactionDetailsItemQuantity.text = item.quantity.toString()
                binding.textViewTransactionDetailsItemBrand.text = item.brand
                binding.textViewTransactionDetailsItemCategoryName.text = item.primaryCategory.name
                val adapter = TransactionDetailsItemImagesRecyclerAdapter(item.images)
                val layoutManager = GridLayoutManager(binding.root.context, 5)
                binding.recyclerViewTransactionDetailsItemImages.adapter = adapter
                binding.recyclerViewTransactionDetailsItemImages.layoutManager = layoutManager
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun changeItems(items: List<TransactionDetailItemUi>) {
        this.items = items
        notifyDataSetChanged()
    }

    class TransactionDetailsItemViewHolder(val binding: RecyclerviewTransactionDetailsItemBinding): ViewHolder(binding.root)
}