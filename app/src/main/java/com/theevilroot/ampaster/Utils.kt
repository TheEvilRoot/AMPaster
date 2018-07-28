package com.theevilroot.ampaster

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable

fun <T: View> Activity.bind(@IdRes id: Int): Lazy<T> =
        lazy { findViewById<T>(id) }

fun <T : View> RecyclerView.ViewHolder.bindView(@IdRes res: Int): Lazy<T> =
        lazy { itemView.findViewById<T>(res) }

fun Activity.makeToast(message: String) =
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }

fun Chip.setRawText(text: String) {
    (chipDrawable as ChipDrawable).text = text
}

fun <K, V> Map<K, V>.key(value: V): K? {
    if(value !in values)
        return null
    for((k, v) in entries)
        if(v == value)
            return k
    return null
}