package com.davidgrath.expensetracker.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.davidgrath.expensetracker.formatDecimal
import com.davidgrath.expensetracker.parseDecimal
import com.ibm.icu.text.DecimalFormatSymbols
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import java.util.Locale

class NumberFormatTextWatcher(val editText: EditText, val maxAmount: BigDecimal, val locale: Locale, val num: (n: BigDecimal?) -> Unit):
    TextWatcher {
    private val formatSymbols = DecimalFormatSymbols.getInstance(locale)
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {
        val decimal = formatSymbols.decimalSeparatorString
        val grouping = formatSymbols.groupingSeparatorString
        var originalEndsWithDecimal = false
        var countOfConsecutiveZeroesAfterDecimal = 0
        var amount = try {
            var string = s.toString()
            val indexOfDecimal = string.indexOf(decimal)
            if(string.length > 1 && indexOfDecimal == string.length - 1) {
                originalEndsWithDecimal = true
                string = string.substring(0, string.length - 1)
            } else if(string.length > 1 && indexOfDecimal >= 0) {
                val fractionalSubstring = string.substring(indexOfDecimal + 1, string.length)
                LOGGER.debug("fractionalSubstring: {}", fractionalSubstring)
                if(fractionalSubstring.matches(Regex("0+"))) {
                    countOfConsecutiveZeroesAfterDecimal = fractionalSubstring.length.coerceAtMost(2)
                    LOGGER.debug("consecutive count: {}", countOfConsecutiveZeroesAfterDecimal)
                }
            }
            parseDecimal(string, locale)
        } catch (e: NumberFormatException) {
            null
        } catch (e: ParseException) {
            null
        }
        if(amount != null) {
            if(amount.compareTo(maxAmount) > 0) {
                amount = maxAmount
            }
        } else {
            amount = BigDecimal.ZERO
        }
        val originalSelectionStart = editText.selectionStart
        val originalSelectionEnd = editText.selectionEnd
        editText.removeTextChangedListener(this)
        val formatted = formatDecimal(amount!!, locale) + if(originalEndsWithDecimal) {
            decimal
        } else {
            ""
        } + if(countOfConsecutiveZeroesAfterDecimal > 0) {
            decimal + "0".repeat(countOfConsecutiveZeroesAfterDecimal)
        } else {
            ""
        }
        editText.setText(formatted)
        if(originalSelectionStart == originalSelectionEnd && originalSelectionStart >= 0) {
//            val plainString = amount!!.toPlainString()
//            val groupingCountsBeforeOriginalStringPosition = s?.toString()?.substring(0, originalSelectionStart)?.count { it == grouping.toCharArray()[0] }?:0
            val originalPosition = originalSelectionStart
            if(originalPosition == (s?.toString()?.length ?: -1)) {
                editText.setSelection(formatted.length)
            } else {
                if(formatted.length > (s?.toString()?.length ?: 0) && originalSelectionStart + 1 <= formatted.length) {
                    editText.setSelection(originalSelectionStart + 1)
                } else if (formatted.length < (s?.toString()?.length ?: 0) && originalSelectionStart - 1 >= 0){
                    editText.setSelection(originalSelectionStart - 1)
                } else {
                    editText.setSelection(originalSelectionStart)
                }
            }
        }
        num.invoke(amount.setScale(2, RoundingMode.DOWN))
        editText.addTextChangedListener(this)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NumberFormatTextWatcher::class.java)
    }
}