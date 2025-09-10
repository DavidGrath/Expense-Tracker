package com.davidgrath.expensetracker.ui.transactiondetails

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.ImageUi

class TransactionDetailsItemImagesRecyclerAdapter(private var images: List<ImageUi>): RecyclerView.Adapter<TransactionDetailsItemImagesRecyclerAdapter.TransactionDetailsItemImagesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionDetailsItemImagesViewHolder {
        val size = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_item_image_size)
        val layoutParams = ViewGroup.LayoutParams(size, size)
        val imageView = ImageView(parent.context)
        imageView.layoutParams = layoutParams
        return TransactionDetailsItemImagesViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: TransactionDetailsItemImagesViewHolder, position: Int) {
        val image = holder.imageView
        val imageUi = images[position]
        Glide.with(image.context)
            .load(imageUi.uri)
            .centerCrop()
            .into(image)
        image.setOnClickListener {
            //TODO Shared element transition
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun setItems(images: List<ImageUi>) {
        this.images = images
        notifyDataSetChanged()
    }

    class TransactionDetailsItemImagesViewHolder(val imageView: ImageView): ViewHolder(imageView) {

    }
}