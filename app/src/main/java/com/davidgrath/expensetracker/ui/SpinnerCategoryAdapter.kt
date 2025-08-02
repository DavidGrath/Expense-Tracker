package com.davidgrath.expensetracker.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.CategoryUi

class SpinnerCategoryAdapter(context: Context, val resourceId: Int, val objects: Array<CategoryUi>): ArrayAdapter<CategoryUi>(context, resourceId, R.id.text_view_spinner_item_category, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(parent.context)
        val category = objects[position]
        val view = inflater.inflate(resourceId, parent, false)
        view.findViewById<ImageView>(R.id.image_view_spinner_item_category).setImageResource(category.iconId)
        view.findViewById<TextView>(R.id.text_view_spinner_item_category).text = category.name
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(parent.context)
        val category = objects[position]
        val view = inflater.inflate(resourceId, parent, false)
        view.findViewById<ImageView>(R.id.image_view_spinner_item_category).setImageResource(category.iconId)
        view.findViewById<TextView>(R.id.text_view_spinner_item_category).text = category.name
        return view
    }
}