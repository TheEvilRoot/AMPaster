package com.theevilroot.ampaster

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class Paste(val id: String, val name: String, val text: String? = null ,val date: Date,val expireDate: Date ,val size: Int, val syntaxFull: String, val syntaxKey: String, val hits: Int, val visibilityKey: String)

class PasteHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val name by bindView<TextView>(R.id.paste_name)
    private val id by bindView<TextView>(R.id.paste_id)
    private val expiration by bindView<Chip>(R.id.paste_expiration_chip)
    private val visibility by bindView<Chip>(R.id.paste_visibility_chip)
    private val syntax by bindView<Chip>(R.id.paste_syntax_chip)
    private val size by bindView<Chip>(R.id.paste_size_chip)
    private val hits by bindView<Chip>(R.id.paste_hits_chip)

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    fun bind(paste: Paste) {
        name.text = paste.name
        id.text = "#${paste.id}"
        expiration.setRawText(SimpleDateFormat("dd.MM.YYYY HH:mm:ss").format(paste.expireDate))
        visibility.setRawText(TheHolder.visibilities.key(paste.visibilityKey) ?: paste.visibilityKey)
        syntax.setRawText(paste.syntaxFull)
        size.setRawText("${paste.size} Байт")
        hits.setRawText("${paste.hits} посещений")
        itemView.setOnClickListener {
            itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pastebin.com/${paste.id}")))
        }
    }
}

class PasteAdapter(private val user: User): RecyclerView.Adapter<PasteHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasteHolder =
            PasteHolder(LayoutInflater.from(parent.context).inflate(R.layout.paste_layout, parent, false))

    override fun getItemCount(): Int =
            user.pasteList?.count() ?: 0

    override fun onBindViewHolder(holder: PasteHolder, position: Int) {
        holder.bind(user.pasteList!![position])
        holder.itemView.tag = position
    }

}