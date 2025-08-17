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
import com.davidgrath.expensetracker.entities.ui.TempStatisticsConfig

class SpinnerStatisticModeAdapter(var currentXDays: Int, context: Context, val objects: Array<TempStatisticsConfig.Mode>): ArrayAdapter<TempStatisticsConfig.Mode>(context, android.R.layout.simple_spinner_item, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val context = LayoutInflater.from(parent.context)
        val mode = objects[position]
        val modeText = modeToText(mode)
        view.text = modeText
        return view
    }

    fun modeToText(mode: TempStatisticsConfig.Mode): String {
        //TODO Context and string ids
        return when(mode) {
            TempStatisticsConfig.Mode.Daily -> {
                "Daily"
            }
            TempStatisticsConfig.Mode.PastXDays -> {
                if(currentXDays == 1) {
                    "Past $currentXDays day"
                } else {
                    "Past $currentXDays days"
                }
            }
            TempStatisticsConfig.Mode.PastWeek -> {
                "Past week"
            }
            TempStatisticsConfig.Mode.Weekly -> {
                "Weekly"
            }
            TempStatisticsConfig.Mode.PastMonth -> {
                "Past month"
            }
            TempStatisticsConfig.Mode.Monthly -> {
                "Monthly"
            }
            TempStatisticsConfig.Mode.PastYear -> {
                "Past year"
            }
            TempStatisticsConfig.Mode.Yearly -> {
                "Yearly"
            }
            TempStatisticsConfig.Mode.Range -> {
                "Range"
            }
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val context = LayoutInflater.from(parent.context)
        val mode = objects[position]
        val modeText = modeToText(mode)
        view.text = modeText
        return view
    }
}