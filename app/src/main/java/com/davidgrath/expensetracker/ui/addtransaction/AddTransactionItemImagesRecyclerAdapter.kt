package com.davidgrath.expensetracker.ui.addtransaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.databinding.RecyclerviewAddEditItemImageBinding
import com.davidgrath.expensetracker.entities.ui.AddEditTransactionFile
import org.slf4j.LoggerFactory

class AddTransactionItemImagesRecyclerAdapter(private var files: List<AddEditTransactionFile>, var listener: ItemImageClickListener? = null): RecyclerView.Adapter<AddTransactionItemImagesRecyclerAdapter.AddTransactionItemImagesViewHolder>() {

    private var selectedItemPosition = -1
    interface ItemImageClickListener {
        fun onDeleteImage(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTransactionItemImagesViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewAddEditItemImageBinding.inflate(inflater, parent, false)
        val viewHolder = AddTransactionItemImagesViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: AddTransactionItemImagesViewHolder, position: Int) {
        val image = holder.binding.imageViewItemImageMain
        val file = files[position]
        Glide.with(image.context)
            .load(file.uri)
            .centerCrop()
            .into(image)
        holder.binding.root.setOnClickListener {
            val pos = holder.absoluteAdapterPosition
            if(selectedItemPosition == pos) {
                listener?.onDeleteImage(pos)
            } else {
                val oldSelection = selectedItemPosition
                selectedItemPosition = pos
                notifyItemChanged(pos)
                if(oldSelection != -1) {
                    notifyItemChanged(oldSelection)
                }
            }
        }
        if(selectedItemPosition == position) {
            holder.binding.imageViewItemImageSelectedIndicator.visibility = View.VISIBLE
        } else {
            holder.binding.imageViewItemImageSelectedIndicator.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    fun setItems(uris: List<AddEditTransactionFile>) {
        LOGGER.info("setItems: List size {}", uris.size)
        this.files = uris
        selectedItemPosition = -1
        notifyDataSetChanged()
    }

    class AddTransactionItemImagesViewHolder(val binding: RecyclerviewAddEditItemImageBinding): ViewHolder(binding.root) {

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddTransactionItemImagesRecyclerAdapter::class.java)
    }
}