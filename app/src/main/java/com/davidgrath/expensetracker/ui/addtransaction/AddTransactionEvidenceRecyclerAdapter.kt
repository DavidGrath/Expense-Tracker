package com.davidgrath.expensetracker.ui.addtransaction

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R

class AddTransactionEvidenceRecyclerAdapter(private var uris: List<String>): RecyclerView.Adapter<AddTransactionEvidenceRecyclerAdapter.AddTransactionEvidenceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionEvidenceViewHolder {
        val size = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_item_image_size)
        val layoutParams = ViewGroup.LayoutParams(size, size)
        val imageView = ImageView(parent.context)
        imageView.layoutParams = layoutParams
        return AddTransactionEvidenceViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: AddTransactionEvidenceViewHolder, position: Int) {
        val image = holder.imageView
        val uri = uris[position]
        Glide.with(image.context)
            .load(Uri.parse(uri))
            .centerCrop()
            .into(image)
    }

    override fun getItemCount(): Int {
        return uris.size
    }

    fun setItems(uris: List<String>) {
        this.uris = uris
        notifyDataSetChanged()
    }

    class AddTransactionEvidenceViewHolder(val imageView: ImageView): ViewHolder(imageView) {

    }
}