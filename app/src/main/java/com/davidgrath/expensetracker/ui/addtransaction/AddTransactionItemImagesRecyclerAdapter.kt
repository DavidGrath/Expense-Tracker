package com.davidgrath.expensetracker.ui.addtransaction

import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import org.slf4j.LoggerFactory

class AddTransactionItemImagesRecyclerAdapter(private var files: List<AddEditTransactionFile>): RecyclerView.Adapter<AddTransactionItemImagesRecyclerAdapter.AddTransactionItemImagesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionItemImagesViewHolder {
        val size = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_item_image_size)
        val layoutParams = ViewGroup.LayoutParams(size, size)
        val imageView = ImageView(parent.context)
        imageView.layoutParams = layoutParams
        return AddTransactionItemImagesViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: AddTransactionItemImagesViewHolder, position: Int) {
        val image = holder.imageView
        val file = files[position]
        Glide.with(image.context)
            .load(file.uri)
            .centerCrop()
            .into(image)
    }

    override fun getItemCount(): Int {
        return files.size
    }

    fun setItems(uris: List<AddEditTransactionFile>) {
        LOGGER.info("setItems: List size {}", uris.size)
        this.files = uris
        notifyDataSetChanged()
    }

    class AddTransactionItemImagesViewHolder(val imageView: ImageView): ViewHolder(imageView) {

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddTransactionItemImagesRecyclerAdapter::class.java)
    }
}