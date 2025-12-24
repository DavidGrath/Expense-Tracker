package com.davidgrath.expensetracker.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import com.ibm.icu.text.BreakIterator
import org.slf4j.LoggerFactory

class MaxCodePointWatcher(val editText: EditText, val maxLength: Int, val indicator: TextView?, val textChanged: (s: String) -> Unit):
    TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {
        val text = s!!.toString()
        val length = text.length
        val codePointCount = text.codePointCount(0, length)
        indicator?.text = codePointCount.toString() + "/" + maxLength
        if(codePointCount > maxLength) {
            LOGGER.info("afterTextChanged: Reached max code point count")
            val breakIterator = BreakIterator.getCharacterInstance()
            breakIterator.setText(text)
            val lastGraphemePosition = if(breakIterator.isBoundary(text.offsetByCodePoints(0, maxLength))) {
                text.offsetByCodePoints(0, maxLength)
            } else {
                breakIterator.preceding(text.offsetByCodePoints(0, maxLength))
            }
            if(lastGraphemePosition != BreakIterator.DONE) {
                editText.removeTextChangedListener(this)
                val substring = text.substring(0, lastGraphemePosition)
                val trimmed = substring.trim()
                editText.setText(
                    trimmed
                )
                textChanged.invoke(trimmed)
                indicator?.text = substring.codePointCount(0, trimmed.length).toString() + "/" + maxLength
                editText.setSelection(if(lastGraphemePosition > trimmed.length) trimmed.length else lastGraphemePosition)
                editText.addTextChangedListener(this)
            }
        } else {
//            textChanged.invoke(text.trim())
            textChanged.invoke(text.trim())
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MaxCodePointWatcher::class.java)
    }
}