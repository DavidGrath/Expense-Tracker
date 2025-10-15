package com.davidgrath.expensetracker.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig

class SpinnerStatisticModeAdapter(var currentXDays: Int, context: Context, val objects: Array<StatisticsConfig.DateMode>): ArrayAdapter<StatisticsConfig.DateMode>(context, android.R.layout.simple_spinner_item, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val context = LayoutInflater.from(parent.context)
        val mode = objects[position]
        val modeText = modeToText(mode)
        view.text = modeText
        return view
    }

    fun modeToText(dateMode: StatisticsConfig.DateMode): String {
        //TODO Context and string ids
        return when(dateMode) {
            StatisticsConfig.DateMode.Daily -> {
                "Daily"
            }
            StatisticsConfig.DateMode.PastXDays -> {
                if(currentXDays == 1) {
                    "Past $currentXDays day"
                } else {
                    "Past $currentXDays days" //TODO Pluralization
                }
            }
            StatisticsConfig.DateMode.PastWeek -> {
                "Past week"
            }
            StatisticsConfig.DateMode.Weekly -> {
                "Weekly"
            }
            StatisticsConfig.DateMode.PastMonth -> {
                "Past month"
            }
            StatisticsConfig.DateMode.Monthly -> {
                "Monthly"
            }
            StatisticsConfig.DateMode.PastYear -> {
                "Past year"
            }
            StatisticsConfig.DateMode.Yearly -> {
                "Yearly"
            }
            StatisticsConfig.DateMode.Range -> {
                "Range"
            }
            StatisticsConfig.DateMode.All -> {
                "All"
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