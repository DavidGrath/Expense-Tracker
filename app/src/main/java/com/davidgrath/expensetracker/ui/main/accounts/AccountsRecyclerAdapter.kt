package com.davidgrath.expensetracker.ui.main.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.davidgrath.expensetracker.databinding.FragmentAccountsBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewAccountBinding
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AccountWithStatsUi
import java.util.Locale

class AccountsRecyclerAdapter(private var _items: List<AccountWithStatsUi>, var listener: AccountClickListener? = null): RecyclerView.Adapter<AccountsRecyclerAdapter.AccountsViewHolder>() {

    interface AccountClickListener {
        fun onEditClicked(accountId: Long, accountName: String)
        fun onViewStatsClicked(accountId: Long)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewAccountBinding.inflate(inflater, parent, false)
        val viewHolder = AccountsViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int) {
        holder.binding.let { binding ->
            _items[position].let { account ->
                binding.textViewAccountName.text = account.name
                if(account.currencyCode == account.currencyDisplayName) {
                    binding.textViewAccountCurrencyCode.text = account.currencyCode
                } else {
                    binding.textViewAccountCurrencyCode.text = "${account.currencyDisplayName} (${account.currencyCode})"
                }
                binding.textViewAccountIncome.text = String.format(Locale.getDefault(), "%.2f", account.income)
                binding.textViewAccountExpenses.text = String.format(Locale.getDefault(), "%.2f", account.expenses)
                binding.textViewAccountTransactionCount.text = "${account.transactionCount} transactions"
                binding.textViewAccountItemCount.text = "${account.itemCount} items"
                binding.imageViewAccountEdit.setOnClickListener {
                    listener?.onEditClicked(account.id, account.name)
                }
                binding.imageViewAccountViewStats.setOnClickListener {
                    listener?.onViewStatsClicked(account.id)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return _items.size
    }

    fun setItems(items: List<AccountWithStatsUi>) {
        this._items = items
        notifyDataSetChanged()
    }
    class AccountsViewHolder(val binding: RecyclerviewAccountBinding): RecyclerView.ViewHolder(binding.root)
}