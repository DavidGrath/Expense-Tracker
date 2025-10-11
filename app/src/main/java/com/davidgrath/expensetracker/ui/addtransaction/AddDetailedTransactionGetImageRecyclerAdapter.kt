package com.davidgrath.expensetracker.ui.addtransaction

import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.db.ImageDb

class AddDetailedTransactionGetImageRecyclerAdapter(private var _images: List<ImageDb>, var listener: OnImageClickListener? = null): RecyclerView.Adapter<AddDetailedTransactionGetImageRecyclerAdapter.AddDetailedTransactionGetImageViewHolder>() {

    interface OnImageClickListener {
        fun onImageClicked(uri: Uri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddDetailedTransactionGetImageViewHolder {
//        val height = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_evidence_height)
        val height = parent.width / 3
        val width = parent.width / 3
        val layoutParams = ViewGroup.LayoutParams(width, height)
        val imageView = ImageView(parent.context)
        imageView.layoutParams = layoutParams
        return AddDetailedTransactionGetImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: AddDetailedTransactionGetImageViewHolder, position: Int) {
        holder.imageView.setOnClickListener {
            val pos = holder.absoluteAdapterPosition
            val image = _images[pos]
            val uri = Uri.parse(image.uri)
            listener?.onImageClicked(uri)
        }
        val image = _images[position]
        val uri = Uri.parse(image.uri)
        Glide.with(holder.imageView.context)
            .load(uri)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return _images.size
    }

    fun setItems(items: List<ImageDb>) {
        this._images = items
        notifyDataSetChanged()
    }

    class AddDetailedTransactionGetImageViewHolder(val imageView: ImageView): ViewHolder(imageView)
}