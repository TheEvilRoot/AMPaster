package com.theevilroot.ampaster

import android.text.Editable
import android.text.TextWatcher


class TextWatcherWrapper(private val afterChange: (Editable) -> Unit = { _ -> },
                         private val onChange: (String, Int, Int, Int) -> Unit = { _, _, _, _ -> },
                         private val beforeChange: (String, Int, Int, Int) -> Unit = { _, _, _, _ -> }): TextWatcher {
    override fun afterTextChanged(s: Editable) {
        afterChange(s)
    }
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        beforeChange(s.toString(),start,count, after)
    }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        onChange(s.toString(),start, before, count)
    }

}