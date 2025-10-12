package com.davidgrath.expensetracker.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig

class SpinnerStatisticModeAdapter(var currentXDays: Int, context: Context, val objects: Array<StatisticsConfig.Mode>): ArrayAdapter<StatisticsConfig.Mode>(context, android.R.layout.simple_spinner_item, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val context = LayoutInflater.from(parent.context)
        val mode = objects[position]
        val modeText = modeToText(mode)
        view.text = modeText
        return view
    }

    fun modeToText(mode: StatisticsConfig.Mode): String {
        //TODO Context and string ids
        return when(mode) {
            StatisticsConfig.Mode.Daily -> {
                "Daily"
            }
            StatisticsConfig.Mode.PastXDays -> {
                if(currentXDays == 1) {
                    "Past $currentXDays day"
                } else {
                    "Past $currentXDays days"
                }
            }
            StatisticsConfig.Mode.PastWeek -> {
                "Past week"
            }
            StatisticsConfig.Mode.Weekly -> {
                "Weekly"
            }
            StatisticsConfig.Mode.PastMonth -> {
                "Past month"
            }
            StatisticsConfig.Mode.Monthly -> {
                "Monthly"
            }
            StatisticsConfig.Mode.PastYear -> {
                "Past year"
            }
            StatisticsConfig.Mode.Yearly -> {
                "Yearly"
            }
            StatisticsConfig.Mode.Range -> {
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