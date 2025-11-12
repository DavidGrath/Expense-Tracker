package com.davidgrath.expensetracker.ui.main.images

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.davidgrath.expensetracker.databinding.FragmentImageStatsBinding
import com.davidgrath.expensetracker.databinding.RecyclerviewImageStatsBinding
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.views.ImageWithStats
import com.davidgrath.expensetracker.entities.ui.TransactionDetailItemUi
import com.davidgrath.expensetracker.formatBytes
import com.davidgrath.expensetracker.ui.addtransaction.AddDetailedTransactionGetImageRecyclerAdapter
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.Measure
import com.ibm.icu.util.MeasureUnit
import com.ibm.icu.util.MeasureUnit.BIT
import systems.uom.quantity.Information
import tech.units.indriya.quantity.Quantities
import tech.units.indriya.unit.Units
import javax.measure.Quantity

class ImageStatsRecyclerAdapter(private var items: List<ImageWithStats>, val timeAndLocaleHandler: TimeAndLocaleHandler): RecyclerView.Adapter<ImageStatsRecyclerAdapter.ImageStatsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageStatsViewHolder {
        val height = parent.width / 3
        val width = parent.width / 3
        val layoutParams = LinearLayout.LayoutParams(width, height)
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerviewImageStatsBinding.inflate(inflater, parent, false)
        val imageView = binding.imageViewImageStatsImage
        imageView.layoutParams = layoutParams
        val bindingLayoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.root.layoutParams = bindingLayoutParams
        return ImageStatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageStatsViewHolder, position: Int) {
        val binding = holder.binding
        val stat = items[position]
        val uri = Uri.parse(stat.uri)
        Glide.with(binding.imageViewImageStatsImage.context)
            .load(uri)
            .centerCrop()
            .into(binding.imageViewImageStatsImage)
        binding.imageViewImageStatsImage.setOnClickListener {
            //TODO Shared element transition
        }
        binding.textViewImageStatsTransactionCount.text = "${stat.transactionCount} transactions" //TODO Pluralization
        binding.textViewImageStatsItemCount.text = "${stat.itemCount} items" //TODO Pluralization
        binding.textViewImageStatsSizeBytes.text = stat.sizeBytes.formatBytes(timeAndLocaleHandler.getLocale())
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: List<ImageWithStats>) {
        this.items = items
        notifyDataSetChanged()
    }

    class ImageStatsViewHolder(val binding: RecyclerviewImageStatsBinding): RecyclerView.ViewHolder(binding.root)
}