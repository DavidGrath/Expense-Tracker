package com.davidgrath.expensetracker.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.annotation.OptIn
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.accountDbToAccountUi
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.FragmentTransactionsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.formatDecimal
import com.davidgrath.expensetracker.ui.AccountAdapter
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionOtherDetailsFragment
import com.davidgrath.expensetracker.ui.dialogs.AddTransactionDialogFragment
import com.davidgrath.expensetracker.ui.main.statistics.StatisticsFragment
import com.davidgrath.expensetracker.ui.transactiondetails.TransactionDetailsActivity
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import org.slf4j.LoggerFactory
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.math.BigDecimal
import javax.inject.Inject

class TransactionsFragment: Fragment(), OnClickListener, OnLongClickListener, AddTransactionDialogFragment.AddTransactionListener, TransactionItemsAdapter.TransactionClickListener,
    MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> {

    lateinit var binding: FragmentTransactionsBinding
    lateinit var viewModel: MainViewModel
    lateinit var adapter: TransactionItemsAdapter
    private var dateRangePicker: MaterialDatePicker<Pair<Long, Long>>? = null
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    private var accountId = -1L
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private lateinit var badgeDrawable: BadgeDrawable

    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, null, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(MainViewModel::class.java)
        val app = requireContext().applicationContext as ExpenseTracker
        val appComponent = app.appComponent
        appComponent.inject(this)
        startDate = LocalDate.now(timeAndLocaleHandler.getClock())
        endDate = LocalDate.now(timeAndLocaleHandler.getClock())
        adapter = TransactionItemsAdapter(emptyList(), timeAndLocaleHandler, this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val accountAdapter = AccountAdapter(requireContext(), mutableListOf<AccountUi>())

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val account = accountAdapter._objects[position]
                viewModel.setHomeAccountId(account.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        binding.spinnerTransactionsCurrentAccount.adapter = accountAdapter
        binding.spinnerTransactionsCurrentAccount.onItemSelectedListener = listener

        binding.recyclerviewTransactions.adapter = adapter
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())

        viewModel.accountsLiveData.observe(viewLifecycleOwner) { accounts ->
            accountAdapter.setItems(accounts)
            if(binding.spinnerTransactionsCurrentAccount.selectedItemPosition == Spinner.INVALID_POSITION) {
                binding.spinnerTransactionsCurrentAccount.onItemSelectedListener = null
                binding.spinnerTransactionsCurrentAccount.setSelection(0)
                binding.spinnerTransactionsCurrentAccount.onItemSelectedListener = listener
            }

            val accountPosition = accountAdapter._objects.indexOfFirst { it.id == accountId }
            binding.spinnerTransactionsCurrentAccount.onItemSelectedListener = null

            if(accountPosition == -1) {
                LOGGER.warn("Current Account not available in spinner. Changing selection to first item")
                binding.spinnerTransactionsCurrentAccount.setSelection(0)
            } else {
                LOGGER.info("Current Account of spinner changed")
                binding.spinnerTransactionsCurrentAccount.setSelection(accountPosition)
            }
            binding.spinnerTransactionsCurrentAccount.onItemSelectedListener = listener
        }
        viewModel.homeListLiveData.observe(viewLifecycleOwner) { list ->
            LOGGER.info("Transactions Item Count: {}", list.size)
            adapter.setItems(list)
        }
        viewModel.homeConfigLiveData.observe(viewLifecycleOwner) {
            this.startDate = it.startDate
            this.endDate = it.endDate
            val startDateFormat = dateFormat.format(startDate)
            if(startDate.compareTo(endDate) != 0) {
                val endDateFormat = dateFormat.format(endDate)
                binding.textViewTransactionsCurrentDate.text = "$startDateFormat - $endDateFormat"
                binding.imageButtonTransactionsCycleDateRight.isEnabled = false
                binding.imageButtonTransactionsCycleDateLeft.isEnabled = false
            } else {
                binding.textViewTransactionsCurrentDate.text = startDateFormat
                binding.imageButtonTransactionsCycleDateRight.isEnabled = true
                binding.imageButtonTransactionsCycleDateLeft.isEnabled = true
            }
            this.accountId = it.accountId
        }
        viewModel.homeTotalIncome.observe(viewLifecycleOwner) {
            binding.textViewTransactionsTotalIncome.text = formatDecimal(it, timeAndLocaleHandler.getLocale())
        }
        viewModel.homeTotalExpense.observe(viewLifecycleOwner) {
            binding.textViewTransactionsTotalExpense.text = formatDecimal(it, timeAndLocaleHandler.getLocale())
        }

        binding.fabTransactions.setOnClickListener(this)
        binding.fabTransactions.setOnLongClickListener(this)
        binding.imageButtonTransactionsCycleDateLeft.setOnClickListener(this)
        binding.imageButtonTransactionsCycleDateRight.setOnClickListener(this)
        binding.textViewTransactionsCurrentDate.setOnClickListener(this)
        badgeDrawable = BadgeDrawable.create(requireContext())
    }

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onResume() {
        super.onResume()
        if(viewModel.doesDraftExist()) {
            badgeDrawable.setVisible(true)
            BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.fabTransactions)
        } else {
            badgeDrawable.setVisible(false)
            BadgeUtils.detachBadgeDrawable(badgeDrawable, binding.fabTransactions)
        }
    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.fabTransactions -> {
//                    if(addTransactionDialog == null) {
//
//                    }
                    val addTransactionDialog = AddTransactionDialogFragment.createDialog(viewModel.currentProfile)
//                    if(!(addTransactionDialog?.dialog?.isShowing?:false)) {
                        addTransactionDialog?.show(childFragmentManager, FRAGMENT_TAG_ADD_TRANSACTION)
                        LOGGER.info("Showed addTransactionDialog")
//                    }
                }
                binding.imageButtonTransactionsCycleDateLeft -> {
                    viewModel.decrementHomeDay()
                }
                binding.imageButtonTransactionsCycleDateRight -> {
                    viewModel.incrementHomeDay()
                }
                binding.textViewTransactionsCurrentDate -> {
                    dateRangePicker = childFragmentManager.findFragmentByTag(DIALOG_TAG_DATE) as MaterialDatePicker<Pair<Long, Long>>?
                    if(dateRangePicker == null) {
                        LOGGER.info("dateRangePicker is null. Creating")
                        val calendarConstraints = CalendarConstraints.Builder()
                            .setValidator(DateValidatorPointBackward.now())
                            .build()
                        val builder = MaterialDatePicker.Builder.dateRangePicker()
                            .setCalendarConstraints(calendarConstraints)

                            val startMillis = startDate.atStartOfDay().toInstant(
                                ZoneOffset.UTC).toEpochMilli()
                            val endMillis = startDate.atTime(LocalTime.MAX).toInstant(
                                ZoneOffset.UTC).toEpochMilli()
                            builder.setSelection(Pair(startMillis, endMillis))

                        builder.setSelection(Pair(startMillis, endMillis))
                        dateRangePicker = builder.build()
                        dateRangePicker!!.addOnPositiveButtonClickListener(this)
                    }
                    if(!(dateRangePicker?.dialog?.isShowing?: false)) {
                        dateRangePicker!!.show(childFragmentManager,
                            DIALOG_TAG_DATE
                        )
                        LOGGER.info("Showed dateRangePicker")
                    }
                }
                else -> {}
            }
        }
    }


    override fun onLongClick(v: View?): Boolean {
        v?.let {
            when(v) {
                binding.fabTransactions -> {
                    val intent = Intent(requireActivity(), AddDetailedTransactionActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        return true
    }

    override fun onPositiveButtonClick(selection: Pair<Long, Long>) {
        selection?.let {
            LOGGER.info("dateRangePicker date picked")
            val startInstant = Instant.ofEpochMilli(it.first)
            val startLocalDate = startInstant.atZone(timeAndLocaleHandler.getZone()).toLocalDate()

            val endInstant = Instant.ofEpochMilli(it.second)
            val endLocalDate = endInstant.atZone(timeAndLocaleHandler.getZone()).toLocalDate()
            viewModel.setHomeDateRange(startLocalDate, endLocalDate)
        }
    }

    override fun onGoToDetails(accountId: Long, debitOrCredit: Boolean, amount: BigDecimal?, description: String?, categoryId: Long?) {
        val bundle = AddDetailedTransactionActivity.createBundle(accountId, amount?.toString(), description, categoryId)
        val intent = Intent(requireActivity(), AddDetailedTransactionActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    override fun onAddTransaction(accountId: Long, debitOrCredit: Boolean, amount: BigDecimal, description: String, categoryId: Long) {
        viewModel.saveTransaction(accountId, debitOrCredit, amount, description, categoryId)
    }

    override fun onTransactionClicked(transactionId: Long) {
        val intent = Intent(requireActivity(), TransactionDetailsActivity::class.java)
        intent.putExtra(TransactionDetailsActivity.ARG_TRANSACTION_ID, transactionId)
        startActivity(intent)
    }

    companion object {
        fun newInstance(): TransactionsFragment {
            val transactionsFragment = TransactionsFragment()
            return transactionsFragment
        }
        private const val FRAGMENT_TAG_ADD_TRANSACTION = "addTransaction"
        private const val DIALOG_TAG_DATE = "dateDialog"

        private val LOGGER = LoggerFactory.getLogger(TransactionsFragment::class.java)
    }
}