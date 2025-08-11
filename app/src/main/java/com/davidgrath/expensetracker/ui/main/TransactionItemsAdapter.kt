package com.davidgrath.expensetracker.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionItemBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionBinding
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.DecimalFormat

class TransactionItemsAdapter(private var items: List<GeneralTransactionListItem>): RecyclerView.Adapter<TransactionItemsAdapter.SealedViewHolder>() {

    private val decimalFormat = DecimalFormat("0.00")
    private val dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SealedViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val viewHolder = when(viewType) {
            VIEW_TYPE_TRANSACTION -> {
                val binding = RecyclerviewTransactionBinding.inflate(inflater, parent, false)
                SealedViewHolder.TransactionViewHolder(binding)
            }
            VIEW_TYPE_TRANSACTION_ITEM -> {
                val binding = RecyclerviewTransactionItemBinding.inflate(inflater, parent, false)
                SealedViewHolder.TransactionItemViewHolder(binding)
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
            is SealedViewHolder.TransactionItemViewHolder -> {
                items[position].transactionItem!!.let { transactionItem ->
                    holder.binding.let { binding ->
                        binding.textViewTransactionItemStore.visibility = View.GONE
                        binding.textViewTransactionItemAmount.text = transactionItem.transaction.currencyCode + " " + decimalFormat.format(transactionItem.amount)
                        binding.textViewTransactionItemDescription.text = transactionItem.description
                        binding.imageViewTransactionItemCategory.setImageResource(transactionItem.category.iconId)
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
        return if (items[position].transactionOrItem) VIEW_TYPE_TRANSACTION else VIEW_TYPE_TRANSACTION_ITEM
    }

    fun setItems(items: List<GeneralTransactionListItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    sealed class SealedViewHolder(itemView: View): ViewHolder(itemView) {
        class TransactionViewHolder(val binding: RecyclerviewTransactionBinding): SealedViewHolder(binding.root)
        class TransactionItemViewHolder(val binding: RecyclerviewTransactionItemBinding): SealedViewHolder(binding.root)
    }

    companion object {
        val VIEW_TYPE_TRANSACTION = 100
        val VIEW_TYPE_TRANSACTION_ITEM = 101
    }
}