package com.davidgrath.expensetracker.ui.main.accounts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.FragmentAccountsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.ui.dialogs.AddAccountDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.EditAccountDialogFragment
import com.davidgrath.expensetracker.ui.main.MainViewModel
import org.slf4j.LoggerFactory
import java.util.Currency
import javax.inject.Inject

class AccountsFragment: Fragment(), OnClickListener, AccountsRecyclerAdapter.AccountClickListener, AddAccountDialogFragment.AddAccountListener, EditAccountDialogFragment.EditAccountListener {

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentAccountsBinding
    private var addAccountDialogFragment: AddAccountDialogFragment? = null
    private var editAccountDialogFragment: EditAccountDialogFragment? = null
    private var listener: AccountsFragmentListener? = null
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    interface AccountsFragmentListener {
        fun onNavigateToStats()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        LOGGER.info("onAttach")
        if(context is AccountsFragmentListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAccountsBinding.inflate(layoutInflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireContext().applicationContext as ExpenseTracker
        app.appComponent.inject(this)
        val adapter = AccountsRecyclerAdapter(emptyList(), timeAndLocaleHandler, this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewAccounts.adapter = adapter
        binding.recyclerviewAccounts.layoutManager = layoutManager
        viewModel.accountWithStatsLiveData.observe(viewLifecycleOwner) { accounts ->
            adapter.setItems(accounts)
        }
        binding.fabAccounts.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v) {
            binding.fabAccounts -> {


                if(addAccountDialogFragment == null) {
                    LOGGER.info("AddAccount dialog is null, creating")
                    addAccountDialogFragment = AddAccountDialogFragment()
                }
                if(!(addAccountDialogFragment?.dialog?.isShowing?:false)) {
                    addAccountDialogFragment?.show(childFragmentManager,
                        FRAGMENT_TAG_ADD_ACCOUNT
                    )
                    LOGGER.info("Showed addAccountDialog")
                }
            }
        }
    }

    override fun onEditClicked(accountId: Long, accountName: String) {
        if(editAccountDialogFragment == null) {
            LOGGER.info("EditAccount dialog is null, creating")
            editAccountDialogFragment = EditAccountDialogFragment.createDialog(accountId, accountName)
        }
        if(!(editAccountDialogFragment?.dialog?.isShowing?:false)) {
            editAccountDialogFragment?.show(childFragmentManager,
                FRAGMENT_TAG_EDIT_ACCOUNT
            )
            LOGGER.info("Showed editAccountDialog")
        }
    }

    override fun onViewStatsClicked(accountId: Long) {
        LOGGER.info("onViewStatsClicked: $accountId") //TODO Move this log statement to the call site
        viewModel.setAccountFilter(accountId)
        listener?.onNavigateToStats()
    }

    override fun onAddAccount(name: String, currency: Currency) {
        viewModel.addAccount(name, currency.currencyCode)
    }

    override fun onEditAccount(accountId: Long, name: String) {
        viewModel.editAccount(accountId, name)
    }

    companion object {
        fun newInstance(): AccountsFragment {
            val statisticsFragment = AccountsFragment()
            return statisticsFragment
        }
        private const val FRAGMENT_TAG_ADD_ACCOUNT = "addAccount"
        private const val FRAGMENT_TAG_EDIT_ACCOUNT = "editAccount"
        private val LOGGER = LoggerFactory.getLogger(AccountsFragment::class.java)
    }
}