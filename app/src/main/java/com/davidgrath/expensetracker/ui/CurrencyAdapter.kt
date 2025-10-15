package com.davidgrath.expensetracker.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import java.util.Currency
import java.util.Locale

class CurrencyAdapter(context: Context, val objects: Array<Currency>, val timeAndLocaleHandler: TimeAndLocaleHandler): ArrayAdapter<Currency>(context, android.R.layout.simple_spinner_item, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val currency = objects[position]
        val displayName = currency.getDisplayName(timeAndLocaleHandler.getLocale())
        textView.text = "$currency - $displayName"
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val currency = objects[position]
        val displayName = currency.getDisplayName(timeAndLocaleHandler.getLocale())
        textView.text = "$currency - $displayName"
        return view
    }
}