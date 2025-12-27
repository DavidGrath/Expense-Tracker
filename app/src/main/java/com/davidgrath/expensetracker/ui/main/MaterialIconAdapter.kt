package com.davidgrath.expensetracker.ui.main

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.davidgrath.expensetracker.MaterialMetadata
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.databinding.RecyclerviewAddCategoryIconBinding
import com.davidgrath.expensetracker.getMaterialResourceId
import org.slf4j.LoggerFactory

class MaterialIconAdapter(
    private var symbols: List<String>,
    var listener: MaterialIconClickListener? = null
) : RecyclerView.Adapter<MaterialIconAdapter.MaterialIconViewHolder>() {

    interface MaterialIconClickListener {
        fun onIconClicked(name: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialIconViewHolder {
        val inflater = LayoutInflater.from(parent.context)
//        val height = parent.context.resources.getDimensionPixelSize(R.dimen.add_transaction_evidence_height)
        val height = parent.width / 6
        val width = parent.width / 6
        val layoutParams = ViewGroup.LayoutParams(width, height)
        val binding = RecyclerviewAddCategoryIconBinding.inflate(inflater, parent, false)
        val imageView = binding.root
//        imageView.layoutParams = layoutParams
        return MaterialIconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialIconViewHolder, position: Int) {

        val pos = holder.absoluteAdapterPosition
        val symbol = symbols[pos]
        val imageView = holder.binding.root
        val imageResource = getMaterialResourceId(imageView.context, "materialsymbols:$symbol")
        if (imageResource == 0) {
            LOGGER.warn("Symbol {} has no resource", symbol)
            imageView.setImageResource(R.drawable.baseline_category_24)
        } else {
            val drawable = ResourcesCompat.getDrawable(imageView.context.resources, imageResource, null)
//            drawable!!.setTint(Color.BLACK)
            imageView.setImageDrawable(drawable)
        }
        imageView.setOnClickListener {
            listener?.onIconClicked(symbol)
        }
    }

    override fun getItemCount(): Int {
        return symbols.size
    }

    fun setIcons(icons: List<String>) {
        this.symbols = icons
        notifyDataSetChanged()
    }

    class MaterialIconViewHolder(val binding: RecyclerviewAddCategoryIconBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MaterialIconAdapter::class.java)
    }
}