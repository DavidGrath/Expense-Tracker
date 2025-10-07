package com.davidgrath.expensetracker.ui.main.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.davidgrath.expensetracker.databinding.FragmentAccountsBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewAccountBinding
import com.davidgrath.expensetracker.entities.ui.AccountUi

class AccountsRecyclerAdapter(private var _items: List<AccountUi>, var listener: AccountClickListener? = null): RecyclerView.Adapter<AccountsRecyclerAdapter.AccountsViewHolder>() {

    interface AccountClickListener {
        fun onEditClicked(accountId: Long, accountName: String)
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

                binding.imageViewAccountEdit.setOnClickListener {
                    listener?.onEditClicked(account.id, account.name)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return _items.size
    }

    fun setItems(items: List<AccountUi>) {
        this._items = items
        notifyDataSetChanged()
    }
    class AccountsViewHolder(val binding: RecyclerviewAccountBinding): RecyclerView.ViewHolder(binding.root)
}