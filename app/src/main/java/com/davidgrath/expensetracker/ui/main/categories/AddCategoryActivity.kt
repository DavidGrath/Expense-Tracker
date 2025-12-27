package com.davidgrath.expensetracker.ui.main.categories

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.ActivityAddCategoryBinding
import com.davidgrath.expensetracker.getMaterialResourceId
import com.davidgrath.expensetracker.loadMaterialSymbolsIcons
import com.davidgrath.expensetracker.ui.dialogs.EditAccountDialogFragment
import com.davidgrath.expensetracker.ui.main.MaterialIconAdapter
import com.davidgrath.expensetracker.utils.MaxCodePointWatcher
import org.slf4j.LoggerFactory

class AddCategoryActivity: AppCompatActivity(), OnClickListener, MaterialIconAdapter.MaterialIconClickListener {

    lateinit var binding: ActivityAddCategoryBinding
    lateinit var viewModel: AddCategoryViewModel
    lateinit var adapter: MaterialIconAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        val icons = loadMaterialSymbolsIcons(this).blockingGet() //TODO Progress indicator
        viewModel = ViewModelProvider(this, AddCategoryViewModelFactory(icons)).get(AddCategoryViewModel::class.java)

        binding.textViewAddCategoryNameIndicator.text = "0/" + Constants.MAX_CODEPOINT_LENGTH_SHORT
        val textWatcher = MaxCodePointWatcher(binding.editTextAddCategory, Constants.MAX_CODEPOINT_LENGTH_SHORT, binding.textViewAddCategoryNameIndicator) { text ->
            viewModel.category = text
        }
        binding.editTextAddCategory.addTextChangedListener(textWatcher)

        val searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.search(s!!.toString())
            }
        }
        binding.editTextAddCategoryIconSearch.addTextChangedListener(searchTextWatcher)

        adapter = MaterialIconAdapter(emptyList(), this)

        viewModel.iconsLiveData.observe(this) { list ->
            adapter.setIcons(list)
        }
        viewModel.iconNameLiveData.observe(this) { name ->
            val imageResource = getMaterialResourceId(this, "materialsymbols:$name")
            binding.imageViewAddCategoryIcon.setImageResource(imageResource)
        }
        val layoutManager = GridLayoutManager(this, 6)
        binding.recyclerviewAddCategory.adapter = adapter
        binding.recyclerviewAddCategory.layoutManager = layoutManager
        binding.fabAddCategoryDone.setOnClickListener(this)
        setContentView(binding.root)
    }

    override fun onClick(v: View?) {
        v?.let { view ->
            when(view) {
                binding.fabAddCategoryDone -> {
                    if(!viewModel.category.isNullOrBlank()) {
                        val ret = Intent().also {
                            it.putExtra(EXTRA_FINISH_CATEGORY_NAME, viewModel.category)
                            it.putExtra(EXTRA_FINISH_ICON, viewModel.iconNameLiveData.value!!)
                        }
                        setResult(RESULT_OK, ret)
                        finish()
                    }
                }
            }
        }
    }

    override fun onIconClicked(name: String) {
        viewModel.setIconName(name)
    }

    companion object {
        const val EXTRA_FINISH_CATEGORY_NAME = "name"
        const val EXTRA_FINISH_ICON = "icon"

        private val LOGGER = LoggerFactory.getLogger(AddCategoryActivity::class.java)
    }
}