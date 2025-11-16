package com.davidgrath.expensetracker.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.entities.ui.SellerLocationUi
import com.davidgrath.expensetracker.entities.ui.SellerUi
import java.util.Currency
import java.util.Locale

/**
 * The first item always has an id of -1
 */
class SellerLocationAdapter(context: Context, var _objects: MutableList<SellerLocationUi>): ArrayAdapter<SellerLocationUi>(context, android.R.layout.simple_spinner_item, _objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        if(position == 0) {
            textView.text = ""
        } else {
            val sellerLocation = _objects[position]
            textView.text = sellerLocation.location
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        if(position == 0) {
            textView.text = ""
        } else {
            val sellerLocation = _objects[position]
            textView.text = sellerLocation.location
        }
        return view
    }

    fun setItems(sellerLocations: List<SellerLocationUi>) {
        this._objects.clear()
        this._objects.addAll(sellerLocations)
        notifyDataSetChanged()
    }
}