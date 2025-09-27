package com.davidgrath.expensetracker.ui.transactiondetails

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityTransactionDetailsBinding
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.EvidenceRepository
import com.davidgrath.expensetracker.repositories.ImageRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionActivity
import com.google.android.material.tabs.TabLayoutMediator
import org.slf4j.LoggerFactory
import javax.inject.Inject

class TransactionDetailsActivity: AppCompatActivity() {

    lateinit var binding: ActivityTransactionDetailsBinding
    lateinit var viewModel: TransactionDetailsViewModel
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var transactionItemRepository: TransactionItemRepository
    @Inject
    lateinit var imageRepository: ImageRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var evidenceRepository: EvidenceRepository
    @Inject
    lateinit var accountRepository: AccountRepository

    var transactionId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transactionId = intent.getLongExtra(ARG_TRANSACTION_ID, -1)
        //TODO Error Dialog on -1
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        val component = app.appComponent
        component.inject(this)
        viewModel = ViewModelProvider.create(viewModelStore,
            TransactionDetailsViewModelFactory(transactionId, transactionRepository,
                transactionItemRepository, imageRepository, categoryRepository, evidenceRepository,
                accountRepository
            )
        ).get(TransactionDetailsViewModel::class)
        setSupportActionBar(binding.toolbarTransactionDetails)
        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                if(position == 0) {
                    return TransactionDetailsItemsFragment.newInstance()
                } else {
                    return TransactionDetailsEvidenceFragment.newInstance()
                }
            }
        }
        binding.viewPagerTransactionDetails.adapter = pagerAdapter
        val tabLayoutMediator = TabLayoutMediator(binding.tabLayoutTransactionDetails, binding.viewPagerTransactionDetails) { tab, position ->
            if (position == 0) {
                tab.text = "Items"
            } else {
                tab.text = "Evidence"
            }
        }
        tabLayoutMediator.attach()

        viewModel.transaction.observe(this) { transaction ->
            binding.transactionDetailsNote.text = transaction.note
            binding.textViewTransactionDetailsTotal.text = transaction.currencyCode + " " + transaction.amount
        }
        setContentView(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_transaction_details, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_item_transaction_details_edit -> {
                val bundle = bundleOf(AddDetailedTransactionActivity.ARG_MODE to "edit", AddDetailedTransactionActivity.ARG_EDIT_TRANSACTION_ID to transactionId)
                val intent = Intent(this, AddDetailedTransactionActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent) //TODO Remove this activity from the task/backstack
                LOGGER.info("Started AddDetailedTransactionActivity for edit")
                true
            }
            R.id.menu_item_transaction_details_delete -> {
                Toast.makeText(this, "Not yet", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
        private val LOGGER = LoggerFactory.getLogger(TransactionDetailsActivity::class.java)
    }
}