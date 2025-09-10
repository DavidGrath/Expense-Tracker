package com.davidgrath.expensetracker.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionItemBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionDateBinding
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.io.File
import java.text.DecimalFormat

class TransactionItemsAdapter(private var items: List<GeneralTransactionListItem>, var listener: TransactionClickListener? = null): RecyclerView.Adapter<TransactionItemsAdapter.SealedViewHolder>() {

    fun interface TransactionClickListener {
        fun onTransactionClicked(transactionId: Long)
    }

    private val decimalFormat = DecimalFormat("0.00")
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)


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
            VIEW_TYPE_TRANSACTION_DATE -> {
                val binding = RecyclerviewTransactionDateBinding.inflate(inflater, parent, false)
                SealedViewHolder.TransactionDateViewHolder(binding)
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
                        val image = transactionItem.images.firstOrNull()
                        if(image != null && image.toFile().exists()) {
                            binding.imageViewTransactionItemFirstImage.visibility = View.VISIBLE
                            Glide.with(holder.binding.root)
                                .load(image)
                                .into(binding.imageViewTransactionItemFirstImage)
                        } else {
                            binding.imageViewTransactionItemFirstImage.visibility = View.GONE
                            Glide.with(holder.binding.root)
                                .clear(binding.imageViewTransactionItemFirstImage)
                        }
                    }
                }
            }
            is SealedViewHolder.TransactionViewHolder -> {
                items[position].transaction!!.let { transaction ->
                    holder.binding.let { binding ->
                        if(transaction.datedTime == null) {
                            binding.textViewTransactionTime.text = ""
                        } else {
                            binding.textViewTransactionTime.text = timeFormat.format(transaction.datedTime)
                        }
//                        binding.textViewTransactionAmount.text = transaction.currencyCode + " " + decimalFormat.format(transaction.amount)
                        binding.textViewTransactionAmount.text = ""
                        binding.root.setOnClickListener {
                            listener?.onTransactionClicked(transaction.id)
                        }
                    }
                }
            }
            is SealedViewHolder.TransactionDateViewHolder -> {
                items[position].date!!.let { date ->
                    holder.binding.let { binding ->
                        binding.textViewTransactionDateDate.text = dateFormat.format(date)
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            GeneralTransactionListItem.Type.Transaction -> VIEW_TYPE_TRANSACTION
            GeneralTransactionListItem.Type.TransactionItem -> VIEW_TYPE_TRANSACTION_ITEM
            GeneralTransactionListItem.Type.Date -> VIEW_TYPE_TRANSACTION_DATE
        }
    }

    fun setItems(items: List<GeneralTransactionListItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    sealed class SealedViewHolder(itemView: View): ViewHolder(itemView) {
        class TransactionViewHolder(val binding: RecyclerviewTransactionBinding): SealedViewHolder(binding.root)
        class TransactionItemViewHolder(val binding: RecyclerviewTransactionItemBinding): SealedViewHolder(binding.root)
        class TransactionDateViewHolder(val binding: RecyclerviewTransactionDateBinding): SealedViewHolder(binding.root)
    }

    companion object {
        const val VIEW_TYPE_TRANSACTION = 100
        const val VIEW_TYPE_TRANSACTION_ITEM = 101
        const val VIEW_TYPE_TRANSACTION_DATE = 102
    }
}