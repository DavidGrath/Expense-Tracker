package com.davidgrath.expensetracker

import android.graphics.Color

data class MaterialColors(val value: Int) {
    companion object {
        val Red600 = MaterialColors(0xFFE53935.toInt())
        val Pink600 = MaterialColors(0xFFD81B60.toInt())
        val Purple600 = MaterialColors(0xFF8E24AA.toInt())
        val DeepPurple600 = MaterialColors(0xFF5E35B1.toInt())
        val Indigo600 = MaterialColors(0xFF3949AB.toInt())
        val Blue600 = MaterialColors(0xFF1E88E5.toInt())
        val LightBlue600 = MaterialColors(0xFF039BE5.toInt())
        val Cyan600 = MaterialColors(0xFF00ACC1.toInt())
        val Teal600 = MaterialColors(0xFF00897B.toInt())
        val Green600 = MaterialColors(0xFF43A047.toInt())
        val LightGreen600 = MaterialColors(0xFF7CB342.toInt())
        val Lime600 = MaterialColors(0xFFC0CA33.toInt())
        val Yellow600 = MaterialColors(0xFFFDD835.toInt())
        val Amber600 = MaterialColors(0xFFFFB300.toInt())
        val Orange600 = MaterialColors(0xFFFB8C00.toInt())
        val DeepOrange600 = MaterialColors(0xFFF4511E.toInt())
        val Brown600 = MaterialColors(0xFF6D4C41.toInt())
        val Gray600 = MaterialColors(0xFF757575.toInt())
        val BlueGray600 = MaterialColors(0xFF546E7A.toInt())
        val Palette = listOf(Red600, Pink600, Purple600, DeepPurple600, Indigo600, Blue600, LightBlue600, Cyan600, Teal600, Green600, LightGreen600, Lime600, Yellow600, Amber600, Orange600, DeepOrange600, Brown600, Gray600, BlueGray600)
    }
}