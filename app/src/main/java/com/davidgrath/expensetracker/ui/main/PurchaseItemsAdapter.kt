package com.davidgrath.expensetracker.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.databinding.RecyclerviewPurchaseItemBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionBinding
import com.davidgrath.expensetracker.entities.ui.TransactionItem
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.DecimalFormat

class PurchaseItemsAdapter(private var items: List<TransactionItem>): RecyclerView.Adapter<PurchaseItemsAdapter.SealedViewHolder>() {

    private val decimalFormat = DecimalFormat("0.00")
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SealedViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val viewHolder = when(viewType) {
            VIEW_TYPE_TRANSACTION -> {
                val binding = RecyclerviewTransactionBinding.inflate(inflater, parent, false)
                SealedViewHolder.TransactionViewHolder(binding)
            }
            VIEW_TYPE_PURCHASE_ITEM -> {
                val binding = RecyclerviewPurchaseItemBinding.inflate(inflater, parent, false)
                SealedViewHolder.PurchaseItemViewHolder(binding)
            }
            else -> {
                val binding = RecyclerviewTransactionBinding.inflate(inflater, parent, false)
                SealedViewHolder.TransactionViewHolder(binding)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: SealedViewHolder, position: Int) {
        when(holder) {
            is SealedViewHolder.PurchaseItemViewHolder -> {
                items[position].purchaseItem!!.let { purchaseItem ->
                    holder.binding.let { binding ->
                        binding.textViewPurchaseItemStore.visibility = View.GONE
                        binding.textViewPurchaseItemAmount.text = purchaseItem.transaction.currencyCode + " " + decimalFormat.format(purchaseItem.amount)
                        binding.textViewPurchaseItemDescription.text = purchaseItem.description
                        binding.imageViewPurchaseItemCategory.setImageResource(purchaseItem.category.iconId)
                    }
                }
            }
            is SealedViewHolder.TransactionViewHolder -> {
                items[position].transaction!!.let { transaction ->
                    holder.binding.let { binding ->
                        binding.textViewTransactionDate.text = dateFormat.format(transaction.timestamp)
                        binding.textViewTransactionAmount.text = transaction.currencyCode + " " + decimalFormat.format(transaction.amount)
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].transactionOrItem) VIEW_TYPE_TRANSACTION else VIEW_TYPE_PURCHASE_ITEM
    }

    fun setItems(items: List<TransactionItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    sealed class SealedViewHolder(itemView: View): ViewHolder(itemView) {
        class TransactionViewHolder(val binding: RecyclerviewTransactionBinding): SealedViewHolder(binding.root)
        class PurchaseItemViewHolder(val binding: RecyclerviewPurchaseItemBinding): SealedViewHolder(binding.root)
    }

    companion object {
        val VIEW_TYPE_TRANSACTION = 100
        val VIEW_TYPE_PURCHASE_ITEM = 101
    }
}