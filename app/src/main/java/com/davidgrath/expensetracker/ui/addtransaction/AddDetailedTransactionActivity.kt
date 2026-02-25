package com.davidgrath.expensetracker.ui.addtransaction

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionBinding
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.ImageAddResult
import com.davidgrath.expensetracker.file
import com.davidgrath.expensetracker.formatDecimal
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.ui.dialogs.AddImageDialogFragment
import com.davidgrath.expensetracker.ui.dialogs.GenericDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

class AddDetailedTransactionActivity : AppCompatActivity(),
    AddDetailedTransactionMainFragment.AddDetailedTransactionMainListener, GenericDialogFragment.GenericDialogListener,
    View.OnClickListener, AddImageDialogFragment.AddImageDialogListener {

    lateinit var binding: ActivityAddDetailedTransactionBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var profileDao: ProfileDao
    @Inject
    lateinit var addDetailedTransactionRepository: AddDetailedTransactionRepository
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    var mode = "add"
    var finishingDraft = false

    var noPagesDialogFragment: GenericDialogFragment? = null
    var passwordDialogFragment: GenericDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LOGGER.info("onCreate")
        binding = ActivityAddDetailedTransactionBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        app.appComponent.inject(this)
        val extras = intent.extras
        var amount: BigDecimal? = null
        var description: String? = null
        var categoryId: Long? = null
        var accountId: Long? = null
        var transactionId: Long? = null
        if (extras != null) {
            mode = extras.getString(ARG_MODE)?: "add"
            if(mode == "edit") {
                transactionId = extras.getLong(ARG_EDIT_TRANSACTION_ID)
            } else if(mode == "add"){
                val amountString = extras.getString(ARG_INITIAL_AMOUNT)
                amount = if (amountString != null) BigDecimal(amountString) else null
                description = extras.getString(ARG_INITIAL_DESCRIPTION)
                categoryId = extras.getLong(ARG_INITIAL_CATEGORY_ID)
                accountId = extras.getLong(ARG_INITIAL_ACCOUNT_ID)
            }
        }
        val preferences = application.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE) //TODO Create profile Observable in Application
        val currentProfileStringId = preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null)!!

        viewModel = ViewModelProvider.create(
            viewModelStore,
            AddDetailedTransactionViewModelFactory(app, mode, currentProfileStringId, transactionId, accountId, amount, description, categoryId)
        ).get(AddDetailedTransactionViewModel::class.java)
        setContentView(binding.root)

        binding.viewPagerAddDetailedTransaction.adapter = AddDetailedTransactionFragmentStateAdapter(this)
        TabLayoutMediator(binding.tabLayoutAddDetailedTransaction, binding.viewPagerAddDetailedTransaction) { tab, position ->
            if(position == 0) {
                tab.text = "Items"
            } else {
                tab.text = "Other Details"
            }
        }.attach()

        binding.imageViewAddDetailedTransactionDebitOrCredit.setOnClickListener(this)
        viewModel.transactionItemsLiveData.observe(this) { triple ->
            LOGGER.info("Event: ${triple.second}, Position: ${triple.third}")
            val draft = triple.first
            val list = draft.items
            val event = triple.second
            val position = triple.third
            binding.textViewAddDetailedTransactionMainItemCount.text = list.size.toString() + "/" + Constants.MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE
            if(draft.debitOrCredit) {
                binding.textViewAddDetailedTransactionMainTotal.setTextColor(ContextCompat.getColor(this, R.color.red_600))
                (binding.imageViewAddDetailedTransactionDebitOrCredit as MaterialButton).setIconResource(
                    R.drawable.baseline_remove_24)
                (binding.imageViewAddDetailedTransactionDebitOrCredit as MaterialButton).setIconTintResource(
                    R.color.red_600)
            } else {
                binding.textViewAddDetailedTransactionMainTotal.setTextColor(ContextCompat.getColor(this, R.color.green_600))
                (binding.imageViewAddDetailedTransactionDebitOrCredit as MaterialButton).setIconResource(
                    R.drawable.baseline_add_24)
                (binding.imageViewAddDetailedTransactionDebitOrCredit as MaterialButton).setIconTintResource(
                    R.color.green_600)
            }
        }
        viewModel.currentAccount.observe(this) { (accounts, account, total) ->
            val currencyCode = account.currencyCode
            binding.textViewAddDetailedTransactionMainTotalCurrency.text = currencyCode //TODO Currency symbols, maybe
            binding.textViewAddDetailedTransactionMainTotal.text = formatDecimal(total, timeAndLocaleHandler.getLocale())
        }
        binding.imageButtonAddDetailedTransactionDone.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isFinishing) {
            if(addDetailedTransactionRepository.isDraftEmpty() && mode == "add" && !finishingDraft) {
                LOGGER.info("Draft is empty. Discarding")
                Toast.makeText(this, "Draft is empty. Discarding", Toast.LENGTH_SHORT).show()
                addDetailedTransactionRepository.deleteDraft()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                when (requestCode) {
                    REQUEST_CODE_ITEM_OPEN_IMAGE -> {
                        LOGGER.info("onActivityResult: Add Image")
                        val uri = data!!.data!!
                        val liveData = viewModel.addItemFile(uri)
                        liveData.observe(this, object: Observer<ImageAddResult> {
                            override fun onChanged(value: ImageAddResult) {
                                if(value.actionNeeded) {
                                    val modificationDetails = value.imageModificationDetails!!
                                    val imageTooLarge = modificationDetails.reducedFileSize != null
                                    val hasGpsData = modificationDetails.locationLongLat != null
                                    val addImageDialog = AddImageDialogFragment.newInstance(uri.toString(), modificationDetails.itemId, modificationDetails.originalFileHash, modificationDetails.fileMimeType, imageTooLarge, modificationDetails.originalFileSize, modificationDetails.originalImageWidthHeight!!.first, modificationDetails.originalImageWidthHeight.second, modificationDetails.reducedFileSize
                                    , modificationDetails.reducedImageWidthHeight?.first, modificationDetails.reducedImageWidthHeight?.second, hasGpsData, modificationDetails.locationLongLat?.first, modificationDetails.locationLongLat?.second)
                                    addImageDialog.show(supportFragmentManager, DIALOG_TAG_ADD_IMAGE)
                                    LOGGER.info("onActivityResult: Action needed for selected image")
                                } else {
                                    LOGGER.info("onActivityResult: File add done")
                                }
                                liveData.removeObserver(this)
                            }
                        })
                    }
                    REQUEST_CODE_OPEN_DOCUMENT -> {
                        LOGGER.info("onActivityResult: Add Document")
                        val uri = data!!.data!!
                        val liveData = viewModel.addEvidence(uri)
                        liveData.observe(this, object: Observer<AddDetailedTransactionViewModel.PdfState> {
                            override fun onChanged(value: AddDetailedTransactionViewModel.PdfState) {
                                when(value) {
                                    AddDetailedTransactionViewModel.PdfState.NOT_PDF -> {
                                        LOGGER.info("Add evidence done")
                                    }
                                    AddDetailedTransactionViewModel.PdfState.ALL_GOOD -> {
                                        LOGGER.info("Add evidence done")
                                    }
                                    AddDetailedTransactionViewModel.PdfState.PASSWORD_PROTECTED -> {
                                        if(passwordDialogFragment == null) {
                                            passwordDialogFragment = GenericDialogFragment.newInstance(
                                                "Password Protected",
                                                "This PDF is password protected", "Ok", null, null, null, DIALOG_TAG_PASSWORD_PROTECTED
                                                )
                                            passwordDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_PASSWORD_PROTECTED)
                                            LOGGER.info("Showed PDF has password dialog")
                                        } else {
                                            if(!passwordDialogFragment!!.dialog!!.isShowing) {
                                                passwordDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_PASSWORD_PROTECTED)
                                                LOGGER.info("Showed PDF has password dialog")
                                            }
                                        }
                                    }
                                    AddDetailedTransactionViewModel.PdfState.ZERO_PAGES -> {
                                        if(noPagesDialogFragment == null) {
                                            noPagesDialogFragment = GenericDialogFragment.newInstance(
                                                "Zero pages",
                                                "Somehow, this PDF has zero pages", ":-)", null, null, null, DIALOG_TAG_NO_PAGES
                                            )
                                            noPagesDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_NO_PAGES)
                                            LOGGER.info("Showed no pages dialog")
                                        } else {
                                            if(!noPagesDialogFragment!!.dialog!!.isShowing) {
                                                noPagesDialogFragment!!.show(supportFragmentManager, DIALOG_TAG_NO_PAGES)
                                                LOGGER.info("Showed no pages dialog")
                                            }
                                        }
                                    }
                                }
                                liveData.removeObserver(this)
                            }
                        })
                    }
                    REQUEST_CODE_ITEM_CAPTURE_IMAGE -> {
                        LOGGER.debug("onActivityResult: Capture Image")
                        val cameraDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                        val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
                        val uri = cameraFile.toUri()
                        val liveData = viewModel.addItemFile(uri)
                        liveData.observe(this, object: Observer<ImageAddResult> {
                            override fun onChanged(value: ImageAddResult) {
                                if(value.actionNeeded) {
                                    val modificationDetails = value.imageModificationDetails!!
                                    val imageTooLarge = modificationDetails.reducedFileSize != null
                                    val hasGpsData = modificationDetails.locationLongLat != null
                                    val addImageDialog = AddImageDialogFragment.newInstance(uri.toString(), modificationDetails.itemId, modificationDetails.originalFileHash, modificationDetails.fileMimeType, imageTooLarge, modificationDetails.originalFileSize, modificationDetails.originalImageWidthHeight!!.first, modificationDetails.originalImageWidthHeight.second, modificationDetails.reducedFileSize
                                        , modificationDetails.reducedImageWidthHeight?.first, modificationDetails.reducedImageWidthHeight?.second, hasGpsData, modificationDetails.locationLongLat?.first, modificationDetails.locationLongLat?.second)
                                    addImageDialog.show(supportFragmentManager, DIALOG_TAG_ADD_IMAGE)
                                    LOGGER.info("onActivityResult: Action needed for selected image")
                                } else {
                                    LOGGER.info("onActivityResult: Camera capture done")
                                }
                                liveData.removeObserver(this)
                            }
                        })
                    }
                    REQUEST_CODE_DOCUMENT_CAPTURE_IMAGE -> {
                        LOGGER.debug("onActivityResult: Capture Document Image")
                        val cameraDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                        val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
                        val uri = cameraFile.toUri()
                        val liveData = viewModel.addEvidence(uri)
                        liveData.observe(this, object: Observer<AddDetailedTransactionViewModel.PdfState> {
                            override fun onChanged(value: AddDetailedTransactionViewModel.PdfState) {
                                LOGGER.info("onActivityResult: Camera capture done")
                                liveData.removeObserver(this)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when(v) {
                binding.imageButtonAddDetailedTransactionDone -> {
                    if(viewModel.validateDraft()) {
                        viewModel.finishDraft()
                            .observe(this) {
                                LOGGER.info("Transaction Added. Draft discarded")
                                onFinished() //TODO SimpleResult
                            }
                    } else {
                        Snackbar.make(binding.root, "Invalid input", Snackbar.LENGTH_SHORT).show()
                    }
                }
                binding.imageViewAddDetailedTransactionDebitOrCredit -> {
                    viewModel.toggleDebitOrCredit()
                }
                else -> {

                }
            }
        }
    }

    override fun onFinished() {
        finishingDraft = true
        finish()
    }

    override fun onPositiveButton(disambiguationTag: String, data: String?) {
        when(disambiguationTag) {
            DIALOG_TAG_NO_PAGES -> {

            }
            DIALOG_TAG_PASSWORD_PROTECTED -> {

            }
        }
    }

    override fun onNegativeButton(disambiguationTag: String, data: String?) {

    }

    override fun onNeutralButton(disambiguationTag: String, data: String?) {

    }

    override fun onAddConfirm(sourceHash: String, mimeType: String, itemId: Int?, reduceSize: Boolean, removeGpsData: Boolean) {
        viewModel.addSelectedImage(sourceHash, mimeType, itemId, reduceSize, removeGpsData).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                       LOGGER.info("Done adding selection")
            }, {
                LOGGER.error("Unexpected error: ", it)
            })
    }

    override fun onDismiss() {
        val folder = file(application.filesDir.absolutePath, Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGE_MODIFICATION)
        if(folder.exists()) {
            val del = folder.deleteRecursively()
            LOGGER.info("Delete image selection folder: {}", del)
        }
    }

    class AddDetailedTransactionFragmentStateAdapter(addDetailedTransactionActivity: AddDetailedTransactionActivity): FragmentStateAdapter(addDetailedTransactionActivity) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            if(position == 0) {
                return AddDetailedTransactionMainFragment.newInstance()
            } else {
                return AddDetailedTransactionOtherDetailsFragment.newInstance()
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionActivity::class.java)
        const val ARG_INITIAL_ACCOUNT_ID = "initialAccount"
        const val ARG_INITIAL_AMOUNT = "initialAmount"
        const val ARG_INITIAL_DESCRIPTION = "initialDescription"
        const val ARG_INITIAL_CATEGORY_ID = "initialCategoryId"
        const val ARG_MODE = "mode"
        const val ARG_EDIT_TRANSACTION_ID = "editTransactionId"
        const val REQUEST_CODE_ITEM_OPEN_IMAGE = 100
        const val REQUEST_CODE_OPEN_DOCUMENT = 101
        const val REQUEST_CODE_ITEM_CAPTURE_IMAGE = 102
        const val REQUEST_CODE_DOCUMENT_CAPTURE_IMAGE = 103
        const val DIALOG_TAG_NO_PAGES = "noPages"
        const val DIALOG_TAG_PASSWORD_PROTECTED = "passwordProtected"
        const val DIALOG_TAG_ADD_IMAGE = "addImage"

        fun createBundle(
            initialAccountId: Long?,
            initialAmount: String?,
            initialDescription: String?,
            initialCategoryId: Long?
        ): Bundle {
            return bundleOf(
                ARG_INITIAL_ACCOUNT_ID to initialAccountId,
                ARG_INITIAL_AMOUNT to initialAmount,
                ARG_INITIAL_DESCRIPTION to initialDescription,
                ARG_INITIAL_CATEGORY_ID to initialCategoryId
            )
        }
    }
}