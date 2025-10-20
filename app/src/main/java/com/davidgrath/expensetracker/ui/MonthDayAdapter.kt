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
import java.util.Currency
import java.util.Locale

class MonthDayAdapter(context: Context, var _objects: MutableList<Int>): ArrayAdapter<Int>(context, android.R.layout.simple_spinner_item, _objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val day = _objects[position]
        textView.text = day.toString()
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val day = _objects[position]
        textView.text = day.toString()
        return view
    }

    fun setItems(accounts: List<Int>) {
        this._objects.clear()
        this._objects.addAll(accounts)
        notifyDataSetChanged()
    }
}