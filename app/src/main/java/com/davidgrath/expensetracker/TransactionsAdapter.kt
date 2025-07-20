package com.davidgrath.expensetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.davidgrath.expensetracker.databinding.RecyclerviewTransactionBinding
import com.davidgrath.expensetracker.entities.ui.Transaction
import java.text.DecimalFormat

class TransactionsAdapter(private var items: List<Transaction>): RecyclerView.Adapter<TransactionsAdapter.TransactionsViewHolder>() {

    private val decimalFormat = DecimalFormat("0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewTransactionBinding.inflate(inflater, parent, false)
        return TransactionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        items[position].let { transaction ->
            holder.binding.let { binding ->
                binding.textViewTransactionStore.visibility = View.GONE
                binding.textViewTransactionAmount.text = transaction.currencyCode + " " + decimalFormat.format(transaction.amount)
                binding.textViewTransactionDescription.text = transaction.description
                binding.imageViewTransactionCategory.setImageResource(R.drawable.baseline_restaurant_24)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: List<Transaction>) {
        this.items = items
        notifyDataSetChanged()
    }
    class TransactionsViewHolder(val binding: RecyclerviewTransactionBinding): ViewHolder(binding.root) {

    }
}