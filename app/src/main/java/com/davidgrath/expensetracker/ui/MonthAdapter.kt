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
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Month
import org.threeten.bp.format.TextStyle
import java.util.Locale

class MonthAdapter(context: Context, val objects: Array<Month>, val timeAndLocaleHandler: TimeAndLocaleHandler): ArrayAdapter<Month>(context, android.R.layout.simple_spinner_item, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val month = objects[position]
        val displayName = month.getDisplayName(TextStyle.FULL, timeAndLocaleHandler.getLocale())
        textView.text = displayName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val month = objects[position]
        val displayName = month.getDisplayName(TextStyle.FULL, timeAndLocaleHandler.getLocale())
        textView.text = displayName
        return view
    }
}