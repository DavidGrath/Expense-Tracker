package com.davidgrath.expensetracker.ui.addtransaction

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.databinding.ActivityAddDetailedTransactionGetImageBinding
import com.davidgrath.expensetracker.repositories.ProfileRepository
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AddDetailedTransactionGetImageActivity: AppCompatActivity(), AddDetailedTransactionGetImageRecyclerAdapter.OnImageClickListener {

    private lateinit var binding: ActivityAddDetailedTransactionGetImageBinding
    private lateinit var viewModel: AddDetailedTransactionGetImageViewModel
    @Inject
    lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailedTransactionGetImageBinding.inflate(layoutInflater)
        val app = application as ExpenseTracker
        app.appComponent.inject(this)
        viewModel = ViewModelProvider(this ,AddDetailedTransactionGetImageViewModelFactory(app.appComponent)).get(AddDetailedTransactionGetImageViewModel::class.java)
        val adapter = AddDetailedTransactionGetImageRecyclerAdapter(emptyList(), this)
        val layoutManager = GridLayoutManager(this, 3)
        binding.recyclerViewAddDetailedTransactionGetImage.adapter = adapter
        binding.recyclerViewAddDetailedTransactionGetImage.layoutManager = layoutManager

        val preferences = application.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE) //TODO Create profile Observable in Application
        val currentProfileStringId = preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null)!!
        val profile = profileRepository.getByStringId(currentProfileStringId).subscribeOn(Schedulers.io()).blockingGet()

        viewModel.getImages(profile.id!!).observe(this) {
            adapter.setItems(it)
        }
        setContentView(binding.root)
    }

    override fun onImageClicked(uri: Uri) {
        LOGGER.info("Image selected. Finishing")
        setResult(RESULT_OK, Intent().setData(uri))
        finish()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddDetailedTransactionGetImageActivity::class.java)
    }
}