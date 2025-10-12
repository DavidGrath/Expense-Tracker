package com.davidgrath.expensetracker.ui.addtransaction

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.categoryDbToCategoryUi
import com.davidgrath.expensetracker.databinding.FragmentAddDetailedTransactionMainBinding
import com.davidgrath.expensetracker.entities.ui.AddTransactionItem
import com.davidgrath.expensetracker.repositories.AddDetailedTransactionRepository
import com.davidgrath.expensetracker.ui.dialogs.AddExternalMediaDialogFragment
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale

class AddDetailedTransactionMainFragment: Fragment(), AddTransactionItemRecyclerAdapter.AddTransactionItemRecyclerListener, OnClickListener, AddExternalMediaDialogFragment.ExternalMediaListener {


    interface AddDetailedTransactionMainListener {
        fun onFinished()
    }

    private var listener: AddDetailedTransactionMainListener? = null
    lateinit var binding: FragmentAddDetailedTransactionMainBinding
    lateinit var viewModel: AddDetailedTransactionViewModel
    lateinit var adapter: AddTransactionItemRecyclerAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is AddDetailedTransactionMainListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAddDetailedTransactionMainBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider.create(requireActivity()).get(AddDetailedTransactionViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categories = viewModel.getCategories()
            .subscribeOn(Schedulers.io())
            .blockingGet()
        val c = categories.map { categoryDbToCategoryUi(it) }
        adapter = AddTransactionItemRecyclerAdapter(c, this)
        binding.recyclerviewAddDetailedTransactionMain.adapter = adapter
        binding.recyclerviewAddDetailedTransactionMain.layoutManager = LinearLayoutManager(requireContext())
        binding.imageViewAddDetailedTransactionDebitOrCredit.setOnClickListener(this)
        viewModel.transactionItemsLiveData.observe(viewLifecycleOwner) { triple ->
            LOGGER.info("Event: ${triple.second}, Position: ${triple.third}")
            val draft = triple.first
            val list = draft.items
            val event = triple.second
            val position = triple.third
            when(event) {
                AddDetailedTransactionRepository.TransactionItemsEvent.Delete -> {
                    adapter.setItems(list)
                    adapter.notifyItemRemoved(position)
                }
                AddDetailedTransactionRepository.TransactionItemsEvent.Insert -> {
                    adapter.setItems(list)
                    adapter.notifyItemInserted(position)
                }
                AddDetailedTransactionRepository.TransactionItemsEvent.All -> {
                    adapter.setItems(list)
                    adapter.notifyDataSetChanged()
                }
                AddDetailedTransactionRepository.TransactionItemsEvent.Change -> {
                    adapter.setItems(list)
                    //No change to prevent EditText focus loss
                }
                AddDetailedTransactionRepository.TransactionItemsEvent.ChangeInvalidate -> {
                    adapter.setItems(list)
                    adapter.notifyItemChanged(position)
                }
                AddDetailedTransactionRepository.TransactionItemsEvent.None -> {

                }
            }

            if(draft.debitOrCredit) {
                binding.imageViewAddDetailedTransactionDebitOrCredit.setImageResource(R.drawable.baseline_remove_24)
            } else {
                binding.imageViewAddDetailedTransactionDebitOrCredit.setImageResource(R.drawable.baseline_add_24)
            }
        }

        viewModel.currentAccount.observe(viewLifecycleOwner) { (accounts, account, total) ->
            val currencyCode = account.currencyCode
            binding.textViewAddDetailedTransactionMainTotal.text = currencyCode + " " + String.format(Locale.getDefault(), "%.2f", total) //TODO General number formatting, UCUM/UOM
        }
        binding.linearLayoutAddDetailedTransactionMainAddItem.setOnClickListener(this)

        binding.imageButtonAddDetailedTransactionDone.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        v?.let {
            when(v) {
                binding.linearLayoutAddDetailedTransactionMainAddItem -> {
                    viewModel.addItem()
                }
                binding.imageButtonAddDetailedTransactionDone -> {
                    if(viewModel.validateDraft()) {
                        viewModel.finishDraft()
                            .observe(viewLifecycleOwner) {
                                LOGGER.info("Transaction Added. Draft discarded")
                                listener?.onFinished() //TODO SimpleResult
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

    override fun onItemChanged(position: Int, item: AddTransactionItem) {
        viewModel.onItemChanged(position, item)
    }

    override fun onItemChangedInvalidate(position: Int, item: AddTransactionItem) {
        viewModel.onItemChangedInvalidate(position, item)
    }

    override fun onDeleteItem(position: Int) {
        viewModel.onItemDeleted(position)
    }

    override fun onDeleteItemImage(position: Int, imagePosition: Int) {
        viewModel.onItemImageDeleted(position, imagePosition)
    }

    override fun onRequestAddImage(position: Int, itemId: Int) {
        val atLeastOneImageExists = viewModel.getImageCount().blockingGet() > 0
        val externalMediaDialog = AddExternalMediaDialogFragment.newInstance(true, itemId, atLeastOneImageExists)
        externalMediaDialog.listener = this
        externalMediaDialog.show(childFragmentManager, DIALOG_TAG_EXTERNAL_MEDIA_PICKER)
        LOGGER.info("Opened dialog externalMediaPicker")
    }

    override fun onSelectionMade(selection: AddExternalMediaDialogFragment.Selection, itemOrEvidence: Boolean, itemId: Int?) {
        when(selection) {
            AddExternalMediaDialogFragment.Selection.Camera -> {
                viewModel.getImageItemId = itemId!!
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val cameraDirectory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val cameraFile = File(cameraDirectory, Constants.FILE_NAME_INTENT_PICTURE)
                if(cameraFile.exists()) {
                    LOGGER.info("Camera Intent file already exists")
                    val delete = cameraFile.delete()
                    LOGGER.info("Delete existing camera file: $delete")
                }
                val uri = FileProvider.getUriForFile(requireContext().applicationContext, requireContext().applicationContext.packageName + ".provider", cameraFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                try {
                    requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_ITEM_CAPTURE_IMAGE)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "Unable to open camera", Toast.LENGTH_SHORT).show()
                    LOGGER.error("Unable to open camera", e)
                }
            }
            AddExternalMediaDialogFragment.Selection.LocalImage -> {
                viewModel.getImageItemId = itemId!!
                val intent = Intent(requireActivity(), AddDetailedTransactionGetImageActivity::class.java)
                requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_ITEM_OPEN_IMAGE)
            }
            AddExternalMediaDialogFragment.Selection.DevicePicker -> {
                viewModel.getImageItemId = itemId!!
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT) //TODO Rework Intents - possibly change to ACTION_GET_CONTENT
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                requireActivity().startActivityForResult(intent, AddDetailedTransactionActivity.REQUEST_CODE_ITEM_OPEN_IMAGE)
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(): AddDetailedTransactionMainFragment {
            val fragment = AddDetailedTransactionMainFragment()
            return fragment
        }
        private const val DIALOG_TAG_EXTERNAL_MEDIA_PICKER = "externalMediaPicker"
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionMainFragment::class.java)
    }
}